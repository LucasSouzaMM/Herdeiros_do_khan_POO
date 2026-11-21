package model;

import java.util.ArrayList;
import java.util.List;

// Uma parada do mapa. Uma parada dupla é um só nó com dois medalhões (§7.1).
class Parada {
    private final String id;
    private final Regiao regiao; // null em Karakorum
    private final int[] pontos; // centro de cada medalhão na imagem do tabuleiro: x1, y1[, x2, y2]
    private final int raio;
    private final List<Parada> vizinhas = new ArrayList<>();
    private final List<Jogador> yurts = new ArrayList<>(); // donos dos yurts construídos aqui (regras, p. 11)

    Parada(String id, Regiao regiao, int raio, int... pontos) {
        this.id = id;
        this.regiao = regiao;
        this.raio = raio;
        this.pontos = pontos;
    }

    String getId() {
        return id;
    }

    Regiao getRegiao() {
        return regiao;
    }

    boolean isDupla() {
        return pontos.length == 4;
    }

    /** Centro onde os piões são desenhados: o meio entre os medalhões de uma parada dupla. */
    int getX() {
        return isDupla() ? (pontos[0] + pontos[2]) / 2 : pontos[0];
    }

    int getY() {
        return isDupla() ? (pontos[1] + pontos[3]) / 2 : pontos[1];
    }

    int[] getPontos() {
        return pontos.clone();
    }

    int getRaio() {
        return raio;
    }

    List<Parada> getVizinhas() {
        return vizinhas;
    }

    List<Jogador> getYurts() {
        return yurts;
    }

    /** Regras, p. 11: 1 yurt por parada, 2 numa parada dupla e nenhum em Karakorum (a única sem região). */
    int getCapacidadeYurts() {
        if (regiao == null) return 0;
        return isDupla() ? 2 : 1;
    }

    boolean temYurtDe(Jogador j) {
        return yurts.contains(j);
    }

    boolean contem(int x, int y) {
        for (int i = 0; i < pontos.length; i += 2) {
            long dx = x - pontos[i], dy = y - pontos[i + 1];
            if (dx * dx + dy * dy <= (long) raio * raio) return true;
        }
        return false;
    }
}
