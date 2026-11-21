package model;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

// Façade do Model (cap. 17): a única classe pública do pacote, além das interfaces do Observer. Ela dá ao
// controlador um ponto único de acesso à partida e repassa cada pedido à classe responsável:
// Movimento (piões), Tributos (províncias, estoque e Khan), Cidades (tesouros e conquista), Yurts e
// Kurultai (conselheiros, cartas de conselheiro aberto, votos e fim da partida). Aqui ficam só o que é da
// partida como um todo: os jogadores, a vez, a coluna do turno, as validações de vez e os avisos ao Observer.
// Singleton (cap. 17): há uma só partida, obtida com Jogo.getInstance(). novaPartida, sortear e carregar
// trocam o estado dessa instância, e os observadores registrados continuam valendo.
// Observado (cap. 17): toda mudança de estado é avisada aos observadores.
// A API pública tem só: getInstance, os comandos (novaPartida a encerrarTurno, salvar e carregar), os testes de
// clique (paradaEm, provinciaEm, localKhanEm) e o Observer (add, remove, get). Quem lê a partida (janelas,
// terminal, controlador e testes) usa só get(chave); a View vê Jogo apenas como ObservadoIF (contrato lá).
public class Jogo implements ObservadoIF {
    private static final int MIN_JOGADORES = 2;
    private static final int MAX_JOGADORES = 5;

    private static Jogo instancia = null;

    // Sorteios (herdeiros, cidades, tesouros e cartas). Para testar situações específicas, o testador pode fixar
    // a semente ao rodar o jogo, por exemplo: java -Djogo.semente=42 app.Main (a mesma semente repete os sorteios).
    private final Random aleatorio = Long.getLong("jogo.semente") == null ? new Random()
            : new Random(Long.getLong("jogo.semente"));

    private final Mapa mapa = new Mapa();
    private final Tributos tributos = new Tributos(mapa);
    private final Cidades cidades = new Cidades(mapa);
    private final Yurts yurts = new Yurts(mapa);
    private final Kurultai kurultai = new Kurultai(yurts, cidades);
    private final List<ObservadorIF> observadores = new ArrayList<>();
    private List<Jogador> jogadores = new ArrayList<>();
    private int jogadorAtual;
    private Turno turno;
    private int selecionada = -1; // coluna marcada na cartela, ainda não ativada
    // O último evento e os seus detalhes (grupo 7 das chaves de get).
    private Evento ultimoEvento = Evento.PARTIDA;
    private int autorDoEvento = -1;
    private String ultimaProvincia;
    private String ultimaCidade;
    private String ultimoTesouro;
    private String ultimaParadaYurt;
    private String ultimoConselheiro;
    private String ultimaCarta;

    private Jogo() {
    }

    public static Jogo getInstance() {
        if (instancia == null) instancia = new Jogo();
        return instancia;
    }

    // ---------------------------------------------------------------- partida

    /** Começa uma partida com os herdeiros na ordem dos jogadores (por exemplo, "Tolui", "Altani"). */
    public void novaPartida(List<String> herdeiros) {
        iniciar(criarJogadores(herdeiros), 0, null);
    }

    /** Começa uma partida com n jogadores e sorteia o herdeiro de cada um (RF07). */
    public void sortear(int n) {
        validarNumeroJogadores(n);
        List<Herdeiro> todos = Arrays.asList(Herdeiro.values());
        Collections.shuffle(todos, aleatorio);
        List<String> nomes = new ArrayList<>();
        for (Herdeiro h : todos.subList(0, n)) nomes.add(h.getNome());
        novaPartida(nomes);
    }

    private List<Jogador> criarJogadores(List<String> herdeiros) {
        validarNumeroJogadores(herdeiros.size());
        Set<Herdeiro> escolhidos = EnumSet.noneOf(Herdeiro.class);
        List<Jogador> novos = new ArrayList<>();
        for (int i = 0; i < herdeiros.size(); i++) {
            Herdeiro h = Herdeiro.porNome(herdeiros.get(i));
            if (!escolhidos.add(h))
                throw new IllegalArgumentException("Erro em Jogo.novaPartida: o herdeiro " + h.getNome()
                        + " foi escolhido mais de uma vez.");
            // §5: 1 moeda para o 1º e o 2º jogadores, 2 moedas para cada um dos outros.
            Jogador j = new Jogador(h, i < 2 ? 1 : 2);
            j.setPosicao(mapa.getParada(Mapa.KARAKORUM)); // §5: todos os piões começam em Karakorum.
            novos.add(j);
        }
        return novos;
    }

    // Troca o estado de uma vez só, depois de tudo validado: se algo falhar antes, a partida anterior continua.
    private void iniciar(List<Jogador> novos, int atual, Turno t) {
        jogadores = novos;
        jogadorAtual = atual;
        turno = t;
        selecionada = -1;
        ultimaProvincia = ultimaCidade = ultimoTesouro = ultimaParadaYurt = ultimoConselheiro = ultimaCarta = null;
        tributos.preparar();
        yurts.preparar();
        cidades.sortear(aleatorio);
        kurultai.preparar(jogadores, aleatorio);
        notificar(Evento.PARTIDA, -1);
    }

