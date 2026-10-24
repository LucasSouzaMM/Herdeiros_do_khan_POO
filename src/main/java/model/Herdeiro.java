package model;

// Cavalos das colunas vermelha, verde, amarela e branca, lidos da cartela de cada herdeiro.
enum Herdeiro {
    ALTANI("Altani", 1, 2, 1, 2),
    JOCHI("Jochi", 1, 1, 2, 2),
    CHAGATAI("Chagatai", 1, 1, 2, 2),
    TOLUI("Tolui", 1, 2, 1, 2),
    OGEDEI("Ogedei", 2, 1, 1, 2);

    private final String nome;
    private final int[] cavalos;

    Herdeiro(String nome, int... cavalos) {
        this.nome = nome;
        this.cavalos = cavalos;
    }

    String getNome() {
        return nome;
    }

    int cavalos(int coluna) {
        return cavalos[coluna];
    }

    static Herdeiro porNome(String nome) {
        for (Herdeiro h : values())
            if (h.nome.equals(nome)) return h;
        throw new IllegalArgumentException("Erro em Herdeiro.porNome: o herdeiro " + nome
                + " não existe (use Altani, Jochi, Chagatai, Tolui ou Ogedei).");
    }
}
