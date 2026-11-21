package model;

// Cartas de conselheiro aberto (regras, p. 18). 4 ficam em jogo; o primeiro jogador a cumprir uma carta ganha
// 1 voto e a carta sai do jogo. As cartas que dependem de melhorias ou de peças de bônus (2 e 3 bônus, linha de
// melhorias, 2 colunas com 2 melhorias, 4 melhorias numa coluna) entram com essas regras, na 4ª iteração.
enum CartaAberta {
    ESPADAS4("espadas4", "Primeiro a ter 4 espadas"),
    YURTS6("yurts6", "Primeiro a ter 6 yurts no mapa"),
    YURTS10("yurts10", "Primeiro a ter 10 yurts no mapa"),
    TESOUROS4("tesouros4", "Primeiro a ter 4 tesouros"),
    DIFERENTES5("diferentes5", "Primeiro a ter 5 tesouros diferentes"),
    REGIOES1("regioes1", "Primeiro a ter 1 yurt em cada região"),
    REGIOES2("regioes2", "Primeiro a ter 2 yurts em cada região"),
    CIDADES2("cidades2", "Primeiro a conquistar 2 cidades"),
    CIDADES3("cidades3", "Primeiro a conquistar 3 cidades");

    private final String id;
    private final String descricao;

    CartaAberta(String id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    String getId() {
        return id;
    }

    String getDescricao() {
        return descricao;
    }

    static CartaAberta porId(String id) {
        for (CartaAberta c : values())
            if (c.id.equals(id)) return c;
        throw new IllegalArgumentException("Erro em CartaAberta.porId: a carta " + id + " não existe.");
    }
}
