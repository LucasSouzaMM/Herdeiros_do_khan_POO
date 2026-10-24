package model;

class Turno {
    private final Jogador jogador;
    private final int coluna;
    private int movimentos;

    Turno(Jogador jogador, int coluna) {
        this(jogador, coluna, -1);
    }

    // movimentos = -1: todos os cavalos da coluna (§7.1). Outro valor vem de uma partida salva.
    Turno(Jogador jogador, int coluna, int movimentos) {
        if (coluna < 0 || coluna > 3)
            throw new IllegalArgumentException("Erro em Turno.Turno: a coluna " + coluna + " não existe (use 0 a 3).");
        int cavalos = jogador.getHerdeiro().cavalos(coluna);
        if (movimentos < -1 || movimentos > cavalos)
            throw new IllegalArgumentException("Erro em Turno.Turno: " + movimentos
                    + " movimentos não é válido para a coluna " + coluna + " (use 0 a " + cavalos + ").");
        this.jogador = jogador;
        this.coluna = coluna;
        this.movimentos = movimentos == -1 ? cavalos : movimentos;
    }

    Jogador getJogador() {
        return jogador;
    }

    int getColuna() {
        return coluna;
    }

    int getMovimentos() {
        return movimentos;
    }

    void gastar(int n) {
        if (n < 0 || n > movimentos)
            throw new IllegalArgumentException("Erro em Turno.gastar: não é possível gastar " + n
                    + " movimentos (restam " + movimentos + ").");
        movimentos -= n;
    }
}
