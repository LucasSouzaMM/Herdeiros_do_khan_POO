package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// O conselho Kurultai (regras, p. 12 a 14 e 18): entregas de tesouros aos conselheiros, cartas de conselheiro
// aberto, votos na trilha e o fim da partida.
class Kurultai {
    // A ficha neutra em 10 / 14 / 16 / 17 votos (2 / 3 / 4 / 5 jogadores) dispara o fim da partida.
    private static final int[] LIMITE_VOTOS = {0, 0, 10, 14, 16, 17};

    private final Yurts yurts;
    private final Cidades cidades;
    private final PecasBonus bonus;
    private List<Jogador> jogadores = new ArrayList<>();
    private final List<CartaAberta> cartas = new ArrayList<>(); // as 4 cartas de conselheiro aberto, na ordem da mesa
    private final Map<CartaAberta, Jogador> donosCartas = new EnumMap<>(CartaAberta.class);
    private boolean fimDisparado; // o conselho ficou cheio ou todas as cidades foram conquistadas
    private int turnosRestantes;  // depois de disparado o fim: turnos que ainda faltam
    private boolean terminada;

    Kurultai(Yurts yurts, Cidades cidades, PecasBonus bonus) {
        this.yurts = yurts;
        this.cidades = cidades;
        this.bonus = bonus;
    }

    /** Começo da partida: 4 cartas sorteadas e o fim ainda não disparado. */
    void preparar(List<Jogador> jogadores, Random aleatorio) {
        this.jogadores = jogadores;
        List<String> ids = new ArrayList<>();
        for (CartaAberta c : CartaAberta.values()) ids.add(c.getId());
        Collections.shuffle(ids, aleatorio);
        prepararCartas(ids.subList(0, 4));
        fimDisparado = false;
        turnosRestantes = 0;
        terminada = false;
    }

    // Usado também pelos testes, para começar com cartas conhecidas.
    void prepararCartas(List<String> ids) {
        cartas.clear();
        donosCartas.clear();
        for (String id : ids) cartas.add(CartaAberta.porId(id));
    }

    // ---------------------------------------------------------------- conselheiros

    /**
     * Entrega ao conselheiro o conjunto de tesouros que ele pede e dá os votos dele ao jogador. O pião precisa ter
     * estado na parada neste turno. Pode-se entregar várias vezes ao mesmo conselheiro.
     * O primeiro jogador a entregar ali leva a peça de bônus do conselheiro (regras, p. 13).
     */
    void entregar(Jogador j, Turno turno, Conselheiro c, int atual) {
        if (!turno.getVisitadas().contains(c.getParada()))
            throw new IllegalArgumentException("Erro em Kurultai.entregar: " + j.getNome() + " não esteve na parada "
                    + c.getParada().getId() + " neste turno.");
        Entrega entrega = entrega(c, j, turno);
        if (entrega == null)
            throw new IllegalStateException("Erro em Kurultai.entregar: " + j.getNome() + " não tem os tesouros que o"
                    + " conselheiro de " + c.getParada().getId() + " pede (" + descreverPedido(c) + ")."
                    + " Cada entrega usa no máximo 1 tesouro virtual, junto com tesouros reais.");
        for (Tesouro t : entrega.reais()) j.gastarTesouro(t);
        for (Tesouro t : entrega.virtuais()) turno.usarTesouroVirtual(t);
        ganharVotos(j, c.getVotos(), atual);
        if (bonus.receber(j, c) == Bonus.TRONO) ganharVotos(j, 1, atual); // o primeiro a entregar leva a peça de bônus
    }

    /** Tesouros de uma entrega: os que saem da reserva do jogador e os virtuais do turno (regras, p. 16). */
    record Entrega(List<Tesouro> reais, List<Tesouro> virtuais) {
    }

    // Os tesouros que sairiam da reserva do jogador numa entrega ao conselheiro, ou null se ele não os tiver.
    static List<Tesouro> entrega(Conselheiro c, Jogador j) {
        Entrega e = entrega(c, j, null);
        return e == null ? null : e.reais();
    }

