package model;

class Jogador {
    private final Herdeiro herdeiro;
    private final int moedas;
    private Parada posicao;

    Jogador(Herdeiro herdeiro, int moedas) {
        if (herdeiro == null)
            throw new IllegalArgumentException("Erro em Jogador.Jogador: o herdeiro do jogador está vazio.");
        if (moedas < 0)
            throw new IllegalArgumentException("Erro em Jogador.Jogador: o número de moedas é negativo (" + moedas + ").");
        this.herdeiro = herdeiro;
        this.moedas = moedas;
    }

    Herdeiro getHerdeiro() {
        return herdeiro;
    }

    String getNome() {
        return herdeiro.getNome();
    }

    int getMoedas() {
        return moedas;
    }

    Parada getPosicao() {
        return posicao;
    }

    void setPosicao(Parada posicao) {
        this.posicao = posicao;
    }
}
