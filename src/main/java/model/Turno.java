package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// O turno de um jogador depois de ativar uma coluna: o que ainda falta fazer, as paradas por onde o pião passou e
// os recursos virtuais da coluna (regras, p. 16), que valem só neste turno.
class Turno {
    private final Jogador jogador;
    private final int coluna;
    private int movimentos;
    private int punhos;
    private boolean khan; // o Khan ainda precisa ser movido
    private final List<Parada> visitadas = new ArrayList<>();
    private final Map<Cidade, Integer> ataques = new java.util.HashMap<>(); // tesouros tomados de cada cidade
    // Recursos virtuais: a moeda impressa na cartela e as melhorias de moeda, espada, yurt e tesouro da coluna.
    private final Map<Tributo, Integer> virtuais = new EnumMap<>(Tributo.class);
    private final Map<Tesouro, Integer> tesourosVirtuais = new EnumMap<>(Tesouro.class);
    private int melhoriasGratis; // melhorias grátis da linha completa que o jogador ainda precisa escolher
    private final List<Tesouro> bau = new ArrayList<>(); // tesouros do baú aberto, à espera da escolha
    private boolean habilidadeUsada; // Chagatai e Ogedei usam a habilidade uma vez por turno

    Turno(Jogador jogador, int coluna) {
        this(jogador, coluna, -1);
    }

