package model;

class Jogador {
    private final String nome;
    private final int moedas;

    Jogador(String nome, int moedas) {
        if (nome == null || nome.isBlank())
            throw new IllegalArgumentException("Erro em Jogador.Jogador: o nome do jogador está vazio.");
        if (moedas < 0)
            throw new IllegalArgumentException("Erro em Jogador.Jogador: o número de moedas é negativo (" + moedas + ").");
        this.nome = nome.strip();
        this.moedas = moedas;
    }

    String getNome() {
        return nome;
    }

    int getMoedas() {
        return moedas;
    }
}