    // A entrega ao conselheiro com os tesouros reais do jogador e os virtuais do turno, ou null se não der.
    // No conselheiro que aceita 3 tesouros quaisquer, vão os que o jogador tem em maior quantidade.
    // C1 (regras, p. 16: "uma única entrega com um tesouro a menos"): no máximo 1 tesouro virtual por entrega.
    // Como toda entrega tem 2 ou 3 tesouros, sobra sempre pelo menos 1 real. O virtual é usado quando dá, porque
    // some no fim do turno.
    static Entrega entrega(Conselheiro c, Jogador j, Turno turno) {
        Map<Tesouro, Integer> reais = new EnumMap<>(Tesouro.class);
        for (Tesouro t : Tesouro.values()) reais.put(t, j.getTesouros(t));
        for (Tesouro virtual : Tesouro.values()) {
            if (turno == null || turno.getTesourosVirtuais(virtual) == 0) continue;
            Map<Tesouro, Integer> comVirtual = new EnumMap<>(reais);
            comVirtual.merge(virtual, 1, Integer::sum);
            List<Tesouro> pedido = escolher(c, comVirtual);
            if (pedido == null) continue;
            if (!pedido.remove(virtual)) { // o conselheiro não pediu esse tipo
                if (c.getPedido() != Conselheiro.QUAISQUER) continue;
                pedido.remove(pedido.size() - 1); // 3 quaisquer: o virtual entra no lugar de um real
            }
            return new Entrega(pedido, List.of(virtual));
        }
        List<Tesouro> pedido = escolher(c, reais);
        return pedido == null ? null : new Entrega(pedido, List.of());
    }

    // Os tesouros que o conselheiro pede, tirados das quantidades dadas, ou null se não houver o bastante.
    private static List<Tesouro> escolher(Conselheiro c, Map<Tesouro, Integer> quantidades) {
        List<Tesouro> entrega = new ArrayList<>();
        if (c.getPedido() == Conselheiro.PAR) {
            Tesouro t = c.getTipos().get(0);
            if (quantidades.get(t) < 2) return null;
            entrega.add(t);
            entrega.add(t);
        } else if (c.getPedido() == Conselheiro.TIPOS) {
            for (Tesouro t : c.getTipos())
                if (quantidades.get(t) == 0) return null;
            entrega.addAll(c.getTipos());
        } else {
            Map<Tesouro, Integer> resta = new EnumMap<>(quantidades);
            for (int i = 0; i < 3; i++) {
                Tesouro maior = null;
                for (Tesouro t : Tesouro.values())
                    if (resta.get(t) > 0 && (maior == null || resta.get(t) > resta.get(maior))) maior = t;
                if (maior == null) return null;
                entrega.add(maior);
                resta.merge(maior, -1, Integer::sum);
            }
        }
        return entrega;
    }

    private static String descreverPedido(Conselheiro c) {
        if (c.getPedido() == Conselheiro.PAR) return "2 " + c.getTipos().get(0).getNome();
        if (c.getPedido() == Conselheiro.QUAISQUER) return "3 tesouros quaisquer";
        List<String> nomes = new ArrayList<>();
        for (Tesouro t : c.getTipos()) nomes.add(t.getNome());
        return String.join(", ", nomes);
    }

    /** Conselheiros a quem o jogador do turno pode entregar tesouros agora. */
    List<Conselheiro> paraEntrega(Turno turno, List<Conselheiro> conselheiros) {
        List<Conselheiro> possiveis = new ArrayList<>();
        if (turno == null) return possiveis;
        for (Conselheiro c : conselheiros)
            if (turno.getVisitadas().contains(c.getParada()) && entrega(c, turno.getJogador(), turno) != null)
                possiveis.add(c);
        return possiveis;
    }

    // ---------------------------------------------------------------- votos e cartas

    // Cada voto avança a ficha do jogador e a ficha neutra na trilha do Kurultai.
    private void ganharVotos(Jogador j, int n, int atual) {
        j.ganharVotos(n);
        verificarFim(atual);
    }

    /** Votos de todos os jogadores somados: a posição da ficha neutra na trilha do Kurultai. */
    int votosNoConselho() {
        int total = 0;
        for (Jogador j : jogadores) total += j.getVotos();
        return total;
    }

    // O jogador da vez ganha 1 voto por carta ainda livre que ele cumprir agora. As espadas e os tesouros
    // virtuais do turno também contam (regras, p. 16). Retorna as cartas que ele ganhou, na ordem da mesa.
    List<CartaAberta> verificarCartas(int atual, Turno turno) {
        Jogador j = jogadores.get(atual);
        List<CartaAberta> ganhas = new ArrayList<>();
        for (CartaAberta c : cartas)
            if (!donosCartas.containsKey(c) && cumpre(j, turno, c)) {
                donosCartas.put(c, j);
                ganharVotos(j, 1, atual);
                ganhas.add(c);
            }
        return ganhas;
    }

