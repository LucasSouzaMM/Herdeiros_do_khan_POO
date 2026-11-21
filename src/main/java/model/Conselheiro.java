package model;

import java.util.List;

// Um conselheiro do Kurultai (regras, p. 12 e 13): 6 paradas duplas e Karakorum. Quem esteve na parada no turno
// pode entregar o conjunto de tesouros que ele pede, quantas vezes quiser, e ganha votos a cada entrega.
class Conselheiro {
    // Tipos de pedido.
    static final int PAR = 0;       // 2 tesouros iguais do tipo indicado (1 voto)
    static final int QUAISQUER = 1; // 3 tesouros de qualquer tipo (1 voto)
    static final int TIPOS = 2;     // 1 tesouro de cada um dos 3 tipos indicados (Karakorum, 2 votos)

    private final Parada parada;
    private final int pedido;
    private final List<Tesouro> tipos; // vazio quando o pedido é QUAISQUER
    private final int votos;

    Conselheiro(Parada parada, int pedido, List<Tesouro> tipos, int votos) {
        this.parada = parada;
        this.pedido = pedido;
        this.tipos = List.copyOf(tipos);
        this.votos = votos;
    }

    Parada getParada() {
        return parada;
    }

    int getPedido() {
        return pedido;
    }

    List<Tesouro> getTipos() {
        return tipos;
    }

    int getVotos() {
        return votos;
    }
}
