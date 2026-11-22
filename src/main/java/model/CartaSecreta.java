package model;

// As 10 cartas de conselheiro secreto (regras, p. 6 e 19). Cada jogador fica com 1 no começo da partida; no fim,
// ela dá 1, 2 ou 3 votos conforme a medida do objetivo alcançar cada faixa.
enum CartaSecreta {
    VIAJANTE("viajante", "Viajante", "Yurts ligados num só grupo (pode ter ramificações)", 3, 6, 8),
    SOLDADO("soldado", "Soldado", "Melhorias", 4, 7, 10),
    COLECIONADOR("colecionador", "Colecionador", "Melhorias com símbolos diferentes (cada tesouro é um símbolo)", 4, 6, 7),
    SACERDOTE("sacerdote", "Sacerdote", "Pares de melhorias com o mesmo símbolo", 1, 2, 4),
    GOVERNADOR("governador", "Governador", "Regiões com 2 yurts e 2 melhorias na coluna da cor", 1, 2, 3),
    BAJULADOR("bajulador", "Bajulador", "Províncias do Khan com 2 yurts ao redor", 2, 3, 4),
    PREFEITO("prefeito", "Prefeito", "Cidades com 2 yurts ao redor (não dentro)", 2, 3, 4),
    CIDADAO("cidadao", "Cidadão", "Cidades com 1 yurt ao redor (não dentro)", 3, 6, 8),
    CAVALEIRO("cavaleiro", "Cavaleiro", "Melhorias de cavalo ou de punho", 2, 4, 5),
    MENSAGEIRO("mensageiro", "Mensageiro", "Paradas duplas com 1 yurt (Karakorum não conta)", 2, 4, 6);

    private final String id;
    private final String nome;
    private final String objetivo;
    private final int[] faixas; // medida mínima para 1, 2 e 3 votos

    CartaSecreta(String id, String nome, String objetivo, int... faixas) {
        this.id = id;
        this.nome = nome;
        this.objetivo = objetivo;
        this.faixas = faixas;
    }

    String getId() {
        return id;
    }

    String getNome() {
        return nome;
    }

    String getObjetivo() {
        return objetivo;
    }

    int[] getFaixas() {
        return faixas.clone();
    }

    /** Votos (0 a 3) que a carta dá para a medida alcançada. */
    int votos(int medida) {
        int v = 0;
        for (int f : faixas)
            if (medida >= f) v++;
        return v;
    }

    static CartaSecreta porId(String id) {
        for (CartaSecreta c : values())
            if (c.id.equals(id)) return c;
        throw new IllegalArgumentException("Erro em CartaSecreta.porId: a carta secreta " + id + " não existe.");
    }
}
