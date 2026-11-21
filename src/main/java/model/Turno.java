package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// O turno de um jogador depois de ativar uma coluna: o que ainda falta fazer e as paradas por onde o pião passou.
class Turno {
    private final Jogador jogador;
    private final int coluna;
    private int movimentos;
    private int punhos;
    private boolean khan; // o Khan ainda precisa ser movido
    private final List<Parada> visitadas = new ArrayList<>();
    private final java.util.Map<Cidade, Integer> ataques = new java.util.HashMap<>(); // tesouros tomados de cada cidade

    Turno(Jogador jogador, int coluna) {
        this(jogador, coluna, -1);
    }

    // movimentos = -1: todos os cavalos da coluna (§7.1). Outro valor vem de uma partida salva.
    // Os punhos e o Khan sempre começam com tudo o que a coluna dá (§7).
    Turno(Jogador jogador, int coluna, int movimentos) {
        if (coluna < 0 || coluna > 3)
            throw new IllegalArgumentException("Erro em Turno.Turno: a coluna " + coluna + " não existe (use 0 a 3).");
        Herdeiro h = jogador.getHerdeiro();
        int cavalos = h.cavalos(coluna);
        if (movimentos < -1 || movimentos > cavalos)
            throw new IllegalArgumentException("Erro em Turno.Turno: " + movimentos
                    + " movimentos não é válido para a coluna " + coluna + " (use 0 a " + cavalos + ").");
        this.jogador = jogador;
        this.coluna = coluna;
        this.movimentos = movimentos == -1 ? cavalos : movimentos;
        this.punhos = h.punhos(coluna);
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
}
