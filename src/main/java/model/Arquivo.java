package model;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Salvar e recuperar a partida (RF08; enunciado da 4ª iteração). O arquivo é texto "chave=valor", lido com
// Properties. A versão 4 grava o estado inteiro: jogadores, turno em andamento, províncias e Khan, cidades e saco
// de tesouros, yurts, melhorias, peças de bônus, cartas abertas e secretas e o fim da partida.
// Arquivos antigos (2ª e 3ª iteração, sem a chave "versao") continuam abrindo: o que eles não têm começa como numa
// partida nova (cidades, melhorias, bônus e cartas sorteados, e a escolha das cartas secretas).
// O arquivo é ASCII puro (A3): uma letra com acento vai como \u00e1, que o Properties.load lê de volta.
// Listas são gravadas como valores separados por vírgula; mapas, como "chave:valor" separados por vírgula.
class Arquivo {
    static final int VERSAO = 4;

    private final Mapa mapa;
    private final Tributos tributos;
    private final Cidades cidades;
    private final Melhorias melhorias;
    private final PecasBonus bonus;
    private final Kurultai kurultai;

    Arquivo(Mapa mapa, Tributos tributos, Cidades cidades, Melhorias melhorias, PecasBonus bonus, Kurultai kurultai) {
        this.mapa = mapa;
        this.tributos = tributos;
        this.cidades = cidades;
        this.melhorias = melhorias;
        this.bonus = bonus;
        this.kurultai = kurultai;
    }

    // ---------------------------------------------------------------- salvar

    void salvar(Path arquivo, List<Jogador> jogadores, int atual, Turno turno, int selecionada) throws IOException {
        StringBuilder s = new StringBuilder();
        linha(s, "versao", VERSAO);
        linha(s, "jogadores", jogadores.size());
        linha(s, "jogadorAtual", atual);
        linha(s, "coluna", turno == null ? -1 : turno.getColuna());
        linha(s, "movimentos", turno == null ? 0 : turno.getMovimentos());
        linha(s, "selecionada", selecionada);
        for (int i = 0; i < jogadores.size(); i++) salvarJogador(s, "jogador" + i + ".", jogadores.get(i), jogadores);
        if (turno != null) salvarTurno(s, turno);

        List<String> provincias = new ArrayList<>();
        for (Provincia p : mapa.getProvincias()) provincias.add(p.getId() + ":" + p.getPecas());
        linha(s, "provincias", String.join(",", provincias));
        linha(s, "khan", tributos.getKhan());

        List<String> pilha = new ArrayList<>();
        for (Cidade c : cidades.getPilha()) pilha.add(c.nome());
        linha(s, "cidades.pilha", String.join(",", pilha));
        linha(s, "cidades.saco", tesouros(cidades.getSaco()));
        for (Cidade c : mapa.getCidades()) {
            String k = "cidade." + c.nome() + ".";
            linha(s, k + "revelada", c.isRevelada());
            linha(s, k + "tesouros", tesouros(c.getTesouros()));
            linha(s, k + "dono", jogadores.indexOf(c.getDono()));
        }
        for (Parada p : mapa.getParadas())
            if (!p.getYurts().isEmpty()) {
                List<String> donos = new ArrayList<>();
                for (Jogador j : p.getYurts()) donos.add(String.valueOf(jogadores.indexOf(j)));
                linha(s, "yurts." + p.getId(), String.join(",", donos));
            }

        List<String> area = new ArrayList<>();
        for (int i = 0; i < Melhorias.ESPACOS; i++) {
            Melhoria m = melhorias.getArea(i);
            area.add(m == null ? "" : m.getId());
        }
        linha(s, "melhorias.area", String.join(",", area));
        linha(s, "melhorias.pilha", ids(melhorias.getPilha()));

        List<String> mesa = new ArrayList<>();
        for (Map.Entry<Conselheiro, Bonus> e : bonus.getMesa().entrySet())
            mesa.add(e.getKey().getParada().getId() + ":" + e.getValue().getId());
        linha(s, "bonus.mesa", String.join(",", mesa));

        List<String> cartas = new ArrayList<>(), donos = new ArrayList<>();
        for (CartaAberta c : kurultai.getCartas()) {
            cartas.add(c.getId());
            if (kurultai.getDono(c) != null) donos.add(c.getId() + ":" + jogadores.indexOf(kurultai.getDono(c)));
        }
        linha(s, "cartas", String.join(",", cartas));
        linha(s, "cartas.donos", String.join(",", donos));
        linha(s, "fim.disparado", kurultai.isFimDisparado());
        linha(s, "fim.turnosRestantes", kurultai.getTurnosRestantes());
        linha(s, "fim.terminada", kurultai.isTerminada());
        Files.writeString(arquivo, ascii(s), StandardCharsets.US_ASCII); // US_ASCII recusa o que escapar
    }

