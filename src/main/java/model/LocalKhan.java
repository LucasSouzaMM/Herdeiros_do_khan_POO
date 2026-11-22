package model;

import java.util.List;

// Uma das 4 províncias do Khan, marcada no tabuleiro com a cabeça do Khan (§7.3). Quando o Khan vai para lá,
// as 3 províncias indicadas pelas setas que partem dele recebem 1 peça de tributo cada.
class LocalKhan {
    static final int RAIO = 90; // área de clique em volta da cabeça do Khan, em pixels da imagem do tabuleiro

    private final int x, y; // centro da cabeça do Khan na imagem do tabuleiro
    private final List<Provincia> provincias;

    LocalKhan(int x, int y, List<Provincia> provincias) {
        this.x = x;
        this.y = y;
        this.provincias = List.copyOf(provincias);
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    List<Provincia> getProvincias() {
        return provincias;
    }

    /**
     * A província do Khan propriamente dita: das 3 que recebem tributo, a que tem o ícone mais perto da cabeça do
     * Khan (a cabeça fica dentro dela; as setas apontam para as outras 2). Usada pela carta secreta do Bajulador.
     */
    Provincia getPropria() {
        Provincia perto = null;
        long menor = Long.MAX_VALUE;
        for (Provincia p : provincias) {
            long dx = p.getX() - x, dy = p.getY() - y, d = dx * dx + dy * dy;
            if (d < menor) {
                menor = d;
                perto = p;
            }
        }
        return perto;
    }

    boolean contem(int px, int py) {
        long dx = px - x, dy = py - y;
        return dx * dx + dy * dy <= (long) RAIO * RAIO;
    }
}
