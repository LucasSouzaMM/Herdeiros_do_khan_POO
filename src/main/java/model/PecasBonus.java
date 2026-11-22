package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Peças de bônus (regras, p. 4, 13 e 17): uma sobre cada conselheiro no começo, a peça do primeiro jogador que
// entregar tesouros ali e o efeito de cada uma quando o jogador a usa.
class PecasBonus {
    // Centro do espaço do bônus de cada conselheiro na imagem original do tabuleiro (o quadrado roxo com o "1").
    private static final Map<String, int[]> PONTOS = Map.of(
            "r4", new int[] {1384, 158},
            "r11", new int[] {2417, 284},
            "p1", new int[] {236, 1042},
            Mapa.KARAKORUM, new int[] {1452, 1404},
            "c7", new int[] {2746, 1693},
            "p8", new int[] {713, 2093},
            "c8", new int[] {1691, 2392});

    private final Mapa mapa;
    private final Tributos tributos;
    private final Cidades cidades;
    private final Map<Conselheiro, Bonus> naMesa = new LinkedHashMap<>(); // peças ainda sobre os conselheiros

    PecasBonus(Mapa mapa, Tributos tributos, Cidades cidades) {
        this.mapa = mapa;
        this.tributos = tributos;
        this.cidades = cidades;
    }

    /** §5: as 7 peças são embaralhadas, e cada conselheiro recebe uma. */
    void sortear(Random aleatorio) {
        List<Bonus> pecas = new ArrayList<>(Arrays.asList(Bonus.values()));
        Collections.shuffle(pecas, aleatorio);
        preparar(pecas);
    }

    // Usado também pelos testes: os conselheiros recebem as peças na ordem de Mapa.getConselheiros().
    void preparar(List<Bonus> pecas) {
        naMesa.clear();
        List<Conselheiro> conselheiros = mapa.getConselheiros();
        for (int i = 0; i < conselheiros.size() && i < pecas.size(); i++) naMesa.put(conselheiros.get(i), pecas.get(i));
    }

    /** Peça ainda sobre o conselheiro, ou null se alguém já a levou. */
    Bonus getNaMesa(Conselheiro c) {
        return naMesa.get(c);
    }

    /** As peças ainda sobre os conselheiros. */
    Map<Conselheiro, Bonus> getMesa() {
        return java.util.Collections.unmodifiableMap(naMesa);
    }

    // Ao carregar uma partida salva.
    void restaurar(Map<Conselheiro, Bonus> mesa) {
        naMesa.clear();
        naMesa.putAll(mesa);
    }

    static int[] ponto(Conselheiro c) {
        return PONTOS.get(c.getParada().getId());
    }

    /**
     * O jogador entregou tesouros ao conselheiro: se a peça ainda estiver lá, ele a leva (regras, p. 13).
     * O trono vale 1 voto na hora, então já fica usado. Retorna a peça, ou null.
     */
    Bonus receber(Jogador j, Conselheiro c) {
        Bonus b = naMesa.remove(c);
        if (b == null) return null;
        j.ganharBonus(b);
        if (b == Bonus.TRONO) j.usarBonus(b);
        return b;
    }

    /**
     * Usa a peça de bônus do jogador, uma vez só, na sua vez e depois de ativar uma coluna (regras, p. 17).
     * Os tributos saem do estoque, enquanto houver. O baú sorteia 3 tesouros do saco, e o jogador escolhe 1 com
     * escolherTesouroBau antes de encerrar o turno.
     */
    void usar(Jogador j, Turno turno, Bonus b, List<Jogador> jogadores) {
        if (!j.getBonus().contains(b))
            throw new IllegalArgumentException("Erro em PecasBonus.usar: " + j.getNome() + " não tem a peça de bônus "
                    + b.getId() + ".");
        String erro = erroAoUsar(j, turno, b);
        if (erro != null) throw new IllegalStateException("Erro em PecasBonus.usar: " + erro);
        switch (b) {
            case ESPADAS -> ganhar(j, Tributo.ESPADA, 2, jogadores);
            case MOEDAS -> ganhar(j, Tributo.MOEDA, 2, jogadores);
            case YURTS -> ganhar(j, Tributo.YURT, 2, jogadores);
            case CAVALOS_PUNHO -> {
                turno.ganharMovimentos(3);
                turno.ganharPunhos(1);
            }
            case PUNHOS -> turno.ganharPunhos(2);
            case BAU -> turno.abrirBau(cidades.tirarDoSaco(3));
            case TRONO -> throw new IllegalStateException("Erro em PecasBonus.usar: o trono vale na hora em que é ganho.");
        }
        j.usarBonus(b);
    }

    /** Por que o jogador não pode usar agora a peça de bônus que ele tem, ou null se pode. */
    static String erroAoUsar(Jogador j, Turno turno, Bonus b) {
        if (j.isBonusUsado(b)) return j.getNome() + " já usou a peça de bônus " + b.getId() + ".";
        if (!turno.getBau().isEmpty()) return j.getNome() + " precisa antes escolher um dos tesouros do baú.";
        return null;
    }

    private void ganhar(Jogador j, Tributo t, int n, List<Jogador> jogadores) {
        for (int i = 0; i < n; i++)
            if (tributos.estoque(t, jogadores) > 0) j.ganhar(t);
    }

    /** O jogador fica com 1 dos tesouros do baú; os outros saem do jogo (C3, regras, p. 17: "Descarte os outros"). */
    void escolherTesouroBau(Jogador j, Turno turno, Tesouro t) {
        List<Tesouro> bau = new ArrayList<>(turno.getBau());
        if (bau.isEmpty())
            throw new IllegalStateException("Erro em PecasBonus.escolherTesouroBau: " + j.getNome()
                    + " não tem um baú aberto.");
        if (!bau.remove(t))
            throw new IllegalArgumentException("Erro em PecasBonus.escolherTesouroBau: o baú não tem " + t.getNome() + ".");
        j.ganharTesouro(t);
        turno.fecharBau();
    }
}
