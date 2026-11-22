package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

// Façade do Model (cap. 17): a única classe pública do pacote, além das interfaces do Observer. Ela dá ao
// controlador um ponto único de acesso à partida e repassa cada pedido à classe responsável:
// Movimento (piões), Tributos (províncias, estoque e Khan), Cidades (tesouros e conquista), Yurts,
// Kurultai (conselheiros, cartas de conselheiro aberto, votos e fim da partida), Melhorias, PecasBonus,
// CartasSecretas, Pontuacao (resultado final), Habilidades (dos herdeiros) e Arquivo (salvar e carregar).
// Aqui ficam só o que é da partida como um todo: os jogadores, a vez, a coluna do turno, as validações de vez e
// os avisos ao Observer.
// Singleton (cap. 17): há uma só partida, obtida com Jogo.getInstance(). novaPartida, sortear e carregar
// trocam o estado dessa instância, e os observadores registrados continuam valendo.
// Observado (cap. 17): toda mudança de estado é avisada aos observadores.
// A API pública tem só: getInstance, os comandos (novaPartida a encerrarTurno, salvar e carregar), os testes de
// clique (paradaEm, provinciaEm, localKhanEm, espacoMelhoriaEm) e o Observer (add, remove, get). Quem lê a
// partida (janelas, terminal, controlador e testes) usa só get(chave); a View vê Jogo apenas como ObservadoIF
// (contrato lá).
public class Jogo implements ObservadoIF {
    private static final int MIN_JOGADORES = 2;
    private static final int MAX_JOGADORES = 5;

    private static Jogo instancia = null;

    // Sorteios (herdeiros, cidades, tesouros e cartas). Para testar situações específicas, o testador pode fixar
    // a semente ao rodar o jogo, por exemplo: java -Djogo.semente=42 app.Main (a mesma semente repete os sorteios).
    private final Random aleatorio = Long.getLong("jogo.semente") == null ? new Random()
            : new Random(Long.getLong("jogo.semente"));

    private final Mapa mapa = new Mapa();
    private final Melhorias melhorias = new Melhorias();
    private final Tributos tributos = new Tributos(mapa, melhorias);
    private final Yurts yurts = new Yurts(mapa);
    private final Cidades cidades = new Cidades(mapa, yurts);
    private final CartasSecretas secretas = new CartasSecretas(mapa, yurts);
    private final Pontuacao pontuacao = new Pontuacao(mapa, yurts, secretas);
    private final Habilidades habilidades = new Habilidades(mapa, tributos, melhorias);
    private final PecasBonus bonus = new PecasBonus(mapa, tributos, cidades);
    private final Kurultai kurultai = new Kurultai(yurts, cidades, bonus);
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
    private String ultimaMelhoria;
    private int espacoUltimaMelhoria = -1;
    private int colunaUltimaMelhoria = -1;
    private String ultimoBonus;
    private String tesouroDoBau;
    private List<String> ganhosDoKhan = List.of();

    private Jogo() {
    }

    public static Jogo getInstance() {
        if (instancia == null) instancia = new Jogo();
        return instancia;
    }

    // ---------------------------------------------------------------- partida

    /** Começa uma partida com os herdeiros na ordem dos jogadores (por exemplo, "Tolui", "Altani"). */
    public void novaPartida(List<String> herdeiros) {
        preparar(criarJogadores(herdeiros), 0);
        concluirCarga(null, -1);
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

    List<Jogador> criarJogadores(List<String> herdeiros) {
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
            j.posicionarPeoes(mapa.getParada(Mapa.KARAKORUM)); // §5: todos os piões começam em Karakorum.
            novos.add(j);
        }
        return novos;
    }

    // Troca o estado de uma vez só, depois de tudo validado: se algo falhar antes, a partida anterior continua.
    // Prepara uma partida nova com os jogadores dados (sorteios, peças e cartas); o Arquivo pode depois trocar
    // esse estado pelo de uma partida salva. Os observadores só são avisados em concluirCarga.
    void preparar(List<Jogador> novos, int atual) {
        jogadores = novos;
        jogadorAtual = atual;
        turno = null;
        selecionada = -1;
        ultimaProvincia = ultimaCidade = ultimoTesouro = ultimaParadaYurt = ultimoConselheiro = ultimaCarta = null;
        ultimaMelhoria = ultimoBonus = tesouroDoBau = null;
        espacoUltimaMelhoria = colunaUltimaMelhoria = -1;
        ganhosDoKhan = List.of();
        tributos.preparar();
        yurts.preparar();
        cidades.sortear(aleatorio);
        melhorias.sortear(aleatorio);
        bonus.sortear(aleatorio);
        kurultai.preparar(jogadores, aleatorio);
        secretas.sortear(jogadores, aleatorio);
        habilidades.preparar(jogadores); // o yurt do Tolui em Karakorum e as melhorias do Jochi
    }

