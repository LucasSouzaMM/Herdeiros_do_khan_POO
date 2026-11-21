package model;

import java.util.List;

// Uma província: a área entre as rotas que tem o ícone de um tipo de tributo impresso no tabuleiro (§7.2).
// Ela guarda até 3 peças de tributo desse tipo. As paradas adjacentes são as que ficam na sua borda.
class Provincia {
    static final int MAX_PECAS = 3;
    static final int RAIO = 110; // área de clique em volta do ícone, em pixels da imagem do tabuleiro

    private final String id;
    private final Tributo tipo;
    private final int x, y; // centro do ícone na imagem do tabuleiro
    private final List<Parada> adjacentes;
    private int pecas;

    Provincia(String id, Tributo tipo, int x, int y, List<Parada> adjacentes) {
        this.id = id;
        this.tipo = tipo;
        this.x = x;
        this.y = y;
        this.adjacentes = List.copyOf(adjacentes);
    }

    String getId() {
        return id;
    }

    Tributo getTipo() {
        return tipo;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    List<Parada> getAdjacentes() {
        return adjacentes;
    }

    int getPecas() {
        return pecas;
    }

    void setPecas(int n) {
        if (n < 0 || n > MAX_PECAS)
            throw new IllegalArgumentException("Erro em Provincia.setPecas: " + n
                    + " peças não é válido (use 0 a " + MAX_PECAS + ").");
        pecas = n;
    }

    /** Recebe 1 peça de tributo, se ainda não tiver 3. Retorna false se já estava cheia. */
    boolean receber() {
        if (pecas == MAX_PECAS) return false;
        pecas++;
        return true;
    }

    void retirar() {
        if (pecas == 0)
            throw new IllegalStateException("Erro em Provincia.retirar: a província " + id + " não tem peças de tributo.");
        pecas--;
    }

    boolean isAdjacente(Parada p) {
        return adjacentes.contains(p);
    }

    boolean contem(int px, int py) {
        long dx = px - x, dy = py - y;
        return dx * dx + dy * dy <= (long) RAIO * RAIO;
    }
}