    // movimentos = -1: todos os cavalos da coluna (§7.1). Outro valor vem de uma partida salva.
    // Os punhos, o Khan e os recursos virtuais sempre começam com tudo o que a coluna dá (§7).
    // Valem as melhorias que já estavam na coluna ao ativá-la: as compradas neste turno só valem nos próximos.
    Turno(Jogador jogador, int coluna, int movimentos) {
        if (coluna < 0 || coluna > 3)
            throw new IllegalArgumentException("Erro em Turno.Turno: a coluna " + coluna + " não existe (use 0 a 3).");
        Herdeiro h = jogador.getHerdeiro();
        int cavalos = h.cavalos(coluna);
        int punhosDaColuna = h.punhos(coluna);
        if (h.moeda(coluna)) virtuais.merge(Tributo.MOEDA, 1, Integer::sum);
        for (Melhoria m : jogador.getMelhorias(coluna)) {
            switch (m.getTipo()) {
                case CAVALOS -> cavalos += 2;
                case PUNHO -> punhosDaColuna++;
                case MOEDA -> virtuais.merge(Tributo.MOEDA, 1, Integer::sum);
                case ESPADA -> virtuais.merge(Tributo.ESPADA, 1, Integer::sum);
                case YURT -> virtuais.merge(Tributo.YURT, 1, Integer::sum);
                case TESOURO -> tesourosVirtuais.merge(m.getTesouro(), 1, Integer::sum);
            }
        }
        if (movimentos < -1 || movimentos > cavalos)
            throw new IllegalArgumentException("Erro em Turno.Turno: " + movimentos
                    + " movimentos não é válido para a coluna " + coluna + " (use 0 a " + cavalos + ").");
        this.jogador = jogador;
        this.coluna = coluna;
        this.movimentos = movimentos == -1 ? cavalos : movimentos;
        this.punhos = punhosDaColuna;
        this.khan = h.khan(coluna);
        visitadas.add(jogador.getPosicao());
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

    int getPunhos() {
        return punhos;
    }

    boolean isKhanPendente() {
        return khan;
    }

    /** Paradas onde o pião esteve neste turno, a começar pela de onde saiu. */
    List<Parada> getVisitadas() {
        return Collections.unmodifiableList(visitadas);
    }

    void gastar(int n) {
        if (n < 0 || n > movimentos)
            throw new IllegalArgumentException("Erro em Turno.gastar: não é possível gastar " + n
                    + " movimentos (restam " + movimentos + ").");
        movimentos -= n;
    }

    /** Gasta 1 punho para pegar 1 peça de tributo (§7.2). */
    void usarPunho() {
        if (punhos == 0)
            throw new IllegalStateException("Erro em Turno.usarPunho: não há mais punhos neste turno.");
        punhos--;
    }

    /** Tesouros já tomados da cidade neste turno (o 1º custa 1 espada; os seguintes, 2). */
    int getAtaques(Cidade c) {
        return ataques.getOrDefault(c, 0);
    }

    void registrarAtaque(Cidade c) {
        ataques.merge(c, 1, Integer::sum);
    }

    /** O Khan obrigatório da coluna já foi movido (§7.3). */
    void moverKhan() {
        if (!khan)
            throw new IllegalStateException("Erro em Turno.moverKhan: a coluna não manda mover o Khan, ou ele já foi movido.");
        khan = false;
    }

    /** Gasta 1 movimento para ir à parada vizinha e a anota como visitada. */
    void andar(Parada p) {
        gastar(1);
        if (!visitadas.contains(p)) visitadas.add(p);
    }

    // ---------------------------------------------------------------- recursos virtuais (regras, p. 16)

    /** Peças virtuais do tipo que ainda valem neste turno. */
    int getVirtuais(Tributo t) {
        return virtuais.getOrDefault(t, 0);
    }

    /** Peças do tipo que o jogador pode usar agora: as virtuais do turno mais as peças reais da reserva. */
    int disponivel(Tributo t) {
        return getVirtuais(t) + jogador.getTributos(t);
    }

    /**
     * Paga n peças do tipo. As virtuais são gastas primeiro, porque somem no fim do turno; o resto sai da reserva
     * do jogador e volta ao estoque.
     */
    void pagar(Tributo t, int n) {
        if (n < 0 || n > disponivel(t))
            throw new IllegalArgumentException("Erro em Turno.pagar: " + jogador.getNome() + " não tem " + n
                    + " peça(s) de " + t.getNome() + " (tem " + disponivel(t) + ").");
        int virtual = Math.min(n, getVirtuais(t));
        virtuais.put(t, getVirtuais(t) - virtual);
        jogador.gastar(t, n - virtual);
    }

    /** Tesouros virtuais do tipo que ainda valem neste turno. */
    int getTesourosVirtuais(Tesouro t) {
        return tesourosVirtuais.getOrDefault(t, 0);
    }

    void usarTesouroVirtual(Tesouro t) {
        if (getTesourosVirtuais(t) == 0)
            throw new IllegalStateException("Erro em Turno.usarTesouroVirtual: não há " + t.getNome()
                    + " virtual neste turno.");
        tesourosVirtuais.merge(t, -1, Integer::sum);
    }

    // ---------------------------------------------------------------- peças de bônus (regras, p. 17)

    void ganharMovimentos(int n) {
        movimentos += n;
    }

    void ganharPunhos(int n) {
        punhos += n;
    }

    /** Tesouros do baú aberto, à espera de o jogador escolher 1 (vazia se não houver baú aberto). */
    List<Tesouro> getBau() {
        return Collections.unmodifiableList(bau);
    }

    void abrirBau(List<Tesouro> tesouros) {
        bau.clear();
        bau.addAll(tesouros);
    }

    void fecharBau() {
        bau.clear();
    }

    // ---------------------------------------------------------------- partida salva

    // Ao carregar uma partida salva: o turno volta exatamente como estava (os movimentos podem passar dos cavalos
    // da coluna, por causa das peças de bônus).
    void restaurar(int movimentos, int punhos, boolean khan, List<Parada> visitadas, Map<Cidade, Integer> ataques,
            Map<Tributo, Integer> virtuais, Map<Tesouro, Integer> tesourosVirtuais, int melhoriasGratis, List<Tesouro> bau,
            boolean habilidadeUsada) {
        this.movimentos = movimentos;
        this.punhos = punhos;
        this.khan = khan;
        if (!visitadas.isEmpty()) {
            this.visitadas.clear();
            this.visitadas.addAll(visitadas);
        }
        this.ataques.clear();
        this.ataques.putAll(ataques);
        this.virtuais.clear();
        this.virtuais.putAll(virtuais);
        this.tesourosVirtuais.clear();
        this.tesourosVirtuais.putAll(tesourosVirtuais);
        this.melhoriasGratis = melhoriasGratis;
        this.bau.clear();
        this.bau.addAll(bau);
        this.habilidadeUsada = habilidadeUsada;
    }

    // ---------------------------------------------------------------- habilidade do herdeiro (uma vez por turno)

    boolean isHabilidadeUsada() {
        return habilidadeUsada;
    }

    void usarHabilidade() {
        habilidadeUsada = true;
    }

    // ---------------------------------------------------------------- melhoria grátis da linha completa

    int getMelhoriasGratis() {
        return melhoriasGratis;
    }

    void ganharMelhoriaGratis() {
        melhoriasGratis++;
    }

    void usarMelhoriaGratis() {
        if (melhoriasGratis == 0)
            throw new IllegalStateException("Erro em Turno.usarMelhoriaGratis: não há melhoria grátis para escolher.");
        melhoriasGratis--;
    }
}