    // Fim da preparação: o turno em andamento (ou null), a coluna marcada, e o aviso aos observadores.
    void concluirCarga(Turno t, int marcada) {
        turno = t;
        selecionada = marcada;
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

    void prepararMelhorias(List<String> ids) {
        List<Melhoria> ordem = new ArrayList<>();
        for (String id : ids) ordem.add(Melhoria.porId(id));
        melhorias.preparar(ordem);
    }

    // As cartas secretas: o jogador i recebe as cartas 2i e 2i + 1 da lista.
    void prepararSecretas(List<String> ids) {
        List<CartaSecreta> cartas = new ArrayList<>();
        for (String id : ids) cartas.add(CartaSecreta.porId(id));
        secretas.preparar(jogadores, cartas);
    }

    // As peças de bônus sobre os conselheiros, na ordem de getConselheiros().
    void prepararBonus(List<String> ids) {
        List<Bonus> pecas = new ArrayList<>();
        for (String id : ids) pecas.add(Bonus.porId(id));
        bonus.preparar(pecas);
    }

    // ---------------------------------------------------------------- cartas secretas (regras, p. 6)

    /**
     * Antes da primeira rodada, cada jogador, na ordem da partida, fica com 1 das 2 cartas de conselheiro secreto
     * que recebeu; a outra sai do jogo. Ninguém ativa uma coluna antes de todos escolherem.
     */
    public void escolherCartaSecreta(int jogador, String carta) {
        int escolhendo = CartasSecretas.escolhendo(jogadores);
        if (escolhendo == -1)
            throw new IllegalStateException("Erro em Jogo.escolherCartaSecreta: todos já escolheram a carta secreta.");
        Jogador j = buscar(jogador, "escolherCartaSecreta");
        if (jogador != escolhendo)
            throw new IllegalStateException("Erro em Jogo.escolherCartaSecreta: agora é a vez de " + getNome(escolhendo)
                    + " escolher a carta secreta.");
        secretas.escolher(j, CartaSecreta.porId(carta));
        notificar(Evento.SECRETA, jogador);
    }

    /** Jogador que precisa escolher a carta secreta agora, ou -1 se todos já escolheram. */
    private int getJogadorEscolhendoSecreta() {
        return CartasSecretas.escolhendo(jogadores);
    }

    /** As 2 cartas secretas que o jogador recebeu no começo. */
    private List<String> getOpcoesSecretas(int jogador) {
        List<String> ids = new ArrayList<>();
        for (CartaSecreta c : buscar(jogador, "getOpcoesSecretas").getOpcoesSecretas()) ids.add(c.getId());
        return ids;
    }

    /** A carta secreta escolhida pelo jogador, ou null. A interface só deve mostrá-la ao próprio jogador. */
    private String getCartaSecreta(int jogador) {
        CartaSecreta c = buscar(jogador, "getCartaSecreta").getCartaSecreta();
        return c == null ? null : c.getId();
    }

    /** Medida atual do objetivo da carta secreta do jogador (por exemplo, quantos yurts ligados), ou 0. */
    private int getMedidaCartaSecreta(int jogador) {
        Jogador j = buscar(jogador, "getMedidaCartaSecreta");
        return j.getCartaSecreta() == null ? 0 : secretas.medida(j, j.getCartaSecreta());
    }

    /** Votos (0 a 3) que a carta secreta do jogador daria se a partida acabasse agora. */
    private int getVotosCartaSecreta(int jogador) {
        return secretas.votos(buscar(jogador, "getVotosCartaSecreta"));
    }

    /** Ids das 10 cartas secretas. */
    private static List<String> getTiposCartaSecreta() {
        List<String> ids = new ArrayList<>();
        for (CartaSecreta c : CartaSecreta.values()) ids.add(c.getId());
        return ids;
    }

    private static String getNomeCartaSecreta(String carta) {
        return CartaSecreta.porId(carta).getNome();
    }

    private static String getObjetivoCartaSecreta(String carta) {
        return CartaSecreta.porId(carta).getObjetivo();
    }

    /** Medida mínima do objetivo para 1, 2 e 3 votos. */
    private static int[] getFaixasCartaSecreta(String carta) {
        return CartaSecreta.porId(carta).getFaixas();
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
        int escolhendo = CartasSecretas.escolhendo(jogadores);
        if (escolhendo != -1)
            throw new IllegalStateException("Erro em Jogo." + funcao + ": antes da primeira rodada, cada jogador escolhe"
                    + " a sua carta secreta. Falta " + getNome(escolhendo) + " escolher.");
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
        if (!Movimento.temSaida(destino, turno.getMovimentos() - 1, j, jogadores))
            throw new IllegalArgumentException("Erro em Jogo.mover: a parada " + paradaId + " já está ocupada, e "
                    + (turno.getMovimentos() == 1 ? "o último movimento não pode terminar nela."
                            : "dali não dá para chegar a uma parada livre com os movimentos que sobram."));
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
        avisar(Evento.TRIBUTO, null, fimAntes);
    }

    /**
     * Move o Khan para a província do Khan indicada (0 a 3) ou para a área de melhorias (KHAN_AREA), quando a
     * coluna ativada manda (§7.3). Ele não pode ficar onde está. Numa província, ela e as 2 apontadas pelas setas
     * recebem 1 peça de tributo cada, até o máximo de 3. Na área, os espaços vazios recebem melhorias da pilha
     * (regras, p. 9).
     */
    public void moverKhan(int jogador, int local) {
        exigirColuna(jogador, "moverKhan");
        Jogador j = jogadores.get(jogador);
        int moedas = j.getMoedas(), espadas = j.getTributos(Tributo.ESPADA);
        tributos.moverKhan(j, turno, local, jogadores);
        if (local == Tributos.AREA) { // a moeda e a espada da área de melhorias (regras, p. 9)
            List<String> ganhos = new ArrayList<>();
            if (j.getMoedas() > moedas) ganhos.add(Tributo.MOEDA.getNome());
            if (j.getTributos(Tributo.ESPADA) > espadas) ganhos.add(Tributo.ESPADA.getNome());
            ganhosDoKhan = List.copyOf(ganhos);
        }
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
        avisar(Evento.CIDADE, conquistou ? Evento.CONQUISTA : null, fimAntes);
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
        avisar(Evento.YURT, null, fimAntes);
    }

    /**
     * Entrega ao conselheiro da parada o conjunto de tesouros que ele pede e ganha os votos dele (regras, p. 12
     * e 13). O pião precisa ter estado na parada neste turno. No conselheiro que aceita 3 tesouros quaisquer,
     * vão os tesouros que o jogador tem em maior quantidade. O primeiro a entregar ali leva a peça de bônus.
     */
    public void entregarTesouros(int jogador, String paradaId) {
        exigirColuna(jogador, "entregarTesouros");
        Jogador j = jogadores.get(jogador);
        boolean fimAntes = kurultai.isFimDisparado();
        int pecas = j.getBonus().size();
        kurultai.entregar(j, turno, mapa.getConselheiro(paradaId), jogadorAtual);
        ultimoConselheiro = paradaId;
        boolean ganhouBonus = j.getBonus().size() > pecas;
        if (ganhouBonus) ultimoBonus = j.getBonus().get(pecas).getId();
        avisar(Evento.CONSELHEIRO, ganhouBonus ? Evento.BONUS_GANHO : null, fimAntes);
    }

    /**
     * Compra com 2 moedas (as virtuais do turno primeiro) a melhoria do espaço da área (0 a 4) ou, com
     * PILHA_MELHORIAS, a do topo da pilha (regras, p. 11). Ela vai para a coluna da sua cor e só vale a partir do
     * próximo turno em que a coluna for ativada. Se a compra completar uma linha de 3 melhorias, o jogador precisa
     * escolher uma melhoria grátis para a coluna branca antes de encerrar o turno.
     */
    public void comprarMelhoria(int jogador, int espaco) {
        exigirColuna(jogador, "comprarMelhoria");
        boolean fimAntes = kurultai.isFimDisparado();
        Jogador j = jogadores.get(jogador);
        int[] antes = tamanhosDasColunas(j);
        melhorias.comprar(j, turno, espaco);
        registrarMelhoria(j, antes, espaco);
        avisar(Evento.MELHORIA, null, fimAntes);
    }

    /** Escolhe a melhoria grátis da linha completa: a do espaço da área (0 a 4) ou a do topo da pilha. */
    public void escolherMelhoriaGratis(int jogador, int espaco) {
        exigirColuna(jogador, "escolherMelhoriaGratis");
        boolean fimAntes = kurultai.isFimDisparado();
        Jogador j = jogadores.get(jogador);
        int[] antes = tamanhosDasColunas(j);
        melhorias.escolherGratis(j, turno, espaco);
        registrarMelhoria(j, antes, espaco);
        avisar(Evento.MELHORIA_GRATIS, null, fimAntes);
    }

    private static int[] tamanhosDasColunas(Jogador j) {
        int[] tamanhos = new int[4];
        for (int c = 0; c < 4; c++) tamanhos[c] = j.getMelhorias(c).size();
        return tamanhos;
    }

    // Guarda os detalhes da melhoria que acabou de entrar numa coluna do jogador (a única coluna que cresceu).
    private void registrarMelhoria(Jogador j, int[] antes, int espaco) {
        for (int c = 0; c < 4; c++) {
            List<Melhoria> coluna = j.getMelhorias(c);
            if (coluna.size() > antes[c]) {
                ultimaMelhoria = coluna.get(coluna.size() - 1).getId();
                espacoUltimaMelhoria = espaco;
                colunaUltimaMelhoria = c;
            }
        }
    }

    /**
     * Usa uma peça de bônus do jogador (regras, p. 17), uma vez só: "espadas", "moedas" e "yurts" dão 2 peças de
     * tributo do estoque; "cavalos-punho" dá 3 movimentos e 1 punho a mais no turno; "punhos" dá 2 punhos a mais;
     * "bau" sorteia 3 tesouros do saco, e o jogador escolhe 1 com escolherTesouroBau antes de encerrar o turno.
     * O "trono" vale 1 voto na hora em que é ganho e não é usado depois.
     */
    public void usarBonus(int jogador, String peca) {
        exigirColuna(jogador, "usarBonus");
        boolean fimAntes = kurultai.isFimDisparado();
        Bonus b = Bonus.porId(peca);
        bonus.usar(jogadores.get(jogador), turno, b, jogadores);
        ultimoBonus = b.getId();
        avisar(Evento.BONUS_USADO, null, fimAntes);
    }

    /** Fica com o tesouro do baú aberto; os outros saem do jogo. */
    public void escolherTesouroBau(int jogador, String tesouro) {
        exigirColuna(jogador, "escolherTesouroBau");
        boolean fimAntes = kurultai.isFimDisparado();
        Tesouro t = Tesouro.porNome(tesouro);
        bonus.escolherTesouroBau(jogadores.get(jogador), turno, t);
        tesouroDoBau = t.getNome();
        avisar(Evento.BAU, null, fimAntes);
    }

    // ---------------------------------------------------------------- habilidades dos herdeiros (cartelas)

    /** Altani: antes de ativar a coluna, escolhe qual dos 2 piões (0 ou 1) anda e age neste turno. */
    public void selecionarPeao(int jogador, int peao) {
        exigirVez(jogador, "selecionarPeao");
        Habilidades.selecionarPeao(jogadores.get(jogador), turno, peao);
        notificar(Evento.PEAO, jogador);
    }

    /** Chagatai: uma vez por turno, troca 1 movimento ainda não usado por 1 espada do estoque. */
    public void trocarCavaloPorEspada(int jogador) {
        exigirColuna(jogador, "trocarCavaloPorEspada");
        boolean fimAntes = kurultai.isFimDisparado();
        habilidades.trocarCavaloPorEspada(jogadores.get(jogador), turno, jogadores);
        avisar(Evento.CAVALO_POR_ESPADA, null, fimAntes);
    }

    /**
     * Ogedei: uma vez por turno, paga 1 moeda para tomar o tesouro de uma cidade com 2 ou mais tesouros, de qualquer
     * lugar do mapa (regras, p. 20).
     */
    public void tomarTesouroComMoeda(int jogador, String cidade, String tesouro) {
        exigirColuna(jogador, "tomarTesouroComMoeda");
        Tesouro t = Tesouro.porNome(tesouro);
        boolean fimAntes = kurultai.isFimDisparado();
        habilidades.tomarTesouroComMoeda(jogadores.get(jogador), turno, mapa.getCidade(cidade), t);
        ultimaCidade = cidade;
        ultimoTesouro = t.getNome();
        avisar(Evento.TESOURO_COM_MOEDA, null, fimAntes);
    }

    // As 5 consultas abaixo dizem o que o jogador da vez pode fazer agora. Cada uma usa a mesma conferência do
    // comando correspondente (o método erro... da classe responsável), para a regra ficar num lugar só.

    /** O jogador da vez é o Chagatai e ainda pode trocar 1 movimento por 1 espada neste turno. */
    private boolean podeTrocarCavaloPorEspada() {
        return turno != null && habilidades.erroDaTrocaDeCavalo(turno.getJogador(), turno, jogadores) == null;
    }

    /** O jogador da vez é a Altani e ainda pode trocar o pião que joga (antes de ativar a coluna). */
    private boolean podeTrocarPeao() {
        return !jogadores.isEmpty() && !kurultai.isTerminada()
                && Habilidades.erroDaTrocaDePeao(jogadores.get(jogadorAtual), turno) == null;
    }

    /** Cidades de onde o jogador da vez (o Ogedei) pode tomar um tesouro com 1 moeda agora. */
    private List<String> getCidadesParaMoeda() {
        List<String> nomes = new ArrayList<>();
        if (turno == null) return nomes;
        for (Cidade c : mapa.getCidades())
            if (Habilidades.erroDoTesouroComMoeda(turno.getJogador(), turno, c) == null) nomes.add(c.nome());
        return nomes;
    }

    /** Espaços da área (0 a 4) e a pilha (Melhorias.ESPACOS) que o jogador da vez pode pegar agora: comprar ou, se
     *  completou uma linha, escolher como melhoria grátis (o mesmo que o controlador faz no clique). */
    private List<Integer> getEspacosCompraveis() {
        List<Integer> espacos = new ArrayList<>();
        if (turno == null) return espacos;
        Jogador j = turno.getJogador();
        for (int e = 0; e <= Melhorias.ESPACOS; e++) {
            String erro = getMelhoriasGratis() > 0 ? melhorias.erroDaEscolhaGratis(j, turno, e)
                    : melhorias.erroDaCompra(j, turno, e);
            if (erro == null) espacos.add(e);
        }
        return espacos;
    }

    /** Peças de bônus que o jogador da vez pode usar agora. */
    private List<String> getBonusUsaveis() {
        List<String> ids = new ArrayList<>();
        if (turno == null) return ids;
        for (Bonus b : turno.getJogador().getBonus())
            if (PecasBonus.erroAoUsar(turno.getJogador(), turno, b) == null) ids.add(b.getId());
        return ids;
    }

    /** Paradas de todos os piões do jogador, na ordem dos piões (2 para a Altani, 1 para os outros). */
    private List<String> getPosicoes(int jogador) {
        return ids(buscar(jogador, "getPosicoes").getPosicoes());
    }

    /** Pião (0 ou 1) que anda e age no turno; sempre 0 para quem tem 1 pião só. */
    private int getPeaoAtivo(int jogador) {
        return buscar(jogador, "getPeaoAtivo").getPeaoAtivo();
    }

    // Depois de uma ação que pode cumprir uma carta de conselheiro aberto: confere as cartas (regras, p. 18) e
    // avisa, nesta ordem, a ação, o evento que vem junto com ela (CONQUISTA ou BONUS_GANHO, ou null), cada carta
    // ganha e o disparo do fim da partida. fimAntes diz se o fim já estava disparado antes da ação.
    private void avisar(Evento acao, Evento junto, boolean fimAntes) {
        List<CartaAberta> ganhas = kurultai.verificarCartas(jogadorAtual, turno);
        notificar(acao, jogadorAtual);
        if (junto != null) notificar(junto, jogadorAtual);
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
        if (!turno.getBau().isEmpty())
            throw new IllegalStateException("Erro em Jogo.encerrarTurno: " + j.getNome() + " abriu o baú."
                    + " Escolha antes com qual tesouro vai ficar.");
        if (turno.getMelhoriasGratis() > 0 && melhorias.temDisponivel())
            throw new IllegalStateException("Erro em Jogo.encerrarTurno: " + j.getNome() + " completou uma linha de"
                    + " melhorias. Escolha antes a melhoria grátis da coluna branca, na área ou na pilha.");
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
        if (chave == KHAN_AREA) return Tributos.AREA;
        if (chave == PILHA_MELHORIAS) return Melhorias.ESPACOS;
        if (chave == CUSTO_MELHORIA) return Melhorias.CUSTO;
        if (chave == RETANGULOS_MELHORIAS) return porIndice(Melhorias.ESPACOS + 1, this::getRetanguloMelhoria);
        if (chave == PONTOS_BONUS) return porId(getConselheiros(), this::getPontoBonus);
        if (chave == DESCRICOES_BONUS) return porId(getTiposBonus(), Jogo::getDescricaoBonus);
        if (chave == NOMES_CARTAS_SECRETAS) return porId(getTiposCartaSecreta(), Jogo::getNomeCartaSecreta);
        if (chave == OBJETIVOS_CARTAS_SECRETAS) return porId(getTiposCartaSecreta(), Jogo::getObjetivoCartaSecreta);
        if (chave == FAIXAS_CARTAS_SECRETAS) return porId(getTiposCartaSecreta(), c -> getFaixasCartaSecreta(c).clone());
        if (chave == NOMES_REGIOES) return porIndice(3, Jogo::getNomeRegiao);
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
        if (chave == MELHORIAS) return porJogador(i -> porIndice(4, c -> List.copyOf(getMelhorias(i, c))));
        if (chave == BONUS) return porJogador(i -> List.copyOf(getBonus(i)));
        if (chave == BONUS_USADOS)
            return porJogador(i -> getBonus(i).stream().filter(b -> isBonusUsado(i, b)).toList());
        if (chave == PEOES) return porJogador(i -> List.copyOf(getPosicoes(i)));
        if (chave == PEOES_ATIVOS) return porJogador(this::getPeaoAtivo);
        if (chave == OPCOES_SECRETAS) return porJogador(i -> List.copyOf(getOpcoesSecretas(i)));
        if (chave == CARTAS_SECRETAS) return porJogador(this::getCartaSecreta);
        if (chave == MEDIDAS_SECRETAS) return porJogador(this::getMedidaCartaSecreta);
        if (chave == VOTOS_SECRETAS) return porJogador(this::getVotosCartaSecreta);
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
        if (chave == JOGADOR_ESCOLHENDO_SECRETA) return getJogadorEscolhendoSecreta();
        if (chave == MELHORIAS_GRATIS) return getMelhoriasGratis();
        if (chave == VIRTUAIS) return porId(Arrays.stream(Tributo.values()).map(Tributo::getNome).toList(), this::getVirtuais);
        if (chave == TESOUROS_VIRTUAIS) return List.copyOf(getTesourosVirtuais());
        if (chave == TESOUROS_BAU) return List.copyOf(getTesourosBau());
        if (chave == PODE_TROCAR_CAVALO_POR_ESPADA) return podeTrocarCavaloPorEspada();
        if (chave == PODE_TOMAR_TESOURO_COM_MOEDA) return !getCidadesParaMoeda().isEmpty();
        if (chave == PODE_TROCAR_PEAO) return podeTrocarPeao();
        if (chave == CIDADES_PARA_MOEDA) return List.copyOf(getCidadesParaMoeda());
        if (chave == ESPACOS_COMPRAVEIS) return List.copyOf(getEspacosCompraveis());
        if (chave == BONUS_USAVEIS) return List.copyOf(getBonusUsaveis());
        // grupo 5: o tabuleiro
        if (chave == YURTS_NAS_PARADAS) return porId(getParadas(), id -> List.copyOf(getYurtsNaParada(id)));
        if (chave == CIDADES_REVELADAS) return getCidades().stream().filter(this::isCidadeRevelada).toList();
        if (chave == TESOUROS_CIDADES) return porId(getCidades(), c -> List.copyOf(getTesourosCidade(c)));
        if (chave == DONOS_CIDADES) return porId(getCidades(), this::getDonoCidade);
        if (chave == CIDADES_NA_PILHA) return getCidadesNaPilha();
        if (chave == PECAS_PROVINCIAS) return porId(getProvincias(), this::getPecasProvincia);
        if (chave == POSICAO_KHAN) return getPosicaoKhan();
        if (chave == ESTOQUE) return porId(Arrays.stream(Tributo.values()).map(Tributo::getNome).toList(), this::getEstoque);
        if (chave == AREA_MELHORIAS) return Collections.unmodifiableList(getAreaMelhorias()); // com null nos vazios
        if (chave == MELHORIAS_NA_PILHA) return getMelhoriasNaPilha();
        if (chave == BONUS_NOS_CONSELHEIROS) return porId(getConselheiros(), this::getBonusNoConselheiro);
        // grupo 6: Kurultai e fim da partida
        if (chave == VOTOS_NO_CONSELHO) return getVotosNoConselho();
        if (chave == DONOS_CARTAS) return porId(getCartasAbertas(), this::getDonoCarta);
        if (chave == FIM_DISPARADO) return isFimDisparado();
        if (chave == TURNOS_RESTANTES) return getTurnosRestantes();
        if (chave == PARTIDA_TERMINADA) return isPartidaTerminada();
        if (chave == VENCEDORES) return List.copyOf(getVencedores());
        if (chave == VOTOS_INFLUENCIA) return porJogador(i -> porIndice(3, c -> getVotosInfluencia(i, c)));
        if (chave == YURTS_NAS_REGIOES) return porJogador(i -> porIndice(3, c -> getYurtsNaRegiao(i, c)));
        if (chave == TOTAIS_FINAIS) return porJogador(this::getTotalFinal);
        if (chave == DISTANCIAS_KARAKORUM) return porJogador(this::getDistanciaKarakorum);
        // grupo 7: o evento e seus detalhes
        if (chave == EVENTO) return ultimoEvento;
        if (chave == AUTOR_DO_EVENTO) return autorDoEvento;
        if (chave == ULTIMA_PROVINCIA) return ultimaProvincia;
        if (chave == ULTIMA_CIDADE) return ultimaCidade;
        if (chave == ULTIMO_TESOURO) return ultimoTesouro;
        if (chave == ULTIMA_PARADA_YURT) return ultimaParadaYurt;
        if (chave == ULTIMO_CONSELHEIRO) return ultimoConselheiro;
        if (chave == ULTIMA_CARTA) return ultimaCarta;
        if (chave == ULTIMA_MELHORIA) return ultimaMelhoria;
        if (chave == ESPACO_ULTIMA_MELHORIA) return espacoUltimaMelhoria;
        if (chave == COLUNA_ULTIMA_MELHORIA) return colunaUltimaMelhoria;
        if (chave == ULTIMO_BONUS) return ultimoBonus;
        if (chave == TESOURO_DO_BAU) return tesouroDoBau;
        if (chave == GANHOS_DO_KHAN) return ganhosDoKhan;
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

    /** Retorna o id da parada onde está o pião do jogador (o pião ativo, no caso da Altani). */
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

    /** Lugares para onde o Khan pode ir (5): as 4 províncias do Khan (0 a 3) e a área de melhorias (KHAN_AREA). */
    private int getNumeroLocaisKhan() {
        return mapa.getLocaisKhan().size() + 1;
    }

    /** Centro da cabeça do Khan do lugar (0 a 4) na imagem original do tabuleiro: x, y. */
    private int[] getPontoKhan(int local) {
        if (local == Tributos.AREA) return new int[] {Melhorias.KHAN_X, Melhorias.KHAN_Y};
        LocalKhan l = tributos.getLocal(local);
        return new int[] {l.getX(), l.getY()};
    }

    /** Ids das 3 províncias que recebem tributo quando o Khan vai para o lugar (vazia na área de melhorias). */
    private List<String> getProvinciasDoKhan(int local) {
        List<String> ids = new ArrayList<>();
        if (local == Tributos.AREA) return ids;
        for (Provincia p : tributos.getLocal(local).getProvincias()) ids.add(p.getId());
        return ids;
    }

    /** Lugar onde o Khan está (0 a 3, ou KHAN_AREA), ou -1 se ele ainda está fora do tabuleiro. */
    private int getPosicaoKhan() {
        return tributos.getKhan();
    }

    /** Lugar do Khan (0 a 3, ou KHAN_AREA) cuja cabeça contém o ponto (x, y) da imagem original, ou -1. */
    public int localKhanEm(int x, int y) {
        int local = mapa.localKhanEm(x, y);
        if (local == -1 && Melhorias.khanEm(x, y)) local = Tributos.AREA;
        return local;
    }

    // ---------------------------------------------------------------- consultas: peças de bônus

    /** Peça de bônus ainda sobre o conselheiro da parada (por exemplo, "trono"), ou null se alguém já a levou. */
    private String getBonusNoConselheiro(String parada) {
        Bonus b = bonus.getNaMesa(mapa.getConselheiro(parada));
        return b == null ? null : b.getId();
    }

    /** Centro do espaço da peça de bônus do conselheiro na imagem original do tabuleiro: x, y. */
    private int[] getPontoBonus(String parada) {
        return PecasBonus.ponto(mapa.getConselheiro(parada)).clone();
    }

    /** Peças de bônus do jogador, usadas ou não, na ordem em que foram ganhas. */
    private List<String> getBonus(int jogador) {
        List<String> ids = new ArrayList<>();
        for (Bonus b : buscar(jogador, "getBonus").getBonus()) ids.add(b.getId());
        return ids;
    }

    private boolean isBonusUsado(int jogador, String peca) {
        return buscar(jogador, "isBonusUsado").isBonusUsado(Bonus.porId(peca));
    }

    /** Ids das 7 peças de bônus. */
    private static List<String> getTiposBonus() {
        List<String> ids = new ArrayList<>();
        for (Bonus b : Bonus.values()) ids.add(b.getId());
        return ids;
    }

    /** O que a peça de bônus dá, em texto. */
    private static String getDescricaoBonus(String peca) {
        return Bonus.porId(peca).getDescricao();
    }

    /** Tesouros do baú aberto pelo jogador da vez, à espera da escolha (vazia se não houver baú aberto). */
    private List<String> getTesourosBau() {
        List<String> nomes = new ArrayList<>();
        if (turno != null)
            for (Tesouro t : turno.getBau()) nomes.add(t.getNome());
        return nomes;
    }

    // ---------------------------------------------------------------- consultas: melhorias

    /**
     * As 5 melhorias da área, da esquerda para a direita (null num espaço vazio). Cada melhoria é um id
     * "<tipo>-<cor>": o tipo é moeda, espada, yurt, punho, cavalos ou, no tesouro virtual, o nome do tesouro
     * (boi, seda, ferro, pele, especiaria); a cor é vermelha, verde ou amarela. Exemplo: "seda-verde".
     */
    private List<String> getAreaMelhorias() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < Melhorias.ESPACOS; i++) {
            Melhoria m = melhorias.getArea(i);
            ids.add(m == null ? null : m.getId());
        }
        return ids;
    }