    private static void salvarJogador(StringBuilder s, String k, Jogador j, List<Jogador> jogadores) {
        // As 4 chaves da 2ª e 3ª iteração, na mesma forma; a posição é a do pião ativo.
        linha(s, k + "herdeiro", j.getNome());
        linha(s, k + "moedas", j.getMoedas());
        linha(s, k + "posicao", j.getPosicao().getId());
        linha(s, k + "bloqueadas", j.getBloqueadas());
        linha(s, k + "espadas", j.getTributos(Tributo.ESPADA));
        linha(s, k + "yurts", j.getTributos(Tributo.YURT));
        List<String> tesouros = new ArrayList<>();
        for (Tesouro t : Tesouro.values())
            if (j.getTesouros(t) > 0) tesouros.add(t.getNome() + ":" + j.getTesouros(t));
        linha(s, k + "tesouros", String.join(",", tesouros));
        linha(s, k + "votos", j.getVotos());
        List<String> posicoes = new ArrayList<>();
        for (Parada p : j.getPosicoes()) posicoes.add(p.getId());
        linha(s, k + "posicoes", String.join(",", posicoes));
        linha(s, k + "peaoAtivo", j.getPeaoAtivo());
        for (int c = 0; c < 4; c++) linha(s, k + "melhorias" + c, ids(j.getMelhorias(c)));
        linha(s, k + "linhasPremiadas", j.getLinhasPremiadas());
        List<String> pecas = new ArrayList<>(), usadas = new ArrayList<>();
        for (Bonus b : j.getBonus()) {
            pecas.add(b.getId());
            if (j.isBonusUsado(b)) usadas.add(b.getId());
        }
        linha(s, k + "bonus", String.join(",", pecas));
        linha(s, k + "bonusUsados", String.join(",", usadas));
        List<String> opcoes = new ArrayList<>();
        for (CartaSecreta c : j.getOpcoesSecretas()) opcoes.add(c.getId());
        linha(s, k + "opcoesSecretas", String.join(",", opcoes));
        linha(s, k + "cartaSecreta", j.getCartaSecreta() == null ? "" : j.getCartaSecreta().getId());
    }

    private void salvarTurno(StringBuilder s, Turno t) {
        linha(s, "turno.punhos", t.getPunhos());
        linha(s, "turno.khan", t.isKhanPendente());
        List<String> visitadas = new ArrayList<>();
        for (Parada p : t.getVisitadas()) visitadas.add(p.getId());
        linha(s, "turno.visitadas", String.join(",", visitadas));
        List<String> ataques = new ArrayList<>();
        for (Cidade c : mapa.getCidades())
            if (t.getAtaques(c) > 0) ataques.add(c.nome() + ":" + t.getAtaques(c));
        linha(s, "turno.ataques", String.join(",", ataques));
        List<String> virtuais = new ArrayList<>();
        for (Tributo tr : Tributo.values())
            if (t.getVirtuais(tr) > 0) virtuais.add(tr.getNome() + ":" + t.getVirtuais(tr));
        linha(s, "turno.virtuais", String.join(",", virtuais));
        List<String> tesouros = new ArrayList<>();
        for (Tesouro te : Tesouro.values())
            if (t.getTesourosVirtuais(te) > 0) tesouros.add(te.getNome() + ":" + t.getTesourosVirtuais(te));
        linha(s, "turno.tesourosVirtuais", String.join(",", tesouros));
        linha(s, "turno.melhoriasGratis", t.getMelhoriasGratis());
        linha(s, "turno.bau", tesouros(t.getBau()));
        linha(s, "turno.habilidade", t.isHabilidadeUsada());
    }

