package model;

// Ações das colunas vermelha, verde e amarela, lidas da cartela de cada herdeiro: cavalos, punhos
// e a coluna que move o Khan. A coluna branca é igual para todos: 2 cavalos, Khan e reativação das outras
// colunas. Uma das colunas coloridas tem também o ícone da moeda virtual, que vale 1 moeda no turno em que
// a coluna é ativada (regras, p. 16).
enum Herdeiro {
    ALTANI("Altani", new int[] {1, 2, 1}, new int[] {2, 1, 1}, 0, 2),
    JOCHI("Jochi", new int[] {1, 1, 2}, new int[] {1, 2, 1}, 1, 0),
    CHAGATAI("Chagatai", new int[] {1, 1, 2}, new int[] {2, 1, 1}, 0, 1),
    TOLUI("Tolui", new int[] {1, 2, 1}, new int[] {1, 1, 2}, 2, 0),
    OGEDEI("Ogedei", new int[] {2, 1, 1}, new int[] {1, 1, 2}, 2, 1);

    static final int BRANCA = 3;
    private static final int CAVALOS_BRANCA = 2;

    private final String nome;
    private final int[] cavalos;
    private final int[] punhos;
    private final int colunaKhan;
    private final int colunaMoeda;

    Herdeiro(String nome, int[] cavalos, int[] punhos, int colunaKhan, int colunaMoeda) {
        this.nome = nome;
        this.cavalos = cavalos;
        this.punhos = punhos;
        this.colunaKhan = colunaKhan;
        this.colunaMoeda = colunaMoeda;
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

    /** A coluna tem o ícone da moeda virtual impresso na cartela. */
    boolean moeda(int coluna) {
        return coluna == colunaMoeda;
    }

    static Herdeiro porNome(String nome) {
        for (Herdeiro h : values())
            if (h.nome.equals(nome)) return h;
        throw new IllegalArgumentException("Erro em Herdeiro.porNome: o herdeiro " + nome
                + " não existe (use Altani, Jochi, Chagatai, Tolui ou Ogedei).");
    }
}