    /** Melhorias viradas para baixo na pilha. */
    private int getMelhoriasNaPilha() {
        return melhorias.naPilha();
    }

    /** Melhorias da coluna (0 a 3) do jogador, de baixo para cima, como ids (veja getAreaMelhorias). */
    private List<String> getMelhorias(int jogador, int coluna) {
        Jogador j = buscar(jogador, "getMelhorias");
        exigirNumeroDaColuna(coluna, "getMelhorias");
        List<String> ids = new ArrayList<>();
        for (Melhoria m : j.getMelhorias(coluna)) ids.add(m.getId());
        return ids;
    }

    /** Melhorias grátis (linha completa) que o jogador da vez ainda precisa escolher neste turno. */
    private int getMelhoriasGratis() {
        return turno == null || !melhorias.temDisponivel() ? 0 : turno.getMelhoriasGratis();
    }

    /** Peças virtuais do tipo ("moeda", "espada" ou "yurt") que ainda valem neste turno. */
    private int getVirtuais(String tipo) {
        for (Tributo t : Tributo.values())
            if (t.getNome().equals(tipo)) return turno == null ? 0 : turno.getVirtuais(t);
        throw new IllegalArgumentException("Erro em Jogo.getVirtuais: o tributo " + tipo
                + " não existe (use yurt, espada ou moeda).");
    }