    static void validarNumeroJogadores(int n) {
        if (n < MIN_JOGADORES || n > MAX_JOGADORES)
            throw new IllegalArgumentException("Erro em Jogo.validarNumeroJogadores: " + n
                    + " jogadores não é válido (mínimo: " + MIN_JOGADORES + ", máximo: " + MAX_JOGADORES
                    + "). Tente novamente.");
    }

    // Usados também pelos testes, para começar com cidades, tesouros e cartas conhecidos.
    void prepararCidades(List<String> ordem, List<Tesouro> tesouros) {
        cidades.preparar(ordem, tesouros);
    }

    void prepararCartas(List<String> ids) {
        kurultai.prepararCartas(ids);
    }

    // ---------------------------------------------------------------- colunas

    /**
     * Marca a coluna (0 a 3) na cartela do jogador da vez, sem ativá-la. Marcar de novo a mesma coluna desmarca.
     * Só dá para marcar uma coluna livre, antes de ativar uma coluna no turno.
     */
    public void selecionarColuna(int jogador, int coluna) {
        exigirColunaLivre(jogador, coluna, "selecionarColuna");
        selecionada = coluna == selecionada ? -1 : coluna;
        notificar(Evento.SELECAO, jogador);
    }

    /** Ativa a coluna marcada com selecionarColuna. */
    public void confirmarColuna(int jogador) {
        exigirVez(jogador, "confirmarColuna");
        if (selecionada == -1)
            throw new IllegalStateException("Erro em Jogo.confirmarColuna: " + getNome(jogador)
                    + " ainda não marcou uma coluna na cartela.");
        ativarColuna(jogador, selecionada);
    }

    /**
     * Ativa a coluna (0 a 3, da esquerda para a direita na cartela): o jogador recebe os cavalos, os punhos
     * e, se a coluna tiver o símbolo, a obrigação de mover o Khan (§7).
     * Uma coluna usada fica bloqueada até que a coluna de reativação seja ativada.
     */
    public void ativarColuna(int jogador, int coluna) {
        exigirColunaLivre(jogador, coluna, "ativarColuna");
        Jogador j = jogadores.get(jogador);
        if (coluna == Herdeiro.BRANCA) j.liberarColunas();
        else j.bloquear(coluna);
        turno = new Turno(j, coluna);
        selecionada = -1;
        notificar(Evento.COLUNA, jogador);
    }

    private void exigirColunaLivre(int jogador, int coluna, String funcao) {
        exigirVez(jogador, funcao);
        if (turno != null)
            throw new IllegalStateException("Erro em Jogo." + funcao + ": " + getNome(jogador)
                    + " já ativou uma coluna neste turno.");
        exigirNumeroDaColuna(coluna, funcao);
        if (jogadores.get(jogador).isBloqueada(coluna))
            throw new IllegalStateException("Erro em Jogo." + funcao + ": a coluna " + (coluna + 1) + " de "
                    + getNome(jogador) + " está bloqueada. Ative a coluna de reativação (" + (Herdeiro.BRANCA + 1)
                    + ") para liberá-la.");
    }

    private static void exigirNumeroDaColuna(int coluna, String funcao) {
        if (coluna < 0 || coluna > 3)
            throw new IllegalArgumentException("Erro em Jogo." + funcao + ": a coluna " + coluna
                    + " não existe (use 0 a 3).");
    }

    /** Indica se a coluna (0 a 3) do jogador está bloqueada. */
    private boolean isColunaBloqueada(int jogador, int coluna) {
        Jogador j = buscar(jogador, "isColunaBloqueada");
        exigirNumeroDaColuna(coluna, "isColunaBloqueada");
        return j.isBloqueada(coluna);
    }

    // ---------------------------------------------------------------- ações do turno

    /**
     * Anda com o pião do jogador gastando 1 movimento (§7.1): até uma parada vizinha ou, pulando paradas com
     * yurts dele, até a parada vizinha à última parada pulada (regras, p. 7). As paradas puladas não contam como
     * paradas onde ele esteve. Uma parada dupla também custa 1. Pode-se passar por uma parada ocupada, mas não
     * terminar o movimento nela. O turno não passa sozinho: o jogador encerra o turno quando quiser.
     */
    public void mover(int jogador, String paradaId) {
        exigirColuna(jogador, "mover");
        Jogador j = jogadores.get(jogador);
        Parada destino = mapa.getParada(paradaId);
        if (turno.getMovimentos() == 0)
            throw new IllegalStateException("Erro em Jogo.mover: " + j.getNome() + " não tem mais movimentos neste turno.");
        if (destino == j.getPosicao())
            throw new IllegalArgumentException("Erro em Jogo.mover: o pião de " + j.getNome() + " já está em " + paradaId + ".");
        if (!Movimento.alcance(j).contains(destino))
            throw new IllegalArgumentException("Erro em Jogo.mover: " + paradaId + " não é vizinha de "
                    + j.getPosicao().getId() + ". Ande uma parada por vez (só os seus yurts podem ser pulados).");
        if (turno.getMovimentos() == 1 && Movimento.lotada(destino, j, jogadores))
            throw new IllegalArgumentException("Erro em Jogo.mover: a parada " + paradaId
                    + " já está ocupada, e o último movimento não pode terminar nela.");
        j.setPosicao(destino);
        turno.andar(destino);
        notificar(Evento.MOVIMENTO, jogador);
    }