    private static String ascii(CharSequence texto) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < 128) s.append(c);
            else s.append(String.format("\\u%04x", (int) c));
        }
        return s.toString();
    }

    private static void linha(StringBuilder s, String chave, Object valor) {
        s.append(chave).append('=').append(valor).append('\n');
    }

    private static String tesouros(List<Tesouro> lista) {
        List<String> nomes = new ArrayList<>();
        for (Tesouro t : lista) nomes.add(t.getNome());
        return String.join(",", nomes);
    }

    private static String ids(List<Melhoria> lista) {
        List<String> ids = new ArrayList<>();
        for (Melhoria m : lista) ids.add(m.getId());
        return String.join(",", ids);
    }

    // ---------------------------------------------------------------- carregar

    /**
     * Lê o arquivo e troca a partida do Jogo por ela. Tudo é lido e conferido antes de mudar qualquer coisa: se o
     * arquivo tiver um erro, a partida anterior continua.
     */
    void carregar(Jogo jogo, Path arquivo) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(arquivo)) {
            p.load(r);
        }
        int n = lerInt(p, "jogadores");
        Jogo.validarNumeroJogadores(n);
        List<String> herdeiros = new ArrayList<>();
        for (int i = 0; i < n; i++) herdeiros.add(ler(p, "jogador" + i + ".herdeiro").strip());
        List<Jogador> novos = jogo.criarJogadores(herdeiros);
        for (int i = 0; i < n; i++) {
            String k = "jogador" + i + ".";
            Jogador j = new Jogador(novos.get(i).getHerdeiro(), lerInt(p, k + "moedas"));
            j.posicionarPeoes(mapa.getParada(Mapa.KARAKORUM));
            j.setPosicao(mapa.getParada(ler(p, k + "posicao").strip()));
            if (p.containsKey(k + "bloqueadas")) { // saves da 2ª iteração não têm esta chave: nenhuma coluna bloqueada
                int mascara = lerInt(p, k + "bloqueadas");
                if (mascara < 0 || mascara > 7)
                    throw new IllegalArgumentException("Erro em Arquivo.carregar: o valor de '" + k + "bloqueadas"
                            + "' não é válido (" + mascara + ").");
                j.setBloqueadas(mascara);
            }
            novos.set(i, j);
        }
        int atual = lerInt(p, "jogadorAtual");
        if (atual < 0 || atual >= n)
            throw new IllegalArgumentException("Erro em Arquivo.carregar: o jogador " + (atual + 1)
                    + " não existe (use 1 a " + n + ").");
        int coluna = lerInt(p, "coluna");
        if (coluna < -1 || coluna > 3)
            throw new IllegalArgumentException("Erro em Arquivo.carregar: a coluna " + coluna + " não existe.");

        if (!p.containsKey("versao")) { // 2ª e 3ª iteração: o resto começa como numa partida nova
            Turno t = coluna == -1 ? null : new Turno(novos.get(atual), coluna, lerInt(p, "movimentos"));
            jogo.preparar(novos, atual);
            jogo.concluirCarga(t, -1);
            return;
        }
        Estado e = lerEstado(p, novos, coluna);
        jogo.preparar(novos, atual);
        aplicar(e, novos);
        Turno t = null;
        if (coluna != -1) {
            t = new Turno(novos.get(atual), coluna);
            t.restaurar(e.movimentos, e.punhos, e.khan, e.visitadas, e.ataques, e.virtuais, e.tesourosVirtuais,
                    e.melhoriasGratis, e.bau, e.habilidade);
        }
        jogo.concluirCarga(t, e.selecionada);
    }

    // Tudo o que a versão 4 grava além do formato antigo, já conferido.
    private static class Estado {
        int selecionada, movimentos, punhos, melhoriasGratis, khanLugar, turnosRestantes;
        boolean khan, habilidade, fimDisparado, terminada;
        List<Parada> visitadas = new ArrayList<>();
        Map<Cidade, Integer> ataques = new HashMap<>();
        Map<Tributo, Integer> virtuais = new EnumMap<>(Tributo.class);
        Map<Tesouro, Integer> tesourosVirtuais = new EnumMap<>(Tesouro.class);
        List<Tesouro> bau = new ArrayList<>();
        Map<Provincia, Integer> provincias = new HashMap<>();
        List<Cidade> pilhaCidades = new ArrayList<>();
        List<Tesouro> saco = new ArrayList<>();
        Map<Cidade, Boolean> reveladas = new HashMap<>();
        Map<Cidade, List<Tesouro>> tesourosCidade = new HashMap<>();
        Map<Cidade, Integer> donosCidade = new HashMap<>();
        Map<Parada, List<Integer>> yurts = new HashMap<>();
        Melhoria[] area = new Melhoria[Melhorias.ESPACOS];
        List<Melhoria> pilhaMelhorias = new ArrayList<>();
        Map<Conselheiro, Bonus> mesa = new LinkedHashMap<>();
        List<CartaAberta> cartas = new ArrayList<>();
        Map<CartaAberta, Integer> donosCartas = new EnumMap<>(CartaAberta.class);
        // Por jogador:
        List<int[]> tributos = new ArrayList<>(); // espadas, yurts, votos, pião ativo, linhas premiadas
        List<Map<Tesouro, Integer>> tesouros = new ArrayList<>();
        List<List<Parada>> posicoes = new ArrayList<>();
        List<List<List<Melhoria>>> melhorias = new ArrayList<>();
        List<List<Bonus>> pecas = new ArrayList<>(), usadas = new ArrayList<>();
        List<List<CartaSecreta>> opcoes = new ArrayList<>();
        List<CartaSecreta> secretas = new ArrayList<>();
    }

    private Estado lerEstado(Properties p, List<Jogador> jogadores, int coluna) {
        int n = jogadores.size();
        Estado e = new Estado();
        e.selecionada = lerInt(p, "selecionada");
        if (e.selecionada < -1 || e.selecionada > 3)
            throw new IllegalArgumentException("Erro em Arquivo.carregar: a coluna marcada " + e.selecionada + " não existe.");
        for (int i = 0; i < n; i++) {
            String k = "jogador" + i + ".";
            Jogador j = jogadores.get(i);
            int[] numeros = {naoNegativo(p, k + "espadas"), naoNegativo(p, k + "yurts"), naoNegativo(p, k + "votos"),
                    lerInt(p, k + "peaoAtivo"), naoNegativo(p, k + "linhasPremiadas")};
            e.tributos.add(numeros);
            Map<Tesouro, Integer> tesouros = new EnumMap<>(Tesouro.class);
            for (Map.Entry<String, Integer> t : mapa(p, k + "tesouros").entrySet())
                tesouros.put(Tesouro.porNome(t.getKey()), t.getValue());
            e.tesouros.add(tesouros);
            List<Parada> posicoes = new ArrayList<>();
            for (String id : lista(p, k + "posicoes")) posicoes.add(mapa.getParada(id));
            if (posicoes.size() != j.getPosicoes().size())
                throw new IllegalArgumentException("Erro em Arquivo.carregar: " + j.getNome() + " tem "
                        + j.getPosicoes().size() + " pião(ões), e o arquivo tem " + posicoes.size() + ".");
            if (numeros[3] < 0 || numeros[3] >= posicoes.size())
                throw new IllegalArgumentException("Erro em Arquivo.carregar: o pião ativo de " + j.getNome() + " não existe.");
            e.posicoes.add(posicoes);
            List<List<Melhoria>> colunas = new ArrayList<>();
            for (int c = 0; c < 4; c++) {
                List<Melhoria> ms = new ArrayList<>();
                for (String id : lista(p, k + "melhorias" + c)) ms.add(Melhoria.porId(id));
                if (ms.size() > Jogador.MELHORIAS_POR_COLUNA)
                    throw new IllegalArgumentException("Erro em Arquivo.carregar: a coluna " + (c + 1) + " de "
                            + j.getNome() + " tem melhorias demais.");
                colunas.add(ms);
            }
            e.melhorias.add(colunas);
            e.pecas.add(bonus(lista(p, k + "bonus")));
            e.usadas.add(bonus(lista(p, k + "bonusUsados")));
            List<CartaSecreta> opcoes = new ArrayList<>();
            for (String id : lista(p, k + "opcoesSecretas")) opcoes.add(CartaSecreta.porId(id));
            e.opcoes.add(opcoes);
            String secreta = ler(p, k + "cartaSecreta").strip();
            e.secretas.add(secreta.isEmpty() ? null : CartaSecreta.porId(secreta));
        }
        if (coluna != -1) {
            e.movimentos = naoNegativo(p, "movimentos");
            e.punhos = naoNegativo(p, "turno.punhos");
            e.khan = lerBoolean(p, "turno.khan");
            for (String id : lista(p, "turno.visitadas")) e.visitadas.add(mapa.getParada(id));
            for (Map.Entry<String, Integer> a : mapa(p, "turno.ataques").entrySet())
                e.ataques.put(mapa.getCidade(a.getKey()), a.getValue());
            for (Map.Entry<String, Integer> v : mapa(p, "turno.virtuais").entrySet()) e.virtuais.put(tributo(v.getKey()), v.getValue());
            for (Map.Entry<String, Integer> v : mapa(p, "turno.tesourosVirtuais").entrySet())
                e.tesourosVirtuais.put(Tesouro.porNome(v.getKey()), v.getValue());
            e.melhoriasGratis = naoNegativo(p, "turno.melhoriasGratis");
            for (String t : lista(p, "turno.bau")) e.bau.add(Tesouro.porNome(t));
            e.habilidade = lerBoolean(p, "turno.habilidade");
        }
        for (Map.Entry<String, Integer> pr : mapa(p, "provincias").entrySet()) {
            if (pr.getValue() > Provincia.MAX_PECAS)
                throw new IllegalArgumentException("Erro em Arquivo.carregar: a província " + pr.getKey()
                        + " tem peças demais (" + pr.getValue() + ").");
            e.provincias.put(mapa.getProvincia(pr.getKey()), pr.getValue());
        }
        e.khanLugar = lerInt(p, "khan");
        if (e.khanLugar < -1 || e.khanLugar > Tributos.AREA)
            throw new IllegalArgumentException("Erro em Arquivo.carregar: o lugar do Khan " + e.khanLugar + " não existe.");
        for (String nome : lista(p, "cidades.pilha")) e.pilhaCidades.add(mapa.getCidade(nome));
        for (String t : lista(p, "cidades.saco")) e.saco.add(Tesouro.porNome(t));
        for (Cidade c : mapa.getCidades()) {
            String k = "cidade." + c.nome() + ".";
            e.reveladas.put(c, lerBoolean(p, k + "revelada"));
            List<Tesouro> tesouros = new ArrayList<>();
            for (String t : lista(p, k + "tesouros")) tesouros.add(Tesouro.porNome(t));
            e.tesourosCidade.put(c, tesouros);
            e.donosCidade.put(c, indice(lerInt(p, k + "dono"), n, k + "dono", true));
        }
        for (Parada pa : mapa.getParadas()) {
            List<Integer> donos = new ArrayList<>();
            for (String d : lista(p, "yurts." + pa.getId(), true)) donos.add(indice(inteiro(d, "yurts." + pa.getId()), n, "yurts", false));
            if (!donos.isEmpty()) e.yurts.put(pa, donos);
        }
        List<String> area = lista(p, "melhorias.area");
        for (int i = 0; i < Melhorias.ESPACOS; i++)
            e.area[i] = i < area.size() && !area.get(i).isEmpty() ? Melhoria.porId(area.get(i)) : null;
        for (String id : lista(p, "melhorias.pilha")) e.pilhaMelhorias.add(Melhoria.porId(id));
        for (String par : lista(p, "bonus.mesa")) {
            String[] partes = par.split(":");
            if (partes.length != 2)
                throw new IllegalArgumentException("Erro em Arquivo.carregar: o valor '" + par + "' de 'bonus.mesa' não é válido.");
            e.mesa.put(mapa.getConselheiro(partes[0]), Bonus.porId(partes[1]));
        }
        for (String id : lista(p, "cartas")) e.cartas.add(CartaAberta.porId(id));
        for (Map.Entry<String, Integer> d : mapa(p, "cartas.donos").entrySet())
            e.donosCartas.put(CartaAberta.porId(d.getKey()), indice(d.getValue(), n, "cartas.donos", false));
        e.fimDisparado = lerBoolean(p, "fim.disparado");
        e.turnosRestantes = naoNegativo(p, "fim.turnosRestantes");
        e.terminada = lerBoolean(p, "fim.terminada");
        return e;
    }

    // Troca o estado sorteado por Jogo.preparar pelo estado lido do arquivo.
    private void aplicar(Estado e, List<Jogador> jogadores) {
        for (int i = 0; i < jogadores.size(); i++) {
            Jogador j = jogadores.get(i);
            int[] numeros = e.tributos.get(i);
            for (int k = 0; k < numeros[0]; k++) j.ganhar(Tributo.ESPADA);
            for (int k = 0; k < numeros[1]; k++) j.ganhar(Tributo.YURT);
            j.ganharVotos(numeros[2]);
            for (Map.Entry<Tesouro, Integer> t : e.tesouros.get(i).entrySet())
                for (int k = 0; k < t.getValue(); k++) j.ganharTesouro(t.getKey());
            List<Parada> posicoes = e.posicoes.get(i);
            for (int k = 0; k < posicoes.size(); k++) j.setPosicao(k, posicoes.get(k));
            j.setPeaoAtivo(numeros[3]);
            j.limparMelhorias(); // as do Jochi vêm do arquivo
            for (int c = 0; c < 4; c++)
                for (Melhoria m : e.melhorias.get(i).get(c)) j.adicionarMelhoria(c, m);
            for (int k = 0; k < numeros[4]; k++) j.premiarLinha();
            for (Bonus b : e.pecas.get(i)) j.ganharBonus(b);
            for (Bonus b : e.usadas.get(i)) j.usarBonus(b);
            j.receberCartasSecretas(e.opcoes.get(i));
            if (e.secretas.get(i) != null) j.escolherCartaSecreta(e.secretas.get(i));
        }
        for (Map.Entry<Provincia, Integer> pr : e.provincias.entrySet()) pr.getKey().setPecas(pr.getValue());
        tributos.restaurar(e.khanLugar);
        for (Cidade c : mapa.getCidades()) {
            c.reiniciar();
            if (e.reveladas.get(c)) c.revelar(e.tesourosCidade.get(c));
            int dono = e.donosCidade.get(c);
            if (dono != -1) c.conquistar(jogadores.get(dono));
        }
        cidades.restaurar(e.pilhaCidades, e.saco);
        for (Parada pa : mapa.getParadas()) pa.getYurts().clear(); // inclusive o do Tolui, que vem do arquivo
        for (Map.Entry<Parada, List<Integer>> y : e.yurts.entrySet())
            for (int dono : y.getValue()) y.getKey().getYurts().add(jogadores.get(dono));
        melhorias.restaurar(e.area, e.pilhaMelhorias);
        bonus.restaurar(e.mesa);
        Map<CartaAberta, Jogador> donos = new EnumMap<>(CartaAberta.class);
        for (Map.Entry<CartaAberta, Integer> d : e.donosCartas.entrySet()) donos.put(d.getKey(), jogadores.get(d.getValue()));
        kurultai.restaurar(e.cartas, donos, e.fimDisparado, e.turnosRestantes, e.terminada);
    }

    // ---------------------------------------------------------------- leitura das chaves

    private static String ler(Properties p, String chave) {
        String valor = p.getProperty(chave);
        if (valor == null)
            throw new IllegalArgumentException("Erro em Arquivo.carregar: falta a chave '" + chave + "' no arquivo.");
        return valor;
    }

    private static int lerInt(Properties p, String chave) {
        return inteiro(ler(p, chave), chave);
    }

    private static int inteiro(String valor, String chave) {
        try {
            return Integer.parseInt(valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Erro em Arquivo.carregar: o valor de '" + chave
                    + "' não é um número inteiro (" + valor + ").");
        }
    }

    private static int naoNegativo(Properties p, String chave) {
        int n = lerInt(p, chave);
        if (n < 0)
            throw new IllegalArgumentException("Erro em Arquivo.carregar: o valor de '" + chave + "' é negativo (" + n + ").");
        return n;
    }

    private static boolean lerBoolean(Properties p, String chave) {
        String valor = ler(p, chave).strip();
        if (!valor.equals("true") && !valor.equals("false"))
            throw new IllegalArgumentException("Erro em Arquivo.carregar: o valor de '" + chave
                    + "' precisa ser true ou false (" + valor + ").");
        return valor.equals("true");
    }

    private static List<String> lista(Properties p, String chave) {
        return lista(p, chave, false);
    }

    // Valores separados por vírgula. Uma chave opcional que falta vale como lista vazia.
    private static List<String> lista(Properties p, String chave, boolean opcional) {
        String valor = opcional ? p.getProperty(chave, "") : ler(p, chave);
        List<String> itens = new ArrayList<>();
        if (valor.strip().isEmpty()) return itens;
        for (String item : valor.split(",", -1)) itens.add(item.strip());
        return itens;
    }

    // Pares "nome:número" separados por vírgula.
    private static Map<String, Integer> mapa(Properties p, String chave) {
        Map<String, Integer> pares = new LinkedHashMap<>();
        for (String par : lista(p, chave)) {
            String[] partes = par.split(":");
            if (partes.length != 2)
                throw new IllegalArgumentException("Erro em Arquivo.carregar: o valor '" + par + "' de '" + chave
                        + "' não é válido (use nome:número).");
            int n = inteiro(partes[1], chave);
            if (n < 0)
                throw new IllegalArgumentException("Erro em Arquivo.carregar: o valor '" + par + "' de '" + chave
                        + "' é negativo.");
            pares.put(partes[0].strip(), n);
        }
        return pares;
    }

    private static int indice(int i, int n, String chave, boolean podeSerNenhum) {
        if ((podeSerNenhum && i == -1) || (i >= 0 && i < n)) return i;
        throw new IllegalArgumentException("Erro em Arquivo.carregar: o jogador " + i + " de '" + chave + "' não existe.");
    }

    private static Tributo tributo(String nome) {
        for (Tributo t : Tributo.values())
            if (t.getNome().equals(nome)) return t;
        throw new IllegalArgumentException("Erro em Arquivo.carregar: o tributo " + nome + " não existe.");
    }

    private static List<Bonus> bonus(List<String> ids) {
        List<Bonus> pecas = new ArrayList<>();
        for (String id : ids) pecas.add(Bonus.porId(id));
        return pecas;
    }
}