    /** Tesouros virtuais que ainda valem neste turno, um nome por peça (por exemplo, [seda, seda, boi]). */
    private List<String> getTesourosVirtuais() {
        List<String> nomes = new ArrayList<>();
        if (turno == null) return nomes;
        for (Tesouro t : Tesouro.values())
            for (int i = 0; i < turno.getTesourosVirtuais(t); i++) nomes.add(t.getNome());
        return nomes;
    }

    /** Espaço da área de melhorias (0 a 4) ou PILHA_MELHORIAS que contém o ponto (x, y) da imagem original, ou -1. */
    public int espacoMelhoriaEm(int x, int y) {
        return Melhorias.espacoEm(x, y);
    }

    /** Retângulo do espaço (0 a 4, ou PILHA_MELHORIAS) na imagem original do tabuleiro: centro x, centro y, largura, altura. */
    private int[] getRetanguloMelhoria(int espaco) {
        if (espaco == Melhorias.ESPACOS)
            return new int[] {Melhorias.PILHA_X, Melhorias.PILHA_Y, Melhorias.LARGURA, Melhorias.PILHA_ALTURA};
        if (espaco < 0 || espaco >= Melhorias.ESPACOS)
            throw new IllegalArgumentException("Erro em Jogo.getRetanguloMelhoria: o espaço " + espaco + " não existe.");
        return new int[] {Melhorias.PONTOS[espaco][0], Melhorias.PONTOS[espaco][1], Melhorias.LARGURA, Melhorias.ALTURA};
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

    // ---------------------------------------------------------------- pontuação final (regras, p. 14 a 16)

    /**
     * Vencedores pela pontuação final (trilha + influência + carta secreta). No empate, ganha quem tem mais tesouros
     * não entregues e, depois, quem está mais perto de Karakorum; se ainda empatar, todos os empatados vencem.
     * Antes do fim, é o resultado se a partida acabasse agora.
     */
    private List<Integer> getVencedores() {
        return pontuacao.vencedores(jogadores);
    }

    /**
     * Votos de influência do jogador na região da cor da coluna (0 = vermelha/China, 1 = verde/Pérsia,
     * 2 = amarela/Rússia), pelos yurts na região e pelas melhorias nessa coluna.
     */
    private int getVotosInfluencia(int jogador, int coluna) {
        Jogador j = buscar(jogador, "getVotosInfluencia");
        if (coluna < 0 || coluna > 2)
            throw new IllegalArgumentException("Erro em Jogo.getVotosInfluencia: a coluna " + coluna
                    + " não tem região (use 0, 1 ou 2).");
        return pontuacao.influencia(j, Cor.daColuna(coluna), jogadores);
    }

    /** Yurts do jogador na região da cor da coluna (0 a 2), contando os das cidades conquistadas. */
    private int getYurtsNaRegiao(int jogador, int coluna) {
        Regiao r = Cor.daColuna(coluna).getRegiao();
        if (r == null)
            throw new IllegalArgumentException("Erro em Jogo.getYurtsNaRegiao: a coluna " + coluna + " não tem região.");
        return yurts.noMapa(buscar(jogador, "getYurtsNaRegiao"), r);
    }

    /** Nome da região da cor da coluna (0 a 2): "China", "Pérsia" ou "Rússia". */
    private static String getNomeRegiao(int coluna) {
        Regiao r = Cor.daColuna(coluna).getRegiao();
        if (r == null)
            throw new IllegalArgumentException("Erro em Jogo.getNomeRegiao: a coluna " + coluna + " não tem região.");
        return switch (r) {
            case CHINA -> "China";
            case PERSIA -> "Pérsia";
            case RUSSIA -> "Rússia";
        };
    }

    /** Total final do jogador: trilha + influência nas 3 regiões + carta secreta. */
    private int getTotalFinal(int jogador) {
        return pontuacao.total(buscar(jogador, "getTotalFinal"), jogadores);
    }

    /** Passos do pião do jogador até Karakorum (o 2º desempate). */
    private int getDistanciaKarakorum(int jogador) {
        return pontuacao.distanciaKarakorum(buscar(jogador, "getDistanciaKarakorum"));
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

    /** Grava a partida inteira no arquivo (veja a classe Arquivo). */
    public void salvar(Path arquivo) throws IOException {
        arquivo().salvar(arquivo, jogadores, jogadorAtual, turno, selecionada);
    }

    /**
     * Troca a partida pela do arquivo e avisa os observadores. Lê também os arquivos da 2ª e da 3ª iteração.
     * Se o arquivo for inválido, lança IllegalArgumentException e a partida anterior continua.
     */
    public static Jogo carregar(Path arquivo) throws IOException {
        Jogo jogo = getInstance();
        jogo.arquivo().carregar(jogo, arquivo);
        return jogo;
    }

    private Arquivo arquivo() {
        return new Arquivo(mapa, tributos, cidades, melhorias, bonus, kurultai);
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
