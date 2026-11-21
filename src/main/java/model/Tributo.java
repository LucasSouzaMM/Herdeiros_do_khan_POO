package model;

// Os 3 tipos de peça de tributo (§7.2): a espada ataca cidades, o yurt é colocado no tabuleiro
// e as moedas compram melhorias. As moedas iniciais dos jogadores (§5) também são peças de moeda.
enum Tributo {
    YURT("yurt", 10),
    ESPADA("espada", 10),
    MOEDA("moeda", 15);

    private final String nome;
    private final int total; // peças desse tipo no jogo (regras, p. 3): estoque + províncias + jogadores

    Tributo(String nome, int total) {
        this.nome = nome;
        this.total = total;
    }

    int getTotal() {
        return total;
    }

    String getNome() {
        return nome;
    }
}
