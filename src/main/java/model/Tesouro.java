package model;

// Os 5 tipos de peça de tesouro, 8 de cada (40 no total). Ficam nas cidades até serem tomados com espadas (regras, p. 10).
enum Tesouro {
    BOI("boi"),
    SEDA("seda"),
    FERRO("ferro"),
    PELE("pele"),
    ESPECIARIA("especiaria");

    static final int POR_TIPO = 8;

    private final String nome;

    Tesouro(String nome) {
        this.nome = nome;
    }

    String getNome() {
        return nome;
    }

    static Tesouro porNome(String nome) {
        for (Tesouro t : values())
            if (t.nome.equals(nome)) return t;
        throw new IllegalArgumentException("Erro em Tesouro.porNome: o tesouro " + nome
                + " não existe (use boi, seda, ferro, pele ou especiaria).");
    }
}
