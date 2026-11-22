package model;

// Cores das 4 colunas da cartela, da esquerda para a direita (regras, p. 11 e 16). Cada coluna colorida corresponde
// à região da mesma cor no tabuleiro: as rotas da China são vermelhas, as da Pérsia verdes e as da Rússia amarelas.
// A coluna branca não tem região. As melhorias também têm cor (menos a branca, que aceita melhoria de qualquer cor).
enum Cor {
    VERMELHA("vermelha", Regiao.CHINA),
    VERDE("verde", Regiao.PERSIA),
    AMARELA("amarela", Regiao.RUSSIA),
    BRANCA("branca", null);

    private final String nome;
    private final Regiao regiao;

    Cor(String nome, Regiao regiao) {
        this.nome = nome;
        this.regiao = regiao;
    }

    String getNome() {
        return nome;
    }

    /** Região da mesma cor, ou null para a branca. */
    Regiao getRegiao() {
        return regiao;
    }

    /** Coluna da cartela com esta cor (0 a 3). */
    int coluna() {
        return ordinal();
    }

    static Cor daColuna(int coluna) {
        if (coluna < 0 || coluna >= values().length)
            throw new IllegalArgumentException("Erro em Cor.daColuna: a coluna " + coluna + " não existe (use 0 a 3).");
        return values()[coluna];
    }

    static Cor porNome(String nome) {
        for (Cor c : values())
            if (c.nome.equals(nome)) return c;
        throw new IllegalArgumentException("Erro em Cor.porNome: a cor " + nome
                + " não existe (use vermelha, verde, amarela ou branca).");
    }
}
