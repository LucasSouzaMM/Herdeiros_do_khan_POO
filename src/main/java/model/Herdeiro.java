package model;

// Ações das colunas vermelha, verde e amarela, lidas da cartela de cada herdeiro: cavalos, punhos
// e a coluna que move o Khan. A coluna branca é igual para todos: 2 cavalos, Khan e reativação das outras
// colunas. A moeda virtual de algumas colunas só vale para as melhorias (4ª iteração).
enum Herdeiro {
    ALTANI("Altani", new int[] {1, 2, 1}, new int[] {2, 1, 1}, 0),
    JOCHI("Jochi", new int[] {1, 1, 2}, new int[] {1, 2, 1}, 1),
    CHAGATAI("Chagatai", new int[] {1, 1, 2}, new int[] {2, 1, 1}, 0),
    TOLUI("Tolui", new int[] {1, 2, 1}, new int[] {1, 1, 2}, 2),
    OGEDEI("Ogedei", new int[] {2, 1, 1}, new int[] {1, 1, 2}, 2);

    static final int BRANCA = 3;
    private static final int CAVALOS_BRANCA = 2;

    private final String nome;
    private final int[] cavalos;
    private final int[] punhos;
    private final int colunaKhan;

    Herdeiro(String nome, int[] cavalos, int[] punhos, int colunaKhan) {
        this.nome = nome;
        this.cavalos = cavalos;
        this.punhos = punhos;
        this.colunaKhan = colunaKhan;
    }

    String getNome() {
        return nome;
    }

    int cavalos(int coluna) {
        return coluna == BRANCA ? CAVALOS_BRANCA : cavalos[coluna];
    }

    int punhos(int coluna) {
        return coluna == BRANCA ? 0 : punhos[coluna];
    }

    /** A coluna obriga a mover o Khan. */
    boolean khan(int coluna) {
        return coluna == BRANCA || coluna == colunaKhan;
    }

    static Herdeiro porNome(String nome) {
        for (Herdeiro h : values())
            if (h.nome.equals(nome)) return h;
        throw new IllegalArgumentException("Erro em Herdeiro.porNome: o herdeiro " + nome
                + " não existe (use Altani, Jochi, Chagatai, Tolui ou Ogedei).");
    }
}
