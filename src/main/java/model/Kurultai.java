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
    private List<Jogador> jogadores = new ArrayList<>();
    private final List<CartaAberta> cartas = new ArrayList<>(); // as 4 cartas de conselheiro aberto, na ordem da mesa
    private final Map<CartaAberta, Jogador> donosCartas = new EnumMap<>(CartaAberta.class);
    private boolean fimDisparado; // o conselho ficou cheio ou todas as cidades foram conquistadas
    private int turnosRestantes;  // depois de disparado o fim: turnos que ainda faltam
    private boolean terminada;

    Kurultai(Yurts yurts, Cidades cidades) {
        this.yurts = yurts;
        this.cidades = cidades;
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
     * As peças de bônus do primeiro a entregar vêm na 4ª iteração.
     */
    void entregar(Jogador j, Turno turno, Conselheiro c, int atual) {
        if (!turno.getVisitadas().contains(c.getParada()))
            throw new IllegalArgumentException("Erro em Kurultai.entregar: " + j.getNome() + " não esteve na parada "
                    + c.getParada().getId() + " neste turno.");
        List<Tesouro> entrega = entrega(c, j);
        if (entrega == null)
            throw new IllegalStateException("Erro em Kurultai.entregar: " + j.getNome() + " não tem os tesouros que o"
                    + " conselheiro de " + c.getParada().getId() + " pede (" + descreverPedido(c) + ").");
        for (Tesouro t : entrega) j.gastarTesouro(t);
        ganharVotos(j, c.getVotos(), atual);
    }

    // Os tesouros que sairiam da reserva do jogador numa entrega ao conselheiro, ou null se ele não os tiver.
    // No conselheiro que aceita 3 tesouros quaisquer, vão os que o jogador tem em maior quantidade.
    static List<Tesouro> entrega(Conselheiro c, Jogador j) {
        List<Tesouro> entrega = new ArrayList<>();
        if (c.getPedido() == Conselheiro.PAR) {
            Tesouro t = c.getTipos().get(0);
            if (j.getTesouros(t) < 2) return null;
            entrega.add(t);
            entrega.add(t);
        } else if (c.getPedido() == Conselheiro.TIPOS) {
            for (Tesouro t : c.getTipos())
                if (j.getTesouros(t) == 0) return null;
            entrega.addAll(c.getTipos());
        } else {
            Map<Tesouro, Integer> resta = new EnumMap<>(Tesouro.class);
            for (Tesouro t : Tesouro.values()) resta.put(t, j.getTesouros(t));
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
            if (turno.getVisitadas().contains(c.getParada()) && entrega(c, turno.getJogador()) != null) possiveis.add(c);
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

    // O jogador da vez ganha 1 voto por carta ainda livre que ele cumprir agora.
    // Retorna as cartas que ele ganhou, na ordem da mesa.
    List<CartaAberta> verificarCartas(int atual) {
        Jogador j = jogadores.get(atual);
        List<CartaAberta> ganhas = new ArrayList<>();
        for (CartaAberta c : cartas)
            if (!donosCartas.containsKey(c) && cumpre(j, c)) {
                donosCartas.put(c, j);
                ganharVotos(j, 1, atual);
                ganhas.add(c);
            }
        return ganhas;
    }

    private boolean cumpre(Jogador j, CartaAberta c) {
        return switch (c) {
            case ESPADAS4 -> j.getTributos(Tributo.ESPADA) >= 4;
            case YURTS6 -> yurts.noMapa(j, null) >= 6;
            case YURTS10 -> yurts.noMapa(j, null) >= 10;
            case TESOUROS4 -> j.getTotalTesouros() >= 4;
            case DIFERENTES5 -> {
                int tipos = 0;
                for (Tesouro t : Tesouro.values())
                    if (j.getTesouros(t) > 0) tipos++;
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
        };
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

    /**
     * Jogadores com mais votos na trilha. Nesta iteração o vencedor sai só dos votos ganhos durante a partida;
     * a influência nas regiões, as cartas secretas e o desempate vêm na 4ª iteração.
     */
    List<Integer> vencedores() {
        int maior = -1;
        for (Jogador j : jogadores) maior = Math.max(maior, j.getVotos());
        List<Integer> vencedores = new ArrayList<>();
        for (int i = 0; i < jogadores.size(); i++)
            if (jogadores.get(i).getVotos() == maior) vencedores.add(i);
        return vencedores;
    }
}
