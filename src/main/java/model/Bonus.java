package model;

// As 7 peças de bônus (regras, p. 4, 13 e 17). Uma fica em cada conselheiro, e o primeiro jogador a entregar
// tesouros ali leva a peça. Cada peça é usada uma vez só; depois fica virada para baixo com o jogador.
enum Bonus {
    TRONO("trono", "1 voto na hora"),
    ESPADAS("espadas", "2 peças de espada"),
    MOEDAS("moedas", "2 peças de moeda"),
    YURTS("yurts", "2 peças de yurt"),
    CAVALOS_PUNHO("cavalos-punho", "3 movimentos e 1 punho a mais no turno"),
    PUNHOS("punhos", "2 punhos a mais no turno"),
    BAU("bau", "sorteia 3 tesouros do saco e fica com 1");

    private final String id;
    private final String descricao;

    Bonus(String id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    String getId() {
        return id;
    }

    String getDescricao() {
        return descricao;
    }

    static Bonus porId(String id) {
        for (Bonus b : values())
            if (b.id.equals(id)) return b;
        throw new IllegalArgumentException("Erro em Bonus.porId: a peça de bônus " + id + " não existe (use trono,"
                + " espadas, moedas, yurts, cavalos-punho, punhos ou bau).");
    }
}