    private boolean cumpre(Jogador j, Turno turno, CartaAberta c) {
        return switch (c) {
            case ESPADAS4 -> (turno == null ? j.getTributos(Tributo.ESPADA) : turno.disponivel(Tributo.ESPADA)) >= 4;
            case YURTS6 -> yurts.noMapa(j, null) >= 6;
            case YURTS10 -> yurts.noMapa(j, null) >= 10;
            case TESOUROS4 -> {
                int n = 0;
                for (Tesouro t : Tesouro.values()) n += tesouros(j, turno, t);
                yield n >= 4;
            }
            case DIFERENTES5 -> {
                int tipos = 0;
                for (Tesouro t : Tesouro.values())
                    if (tesouros(j, turno, t) > 0) tipos++;
                yield tipos == Tesouro.values().length;
            }
            case REGIOES1, REGIOES2 -> {
                int minimo = c == CartaAberta.REGIOES1 ? 1 : 2;
                boolean todas = true;
                for (Regiao r : Regiao.values())
                    if (yurts.noMapa(j, r) < minimo) todas = false;
                yield todas;
            }
            case CIDADES2 -> cidades.conquistadas(j) >= 2;
            case CIDADES3 -> cidades.conquistadas(j) >= 3;
            case BONUS2 -> j.getBonus().size() >= 2; // as usadas também contam
            case BONUS3 -> j.getBonus().size() >= 3;
            case LINHA -> j.linhasCompletas() >= 1;
            case COLUNAS2 -> colunasCom(j, 2) >= 2;
            case COLUNA4 -> colunasCom(j, Jogador.MELHORIAS_POR_COLUNA) >= 1;
        };
    }

    // Colunas coloridas do jogador com pelo menos n melhorias (a coluna branca não conta).
    private static int colunasCom(Jogador j, int n) {
        int colunas = 0;
        for (int c = 0; c < 3; c++)
            if (j.getMelhorias(c).size() >= n) colunas++;
        return colunas;
    }

    // Tesouros do tipo que contam para as cartas: os do jogador e os virtuais do turno.
    private static int tesouros(Jogador j, Turno turno, Tesouro t) {
        return j.getTesouros(t) + (turno == null ? 0 : turno.getTesourosVirtuais(t));
    }

    List<CartaAberta> getCartas() {
        return Collections.unmodifiableList(cartas);
    }

    /** Jogador que ganhou a carta, ou null se ela ainda está livre. */
    Jogador getDono(CartaAberta c) {
        return donosCartas.get(c);
    }

    // ---------------------------------------------------------------- fim da partida

    // A ficha neutra no limite ou todas as cidades conquistadas disparam o fim. A rodada atual termina
    // (até a vez voltar ao 1º jogador) e há mais uma rodada completa.
    void verificarFim(int atual) {
        if (fimDisparado || jogadores.isEmpty()) return;
        if (votosNoConselho() >= limite() || cidades.todasConquistadas()) {
            fimDisparado = true;
            turnosRestantes = (jogadores.size() - 1 - atual) + jogadores.size();
        }
    }

    /**
     * Fim de um turno: retorna true se ele era o último da partida, que então termina. Se o fim já foi
     * disparado, conta um turno a menos.
     */
    boolean encerrarTurno() {
        if (fimDisparado && turnosRestantes == 0) {
            terminada = true;
            return true;
        }
        if (fimDisparado) turnosRestantes--;
        return false;
    }

    int limite() {
        return jogadores.isEmpty() ? 0 : LIMITE_VOTOS[jogadores.size()];
    }

    boolean isFimDisparado() {
        return fimDisparado;
    }

    int getTurnosRestantes() {
        return turnosRestantes;
    }

    boolean isTerminada() {
        return terminada;
    }

    // Ao carregar uma partida salva (os jogadores já foram dados em preparar).
    void restaurar(List<CartaAberta> cartas, Map<CartaAberta, Jogador> donos, boolean fimDisparado, int turnosRestantes,
            boolean terminada) {
        this.cartas.clear();
        this.cartas.addAll(cartas);
        donosCartas.clear();
        donosCartas.putAll(donos);
        this.fimDisparado = fimDisparado;
        this.turnosRestantes = turnosRestantes;
        this.terminada = terminada;
    }
}
