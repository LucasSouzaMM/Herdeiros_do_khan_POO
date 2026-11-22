package model;

class Jogador {
    static final int YURTS = 12; // yurts de cada jogador (regras, p. 3)
    static final int MELHORIAS_POR_COLUNA = 4; // espaços de melhoria em cada coluna da cartela

    private final Herdeiro herdeiro;
    private int moedas; // peças de moeda (§5: as moedas iniciais também são peças de tributo)
    private int espadas;
    private int yurts; // peças de tributo de yurt, ainda não usadas
    private final java.util.Map<Tesouro, Integer> tesouros = new java.util.EnumMap<>(Tesouro.class);
    private int votos; // posição da ficha do jogador na trilha do Kurultai
    // Posição de cada pião. Só a Altani tem 2 (cartela da Altani); o pião ativo é o que anda e age no turno.
    private final java.util.List<Parada> posicoes = new java.util.ArrayList<>();
    private int peaoAtivo;
    private int bloqueadas; // bit i ligado = coluna i bloqueada (colunas 0 a 2)
    // Melhorias de cada coluna da cartela (0 a 3), de baixo para cima (regras, p. 11).
    private final java.util.List<java.util.List<Melhoria>> melhorias = new java.util.ArrayList<>();
    private int linhasPremiadas; // linhas completas que já deram a melhoria grátis da coluna branca
    // Peças de bônus ganhas, na ordem em que foram ganhas, e as já usadas (regras, p. 17).
    private final java.util.List<Bonus> bonus = new java.util.ArrayList<>();
    private final java.util.Set<Bonus> bonusUsados = java.util.EnumSet.noneOf(Bonus.class);
    // As 2 cartas de conselheiro secreto recebidas no começo e a escolhida (null até a escolha).
    private final java.util.List<CartaSecreta> opcoesSecretas = new java.util.ArrayList<>();
    private CartaSecreta cartaSecreta;

    Jogador(Herdeiro herdeiro, int moedas) {
        if (herdeiro == null)
            throw new IllegalArgumentException("Erro em Jogador.Jogador: o herdeiro do jogador está vazio.");
        if (moedas < 0)
            throw new IllegalArgumentException("Erro em Jogador.Jogador: o número de moedas é negativo (" + moedas + ").");
        this.herdeiro = herdeiro;
        this.moedas = moedas;
        for (int c = 0; c < 4; c++) melhorias.add(new java.util.ArrayList<>());
        for (int k = 0; k < (herdeiro == Herdeiro.ALTANI ? 2 : 1); k++) posicoes.add(null);
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

    /** Posição do pião ativo. */
    Parada getPosicao() {
        return posicoes.get(peaoAtivo);
    }

    /** Move o pião ativo. */
    void setPosicao(Parada posicao) {
        posicoes.set(peaoAtivo, posicao);
    }

    /** Posições de todos os piões do jogador (2 para a Altani). */
    java.util.List<Parada> getPosicoes() {
        return java.util.Collections.unmodifiableList(posicoes);
    }

    /** Coloca todos os piões do jogador na parada (começo da partida). */
    void posicionarPeoes(Parada p) {
        for (int k = 0; k < posicoes.size(); k++) posicoes.set(k, p);
    }

    void setPosicao(int peao, Parada p) {
        exigirPeao(peao);
        posicoes.set(peao, p);
    }

    int getPeaoAtivo() {
        return peaoAtivo;
    }

    void setPeaoAtivo(int peao) {
        exigirPeao(peao);
        peaoAtivo = peao;
    }

    private void exigirPeao(int peao) {
        if (peao < 0 || peao >= posicoes.size())
            throw new IllegalArgumentException("Erro em Jogador.setPeaoAtivo: " + getNome() + " não tem o pião " + (peao + 1)
                    + " (tem " + posicoes.size() + ").");
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

    /** Melhorias da coluna (0 a 3), de baixo para cima. */
    java.util.List<Melhoria> getMelhorias(int coluna) {
        return java.util.Collections.unmodifiableList(melhorias.get(Cor.daColuna(coluna).coluna()));
    }

    /** Coloca a melhoria no próximo espaço livre da coluna, de baixo para cima. */
    void adicionarMelhoria(int coluna, Melhoria m) {
        java.util.List<Melhoria> lista = melhorias.get(Cor.daColuna(coluna).coluna());
        if (lista.size() >= MELHORIAS_POR_COLUNA)
            throw new IllegalStateException("Erro em Jogador.adicionarMelhoria: a coluna " + (coluna + 1) + " de "
                    + getNome() + " já tem " + MELHORIAS_POR_COLUNA + " melhorias.");
        lista.add(m);
    }

    // Usado nos testes: tira todas as melhorias (por exemplo, as 2 sorteadas do Jochi no começo da partida).
    void limparMelhorias() {
        for (java.util.List<Melhoria> coluna : melhorias) coluna.clear();
        linhasPremiadas = 0;
    }

    /** Linhas de melhorias completas: as linhas preenchidas nas 3 colunas coloridas (a branca não conta). */
    int linhasCompletas() {
        int linhas = MELHORIAS_POR_COLUNA;
        for (int c = 0; c < 3; c++) linhas = Math.min(linhas, melhorias.get(c).size());
        return linhas;
    }

    int getLinhasPremiadas() {
        return linhasPremiadas;
    }

    void premiarLinha() {
        linhasPremiadas++;
    }

    /** Peças de bônus do jogador, usadas ou não, na ordem em que foram ganhas. */
    java.util.List<Bonus> getBonus() {
        return java.util.Collections.unmodifiableList(bonus);
    }

    void ganharBonus(Bonus b) {
        if (!bonus.contains(b)) bonus.add(b);
    }

    boolean isBonusUsado(Bonus b) {
        return bonusUsados.contains(b);
    }

    void usarBonus(Bonus b) {
        bonusUsados.add(b);
    }

    java.util.List<CartaSecreta> getOpcoesSecretas() {
        return java.util.Collections.unmodifiableList(opcoesSecretas);
    }

    void receberCartasSecretas(java.util.List<CartaSecreta> cartas) {
        opcoesSecretas.clear();
        opcoesSecretas.addAll(cartas);
        cartaSecreta = null;
    }

    /** A carta secreta escolhida, ou null se o jogador ainda não escolheu. */
    CartaSecreta getCartaSecreta() {
        return cartaSecreta;
    }

    void escolherCartaSecreta(CartaSecreta c) {
        cartaSecreta = c;
    }
}