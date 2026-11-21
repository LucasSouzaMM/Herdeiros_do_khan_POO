package model;

class Jogador {
    static final int YURTS = 12; // yurts de cada jogador (regras, p. 3)

    private final Herdeiro herdeiro;
    private int moedas; // peças de moeda (§5: as moedas iniciais também são peças de tributo)
    private int espadas;
    private int yurts; // peças de tributo de yurt, ainda não usadas
    private final java.util.Map<Tesouro, Integer> tesouros = new java.util.EnumMap<>(Tesouro.class);
    private int votos; // posição da ficha do jogador na trilha do Kurultai
    private Parada posicao;
    private int bloqueadas; // bit i ligado = coluna i bloqueada (colunas 0 a 2)

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

    /** Quantas peças de tributo do tipo o jogador tem guardadas. */
    int getTributos(Tributo t) {
        return switch (t) {
            case MOEDA -> moedas;
            case ESPADA -> espadas;
            case YURT -> yurts;
        };
    }

    /** Devolve n peças de tributo do tipo ao estoque, ao usá-las. */
    void gastar(Tributo t, int n) {
        if (n < 0 || n > getTributos(t))
            throw new IllegalArgumentException("Erro em Jogador.gastar: " + getNome() + " não tem " + n + " peça(s) de "
                    + t.getNome() + " (tem " + getTributos(t) + ").");
        switch (t) {
            case MOEDA -> moedas -= n;
            case ESPADA -> espadas -= n;
            case YURT -> yurts -= n;
        }
    }

    int getTesouros(Tesouro t) {
        return tesouros.getOrDefault(t, 0);
    }

    void ganharTesouro(Tesouro t) {
        tesouros.merge(t, 1, Integer::sum);
    }

    /** Entrega 1 tesouro do tipo a um conselheiro: a peça sai do jogo. */
    void gastarTesouro(Tesouro t) {
        if (getTesouros(t) == 0)
            throw new IllegalStateException("Erro em Jogador.gastarTesouro: " + getNome() + " não tem " + t.getNome() + ".");
        tesouros.merge(t, -1, Integer::sum);
    }

    int getTotalTesouros() {
        int n = 0;
        for (int q : tesouros.values()) n += q;
        return n;
    }

    int getVotos() {
        return votos;
    }

    void ganharVotos(int n) {
        votos += n;
    }

    /** Guarda 1 peça de tributo do tipo (§7.2). */
    void ganhar(Tributo t) {
        switch (t) {
            case MOEDA -> moedas++;
            case ESPADA -> espadas++;
            case YURT -> yurts++;
        }
    }

    Parada getPosicao() {
        return posicao;
    }

    void setPosicao(Parada posicao) {
        this.posicao = posicao;
    }

    boolean isBloqueada(int coluna) {
        return ((bloqueadas >> coluna) & 1) != 0;
    }

    void bloquear(int coluna) {
        bloqueadas |= 1 << coluna;
    }

    void liberarColunas() {
        bloqueadas = 0;
    }

    int getBloqueadas() {
        return bloqueadas;
    }

    void setBloqueadas(int mascara) {
        if (mascara < 0 || mascara > 7)
            throw new IllegalArgumentException("Erro em Jogador.setBloqueadas: o valor " + mascara
                    + " não é válido (use 0 a 7).");
        bloqueadas = mascara;
    }
}