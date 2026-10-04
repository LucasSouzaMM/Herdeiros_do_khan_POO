package model;

class Turno {
    private final Jogador jogador;
    private final int coluna;

    Turno(Jogador jogador, int coluna) {
        if (coluna < 0 || coluna > 3)
            throw new IllegalArgumentException("Erro em Turno.Turno: a coluna " + coluna + " não existe (use 0 a 3).");
        this.jogador = jogador;
        this.coluna = coluna;
    }

    Jogador getJogador() {
        return jogador;
    }

    int getColuna() {
        return coluna;
    }
}