    /**
     * Gasta 1 punho para pegar 1 peça de tributo da província (§7.2). A província precisa ter peças e ser
     * adjacente a uma parada onde o pião esteve neste turno, antes, durante ou depois do movimento.
     */
    public void pegarTributo(int jogador, String provinciaId) {
        exigirColuna(jogador, "pegarTributo");
        boolean fimAntes = kurultai.isFimDisparado();
        tributos.pegar(jogadores.get(jogador), turno, mapa.getProvincia(provinciaId));
        ultimaProvincia = provinciaId;
        avisar(Evento.TRIBUTO, false, fimAntes);
    }

    /**
     * Move o Khan para a província do Khan indicada (0 a 3), quando a coluna ativada manda (§7.3). Ele não pode
     * ficar onde está. A província e as 2 apontadas pelas setas recebem 1 peça de tributo cada, até o máximo de 3.
     */
    public void moverKhan(int jogador, int local) {
        exigirColuna(jogador, "moverKhan");
        tributos.moverKhan(jogadores.get(jogador), turno, local, jogadores);
        notificar(Evento.KHAN, jogador);
    }

    /**
     * Toma o tesouro da cidade com espadas (regras, p. 10). O 1º tesouro de uma cidade no turno custa 1 espada, e
     * cada tesouro seguinte da mesma cidade custa 2. Quem toma o último tesouro conquista a cidade.
     */
    public void atacarCidade(int jogador, String cidade, String tesouro) {
        exigirColuna(jogador, "atacarCidade");
        Cidade c = mapa.getCidade(cidade);
        Tesouro t = Tesouro.porNome(tesouro);
        boolean fimAntes = kurultai.isFimDisparado();
        boolean conquistou = cidades.atacar(jogadores.get(jogador), turno, c, t);
        if (conquistou) kurultai.verificarFim(jogadorAtual);
        ultimaCidade = cidade;
        ultimoTesouro = t.getNome();
        avisar(Evento.CIDADE, conquistou, fimAntes);
    }

    /**
     * Gasta 1 peça de tributo de yurt para construir um yurt do jogador numa parada onde ele esteve neste turno
     * (regras, p. 11). Cabe 1 yurt por parada, 2 numa parada dupla, e nenhum em Karakorum. Cada jogador tem
     * 12 yurts, contando os que estão no meio das cidades conquistadas.
     */
    public void construirYurt(int jogador, String paradaId) {
        exigirColuna(jogador, "construirYurt");
        boolean fimAntes = kurultai.isFimDisparado();
        yurts.construir(jogadores.get(jogador), turno, mapa.getParada(paradaId));
        ultimaParadaYurt = paradaId;
        avisar(Evento.YURT, false, fimAntes);
    }

    /**
     * Entrega ao conselheiro da parada o conjunto de tesouros que ele pede e ganha os votos dele (regras, p. 12
     * e 13). O pião precisa ter estado na parada neste turno. No conselheiro que aceita 3 tesouros quaisquer,
     * vão os tesouros que o jogador tem em maior quantidade.
     */
    public void entregarTesouros(int jogador, String paradaId) {
        exigirColuna(jogador, "entregarTesouros");
        boolean fimAntes = kurultai.isFimDisparado();
        kurultai.entregar(jogadores.get(jogador), turno, mapa.getConselheiro(paradaId), jogadorAtual);
        ultimoConselheiro = paradaId;
        avisar(Evento.CONSELHEIRO, false, fimAntes);
    }

    // Depois de uma ação que pode cumprir uma carta de conselheiro aberto: confere as cartas (regras, p. 18) e
    // avisa, nesta ordem, a ação, a conquista da cidade, cada carta ganha e o disparo do fim da partida.
    // fimAntes diz se o fim já estava disparado antes da ação.
    private void avisar(Evento acao, boolean conquistou, boolean fimAntes) {
        List<CartaAberta> ganhas = kurultai.verificarCartas(jogadorAtual);
        notificar(acao, jogadorAtual);
        if (conquistou) notificar(Evento.CONQUISTA, jogadorAtual);
        for (CartaAberta c : ganhas) {
            ultimaCarta = c.getId();
            notificar(Evento.CARTA, jogadorAtual);
        }
        if (!fimAntes && kurultai.isFimDisparado()) notificar(Evento.FIM_DISPARADO, jogadorAtual);
    }

    public void encerrarTurno(int jogador) {
        exigirColuna(jogador, "encerrarTurno");
        Jogador j = jogadores.get(jogador);
        if (turno.isKhanPendente())
            throw new IllegalStateException("Erro em Jogo.encerrarTurno: a coluna ativada manda mover o Khan."
                    + " Clique numa das cabeças do Khan no tabuleiro.");
        if (Movimento.lotada(j.getPosicao(), j, jogadores))
            throw new IllegalStateException("Erro em Jogo.encerrarTurno: " + j.getNome() + " está na parada "
                    + j.getPosicao().getId() + ", que já tem outro pião. Ande até uma parada livre.");
        turno = null;
        selecionada = -1;
        if (kurultai.encerrarTurno()) { // acabou a última rodada
            notificar(Evento.FIM, jogador);
            return;
        }
        jogadorAtual = (jogadorAtual + 1) % jogadores.size();
        notificar(Evento.TURNO, jogador);
    }

    // ---------------------------------------------------------------- Observer

    @Override
    public void add(ObservadorIF observador) {
        if (!observadores.contains(observador)) observadores.add(observador);
    }

    @Override
    public void remove(ObservadorIF observador) {
        observadores.remove(observador);
    }

    // Cada chave só é usada com valores do seu tipo (veja valor), por isso a conversão para T é segura.
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Chave<T> chave) {
        return (T) valor(chave);
    }

    // O valor de cada chave de ObservadoIF, sempre numa cópia nova e só para leitura.
    private Object valor(Chave<?> chave) {
        // grupo 1: dados fixos
        if (chave == HERDEIROS) return getHerdeiros();
        if (chave == TIPOS_TESOURO) return getTiposTesouro();
        if (chave == COLUNA_REATIVACAO) return Herdeiro.BRANCA;
        if (chave == PONTOS_PARADAS) return porId(getParadas(), id -> getPontos(id).clone());
        if (chave == RAIOS_PARADAS) return porId(getParadas(), this::getRaio);
        if (chave == VIZINHAS_PARADAS) return porId(getParadas(), id -> List.copyOf(getVizinhas(id)));
        if (chave == PONTOS_PROVINCIAS) return porId(getProvincias(), this::getPontoProvincia);
        if (chave == TIPOS_PROVINCIAS) return porId(getProvincias(), this::getTipoProvincia);
        if (chave == ADJACENTES_PROVINCIAS) return porId(getProvincias(), id -> List.copyOf(getAdjacentesProvincia(id)));
        if (chave == PONTOS_KHAN) return porIndice(getNumeroLocaisKhan(), this::getPontoKhan);
        if (chave == PROVINCIAS_KHAN) return porIndice(getNumeroLocaisKhan(), i -> List.copyOf(getProvinciasDoKhan(i)));
        if (chave == PONTOS_CIDADES) return porId(getCidades(), this::getPontoCidade);
        if (chave == PEDIDOS_CONSELHEIROS) return porId(getConselheiros(), id -> List.copyOf(getPedidoConselheiro(id)));
        if (chave == VOTOS_CONSELHEIROS) return porId(getConselheiros(), this::getVotosConselheiro);
        // grupo 2: dados da partida
        if (chave == NOMES) return porJogador(this::getNome);
        if (chave == LIMITE_VOTOS) return getLimiteVotos();
        if (chave == CARTAS_ABERTAS) return porId(getCartasAbertas(), this::getDescricaoCarta);
        // grupo 3: jogadores
        if (chave == JOGADOR_ATUAL) return jogadorAtual;
        if (chave == MOEDAS) return porJogador(this::getMoedas);
        if (chave == ESPADAS) return porJogador(this::getEspadas);
        if (chave == PECAS_YURT) return porJogador(this::getYurts);
        if (chave == YURTS_RESTANTES) return porJogador(this::getYurtsRestantes);
        if (chave == TESOUROS) return porJogador(i -> porId(getTiposTesouro(), t -> getTesouros(i, t)));
        if (chave == VOTOS) return porJogador(this::getVotos);
        if (chave == POSICOES) return porJogador(this::getPosicao);
        if (chave == COLUNAS_BLOQUEADAS) return porJogador(this::bloqueadas);
        // grupo 4: o turno
        if (chave == COLUNA_SELECIONADA) return selecionada;
        if (chave == COLUNA_ATIVADA) return getColunaAtivada();
        if (chave == MOVIMENTOS) return getMovimentos();
        if (chave == PUNHOS) return getPunhos();
        if (chave == KHAN_PENDENTE) return isKhanPendente();
        if (chave == VISITADAS) return List.copyOf(getVisitadas());
        if (chave == DESTINOS) return List.copyOf(getDestinos());
        if (chave == PROVINCIAS_ALCANCAVEIS) return List.copyOf(getProvinciasAlcancaveis());
        if (chave == CIDADES_ATACAVEIS) return List.copyOf(getCidadesAtacaveis());
        if (chave == PARADAS_PARA_YURT) return List.copyOf(getParadasParaYurt());
        if (chave == CONSELHEIROS_PARA_ENTREGA) return List.copyOf(getConselheirosParaEntrega());
        // grupo 5: o tabuleiro
        if (chave == YURTS_NAS_PARADAS) return porId(getParadas(), id -> List.copyOf(getYurtsNaParada(id)));
        if (chave == CIDADES_REVELADAS) return getCidades().stream().filter(this::isCidadeRevelada).toList();
        if (chave == TESOUROS_CIDADES) return porId(getCidades(), c -> List.copyOf(getTesourosCidade(c)));
        if (chave == DONOS_CIDADES) return porId(getCidades(), this::getDonoCidade);
        if (chave == CIDADES_NA_PILHA) return getCidadesNaPilha();
        if (chave == PECAS_PROVINCIAS) return porId(getProvincias(), this::getPecasProvincia);
        if (chave == POSICAO_KHAN) return getPosicaoKhan();
        if (chave == ESTOQUE) return porId(Arrays.stream(Tributo.values()).map(Tributo::getNome).toList(), this::getEstoque);
        // grupo 6: Kurultai e fim da partida
        if (chave == VOTOS_NO_CONSELHO) return getVotosNoConselho();
        if (chave == DONOS_CARTAS) return porId(getCartasAbertas(), this::getDonoCarta);
        if (chave == FIM_DISPARADO) return isFimDisparado();
        if (chave == TURNOS_RESTANTES) return getTurnosRestantes();
        if (chave == PARTIDA_TERMINADA) return isPartidaTerminada();
        if (chave == VENCEDORES) return List.copyOf(getVencedores());
        // grupo 7: o evento e seus detalhes
        if (chave == EVENTO) return ultimoEvento;
        if (chave == AUTOR_DO_EVENTO) return autorDoEvento;
        if (chave == ULTIMA_PROVINCIA) return ultimaProvincia;
        if (chave == ULTIMA_CIDADE) return ultimaCidade;
        if (chave == ULTIMO_TESOURO) return ultimoTesouro;
        if (chave == ULTIMA_PARADA_YURT) return ultimaParadaYurt;
        if (chave == ULTIMO_CONSELHEIRO) return ultimoConselheiro;
        if (chave == ULTIMA_CARTA) return ultimaCarta;
        throw new IllegalArgumentException("Erro em Jogo.get: a chave " + chave + " não tem valor.");
    }

    // Mapa id → valor, na ordem dos ids, só para leitura.
    private static <V> Map<String, V> porId(List<String> ids, Function<String, V> valor) {
        Map<String, V> mapa = new LinkedHashMap<>();
        for (String id : ids) mapa.put(id, valor.apply(id));
        return Collections.unmodifiableMap(mapa);
    }

    // Lista com o valor de cada índice de 0 a n - 1, só para leitura.
    private static <V> List<V> porIndice(int n, IntFunction<V> valor) {
        List<V> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) lista.add(valor.apply(i));
        return Collections.unmodifiableList(lista);
    }

    private <V> List<V> porJogador(IntFunction<V> valor) {
        return porIndice(jogadores.size(), valor);
    }

    private List<Integer> bloqueadas(int jogador) {
        List<Integer> colunas = new ArrayList<>();
        for (int c = 0; c < 4; c++)
            if (isColunaBloqueada(jogador, c)) colunas.add(c);
        return List.copyOf(colunas);
    }

    // O autor é o jogador que causou o evento (-1 quando uma partida começa).
    private void notificar(Evento evento, int autor) {
        ultimoEvento = evento;
        autorDoEvento = autor;
        ListIterator<ObservadorIF> li = new ArrayList<>(observadores).listIterator();
        while (li.hasNext()) li.next().notify(this);
    }

    // ---------------------------------------------------------------- consultas
    // Os métodos privados abaixo calculam os valores de get (veja valor). Os públicos são os testes de clique,
    // que recebem coordenadas e por isso não cabem numa chave.

    // ---------------------------------------------------------------- consultas: mapa e jogadores

    /** Retorna o id da parada que contém o ponto (x, y) da imagem original do tabuleiro, ou null. */
    public String paradaEm(int x, int y) {
        Parada p = mapa.paradaEm(x, y);
        return p == null ? null : p.getId();
    }

    /** Nomes dos 5 herdeiros, na ordem das cartelas. */
    private static List<String> getHerdeiros() {
        List<String> nomes = new ArrayList<>();
        for (Herdeiro h : Herdeiro.values()) nomes.add(h.getNome());
        return nomes;
    }

    /** Ids de todas as paradas do mapa. */
    private List<String> getParadas() {
        return ids(mapa.getParadas());
    }

    /** Centro de cada medalhão da parada na imagem original do tabuleiro: x1, y1[, x2, y2]. */
    private int[] getPontos(String paradaId) {
        return mapa.getParada(paradaId).getPontos();
    }

    /** Raio da área de clique da parada, em pixels da imagem original. */
    private int getRaio(String paradaId) {
        return mapa.getParada(paradaId).getRaio();
    }

    private List<String> getVizinhas(String paradaId) {
        return ids(mapa.getParada(paradaId).getVizinhas());
    }

    /** Retorna o nome do herdeiro do jogador. */
    private String getNome(int jogador) {
        return buscar(jogador, "getNome").getNome();
    }

    private int getMoedas(int jogador) {
        return buscar(jogador, "getMoedas").getMoedas();
    }

    /** Peças de espada guardadas pelo jogador. */
    private int getEspadas(int jogador) {
        return buscar(jogador, "getEspadas").getTributos(Tributo.ESPADA);
    }

    /** Peças de tributo de yurt guardadas pelo jogador (os yurts colocados no tabuleiro são outra coisa). */
    private int getYurts(int jogador) {
        return buscar(jogador, "getYurts").getTributos(Tributo.YURT);
    }

    /** Peças de tesouro do tipo ("boi", "seda", "ferro", "pele" ou "especiaria") guardadas pelo jogador. */
    private int getTesouros(int jogador, String tipo) {
        return buscar(jogador, "getTesouros").getTesouros(Tesouro.porNome(tipo));
    }

    /** Votos do jogador: a posição da sua ficha na trilha do Kurultai. */
    private int getVotos(int jogador) {
        return buscar(jogador, "getVotos").getVotos();
    }

    /** Retorna o id da parada onde está o pião do jogador. */
    private String getPosicao(int jogador) {
        return buscar(jogador, "getPosicao").getPosicao().getId();
    }

    // ---------------------------------------------------------------- consultas: turno

    /** Retorna -1 se nenhuma coluna foi ativada neste turno. */
    private int getColunaAtivada() {
        return turno == null ? -1 : turno.getColuna();
    }

    /** Retorna 0 se nenhuma coluna foi ativada neste turno. */
    private int getMovimentos() {
        return turno == null ? 0 : turno.getMovimentos();
    }

    /** Punhos ainda não usados neste turno (0 se nenhuma coluna foi ativada). */
    private int getPunhos() {
        return turno == null ? 0 : turno.getPunhos();
    }

    /** Indica se a coluna ativada obriga a mover o Khan e isso ainda não foi feito. */
    private boolean isKhanPendente() {
        return turno != null && turno.isKhanPendente();
    }

    /** Paradas onde o pião do jogador da vez esteve neste turno, a começar pela de onde saiu. */
    private List<String> getVisitadas() {
        return turno == null ? new ArrayList<>() : ids(turno.getVisitadas());
    }

    /** Paradas para onde o jogador da vez pode andar agora (vazia se ele não tiver movimentos). */
    private List<String> getDestinos() {
        return ids(Movimento.destinos(turno, jogadores));
    }

    /** Províncias de onde o jogador da vez pode pegar tributo agora (vazia se ele não tiver punhos). */
    private List<String> getProvinciasAlcancaveis() {
        return idsProvincias(tributos.alcancaveis(turno));
    }

    /** Cidades que o jogador da vez pode atacar agora: reveladas, adjacentes ao caminho e com espadas suficientes. */
    private List<String> getCidadesAtacaveis() {
        List<String> nomes = new ArrayList<>();
        for (Cidade c : cidades.atacaveis(turno)) nomes.add(c.nome());
        return nomes;
    }

    /** Paradas onde o jogador da vez pode construir um yurt agora (vazia se ele não tiver peças de yurt). */
    private List<String> getParadasParaYurt() {
        return ids(yurts.paradasParaYurt(turno));
    }

    /** Conselheiros a quem o jogador da vez pode entregar tesouros agora. */
    private List<String> getConselheirosParaEntrega() {
        List<String> ids = new ArrayList<>();
        for (Conselheiro c : kurultai.paraEntrega(turno, mapa.getConselheiros())) ids.add(c.getParada().getId());
        return ids;
    }

    // ---------------------------------------------------------------- consultas: tributos e Khan

    /** Peças de tributo do tipo ("yurt", "espada" ou "moeda") que ainda estão no estoque. */
    private int getEstoque(String tipo) {
        for (Tributo t : Tributo.values())
            if (t.getNome().equals(tipo)) return tributos.estoque(t, jogadores);
        throw new IllegalArgumentException("Erro em Jogo.getEstoque: o tributo " + tipo
                + " não existe (use yurt, espada ou moeda).");
    }

    /** Ids das 12 províncias. */
    private List<String> getProvincias() {
        return idsProvincias(mapa.getProvincias());
    }

    /** Centro do ícone da província na imagem original do tabuleiro: x, y. */
    private int[] getPontoProvincia(String provinciaId) {
        Provincia p = mapa.getProvincia(provinciaId);
        return new int[] {p.getX(), p.getY()};
    }

    /** Tipo de tributo da província: "yurt", "espada" ou "moeda". */
    private String getTipoProvincia(String provinciaId) {
        return mapa.getProvincia(provinciaId).getTipo().getNome();
    }

    /** Peças de tributo que estão na província (0 a 3). */
    private int getPecasProvincia(String provinciaId) {
        return mapa.getProvincia(provinciaId).getPecas();
    }

    /** Paradas na borda da província (para o modo de depuração). */
    private List<String> getAdjacentesProvincia(String provinciaId) {
        return ids(mapa.getProvincia(provinciaId).getAdjacentes());
    }

    /** Retorna o id da província cujo ícone contém o ponto (x, y) da imagem original do tabuleiro, ou null. */
    public String provinciaEm(int x, int y) {
        Provincia p = mapa.provinciaEm(x, y);
        return p == null ? null : p.getId();
    }

    /** Número de províncias do Khan (4). */
    private int getNumeroLocaisKhan() {
        return mapa.getLocaisKhan().size();
    }

    /** Centro da cabeça do Khan da província do Khan na imagem original do tabuleiro: x, y. */
    private int[] getPontoKhan(int local) {
        LocalKhan l = tributos.getLocal(local);
        return new int[] {l.getX(), l.getY()};
    }

    /** Ids das 3 províncias que recebem tributo quando o Khan vai para a província do Khan. */
    private List<String> getProvinciasDoKhan(int local) {
        List<String> ids = new ArrayList<>();
        for (Provincia p : tributos.getLocal(local).getProvincias()) ids.add(p.getId());
        return ids;
    }

    /** Província do Khan onde ele está (0 a 3), ou -1 se ele ainda está fora do tabuleiro. */
    private int getPosicaoKhan() {
        return tributos.getKhan();
    }

    /** Índice da província do Khan cuja cabeça contém o ponto (x, y) da imagem original, ou -1. */
    public int localKhanEm(int x, int y) {
        return mapa.localKhanEm(x, y);
    }

    // ---------------------------------------------------------------- consultas: cidades e yurts

    /** Nomes dos 5 tipos de tesouro. */
    private static List<String> getTiposTesouro() {
        List<String> nomes = new ArrayList<>();
        for (Tesouro t : Tesouro.values()) nomes.add(t.getNome());
        return nomes;
    }

    /** Nomes das 9 cidades. */
    private List<String> getCidades() {
        List<String> nomes = new ArrayList<>();
        for (Cidade c : mapa.getCidades()) nomes.add(c.nome());
        return nomes;
    }

    /** Centro da cidade na imagem original do tabuleiro: x, y. */
    private int[] getPontoCidade(String cidade) {
        Cidade c = mapa.getCidade(cidade);
        return new int[] {c.x(), c.y()};
    }

    /** Indica se a peça da cidade já foi revelada (só cidades reveladas podem ser atacadas). */
    private boolean isCidadeRevelada(String cidade) {
        return mapa.getCidade(cidade).isRevelada();
    }

    /** Tesouros que ainda estão na cidade. */
    private List<String> getTesourosCidade(String cidade) {
        List<String> nomes = new ArrayList<>();
        for (Tesouro t : mapa.getCidade(cidade).getTesouros()) nomes.add(t.getNome());
        return nomes;
    }

    /** Jogador que conquistou a cidade, ou -1. */
    private int getDonoCidade(String cidade) {
        Jogador dono = mapa.getCidade(cidade).getDono();
        return dono == null ? -1 : jogadores.indexOf(dono);
    }

    /** Cidades ainda na pilha, sem revelar. */
    private int getCidadesNaPilha() {
        return cidades.naPilha();
    }

    /** Jogadores donos dos yurts construídos na parada, na ordem em que foram construídos. */
    private List<Integer> getYurtsNaParada(String paradaId) {
        List<Integer> donos = new ArrayList<>();
        for (Jogador j : mapa.getParada(paradaId).getYurts()) donos.add(jogadores.indexOf(j));
        return donos;
    }

    /** Yurts que o jogador ainda pode colocar no tabuleiro, dos 12 que tem. */
    private int getYurtsRestantes(int jogador) {
        return yurts.restantes(buscar(jogador, "getYurtsRestantes"));
    }

    // ---------------------------------------------------------------- consultas: Kurultai

    /** Paradas dos 7 conselheiros (as 6 duplas e Karakorum). */
    private List<String> getConselheiros() {
        List<String> ids = new ArrayList<>();
        for (Conselheiro c : mapa.getConselheiros()) ids.add(c.getParada().getId());
        return ids;
    }

    /** Os 3 tesouros que o conselheiro pede, por exemplo [seda, seda] ou [qualquer, qualquer, qualquer]. */
    private List<String> getPedidoConselheiro(String parada) {
        Conselheiro c = mapa.getConselheiro(parada);
        List<String> pedido = new ArrayList<>();
        if (c.getPedido() == Conselheiro.QUAISQUER) {
            for (int i = 0; i < 3; i++) pedido.add("qualquer");
        } else {
            for (Tesouro t : c.getTipos()) pedido.add(t.getNome());
            if (c.getPedido() == Conselheiro.PAR) pedido.add(c.getTipos().get(0).getNome());
        }
        return pedido;
    }

    /** Votos que cada entrega ao conselheiro dá. */
    private int getVotosConselheiro(String parada) {
        return mapa.getConselheiro(parada).getVotos();
    }

    /** Votos de todos os jogadores somados: a posição da ficha neutra na trilha do Kurultai. */
    private int getVotosNoConselho() {
        return kurultai.votosNoConselho();
    }

    /** Ids das 4 cartas de conselheiro aberto da partida, na ordem da mesa (as já ganhas continuam na lista). */
    private List<String> getCartasAbertas() {
        List<String> ids = new ArrayList<>();
        for (CartaAberta c : kurultai.getCartas()) ids.add(c.getId());
        return ids;
    }

    /** Jogador que ganhou a carta de conselheiro aberto, ou -1 se ela ainda está livre. */
    private int getDonoCarta(String carta) {
        Jogador dono = kurultai.getDono(CartaAberta.porId(carta));
        return dono == null ? -1 : jogadores.indexOf(dono);
    }

    /** O que a carta de conselheiro aberto pede, em texto. */
    private String getDescricaoCarta(String carta) {
        return CartaAberta.porId(carta).getDescricao();
    }

    /** Votos da trilha do Kurultai que disparam o fim da partida com o número atual de jogadores. */
    private int getLimiteVotos() {
        return kurultai.limite();
    }

    private boolean isFimDisparado() {
        return kurultai.isFimDisparado();
    }

    /** Turnos que ainda faltam depois do atual, quando o fim já foi disparado. */
    private int getTurnosRestantes() {
        return kurultai.getTurnosRestantes();
    }

    private boolean isPartidaTerminada() {
        return kurultai.isTerminada();
    }

    /**
     * Jogadores com mais votos na trilha do Kurultai. Nesta iteração o vencedor sai só dos votos ganhos durante
     * a partida; a influência nas regiões, as cartas secretas e o desempate vêm na 4ª iteração.
     */
    private List<Integer> getVencedores() {
        return kurultai.vencedores();
    }

    private static List<String> ids(Iterable<Parada> paradas) {
        List<String> ids = new ArrayList<>();
        for (Parada p : paradas) ids.add(p.getId());
        return ids;
    }

    private static List<String> idsProvincias(Iterable<Provincia> provincias) {
        List<String> ids = new ArrayList<>();
        for (Provincia p : provincias) ids.add(p.getId());
        return ids;
    }

    // ---------------------------------------------------------------- salvar e carregar

    public void salvar(Path arquivo) throws IOException {
        StringBuilder s = new StringBuilder();
        s.append("jogadores=").append(jogadores.size()).append('\n');
        s.append("jogadorAtual=").append(jogadorAtual).append('\n');
        s.append("coluna=").append(getColunaAtivada()).append('\n');
        s.append("movimentos=").append(getMovimentos()).append('\n');
        for (int i = 0; i < jogadores.size(); i++) {
            Jogador j = jogadores.get(i);
            s.append("jogador").append(i).append(".herdeiro=").append(j.getNome()).append('\n');
            s.append("jogador").append(i).append(".moedas=").append(j.getMoedas()).append('\n');
            s.append("jogador").append(i).append(".posicao=").append(j.getPosicao().getId()).append('\n');
            s.append("jogador").append(i).append(".bloqueadas=").append(j.getBloqueadas()).append('\n');
        }
        Files.writeString(arquivo, s);
    }

    public static Jogo carregar(Path arquivo) throws IOException {
        Jogo jogo = getInstance();
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(arquivo)) {
            p.load(r);
        }
        int n = lerInt(p, "jogadores");
        validarNumeroJogadores(n);
        List<String> herdeiros = new ArrayList<>();
        for (int i = 0; i < n; i++)
            herdeiros.add(ler(p, "jogador" + i + ".herdeiro").strip());

        List<Jogador> novos = jogo.criarJogadores(herdeiros);
        for (int i = 0; i < n; i++) {
            Jogador j = new Jogador(novos.get(i).getHerdeiro(), lerInt(p, "jogador" + i + ".moedas"));
            j.setPosicao(jogo.mapa.getParada(ler(p, "jogador" + i + ".posicao").strip()));
            String chave = "jogador" + i + ".bloqueadas";
            if (p.containsKey(chave)) { // saves antigos não têm esta chave: nenhuma coluna bloqueada
                int mascara = lerInt(p, chave);
                if (mascara < 0 || mascara > 7)
                    throw new IllegalArgumentException("Erro em Jogo.carregar: o valor de '" + chave
                            + "' não é válido (" + mascara + ").");
                j.setBloqueadas(mascara);
            }
            novos.set(i, j);
        }
        int atual = lerInt(p, "jogadorAtual");
        if (atual < 0 || atual >= n)
            throw new IllegalArgumentException("Erro em Jogo.carregar: o jogador " + (atual + 1)
                    + " não existe (use 1 a " + n + ").");
        int coluna = lerInt(p, "coluna");
        Turno t = null;
        if (coluna != -1)
            t = new Turno(novos.get(atual), coluna, lerInt(p, "movimentos"));
        jogo.iniciar(novos, atual, t);
        return jogo;
    }

    private static String ler(Properties p, String chave) {
        String valor = p.getProperty(chave);
        if (valor == null)
            throw new IllegalArgumentException("Erro em Jogo.carregar: falta a chave '" + chave + "' no arquivo.");
        return valor;
    }

    private static int lerInt(Properties p, String chave) {
        String valor = ler(p, chave);
        try {
            return Integer.parseInt(valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Erro em Jogo.carregar: o valor de '" + chave
                    + "' não é um número inteiro (" + valor + ").");
        }
    }

    // ---------------------------------------------------------------- validações

    // Só para os testes do pacote: dá acesso ao jogador para montar situações (tesouros, espadas, votos).
    Jogador jogador(int i) {
        return buscar(i, "jogador");
    }

    private Jogador buscar(int jogador, String funcao) {
        if (jogador < 0 || jogador >= jogadores.size())
            throw new IllegalArgumentException("Erro em Jogo." + funcao + ": o jogador " + (jogador + 1)
                    + " não existe (use 1 a " + jogadores.size() + ").");
        return jogadores.get(jogador);
    }

    private void exigirColuna(int jogador, String funcao) {
        exigirVez(jogador, funcao);
        if (turno == null)
            throw new IllegalStateException("Erro em Jogo." + funcao + ": " + getNome(jogador)
                    + " ainda não ativou uma coluna neste turno.");
    }

    private void exigirVez(int jogador, String funcao) {
        buscar(jogador, funcao);
        if (kurultai.isTerminada())
            throw new IllegalStateException("Erro em Jogo." + funcao + ": a partida terminou.");
        if (jogador != jogadorAtual)
            throw new IllegalStateException("Erro em Jogo." + funcao + ": não é a vez de " + getNome(jogador)
                    + ". A vez é de " + getNome(jogadorAtual) + ".");
    }
}
