package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Uma cidade do mapa. Só as cidades reveladas (3 por vez) têm tesouros e podem ser atacadas (§5; regras, p. 10).
// Quem toma o último tesouro conquista a cidade e põe um yurt seu no meio dela.
class Cidade {
    static final int TESOUROS = 4; // tesouros colocados na cidade quando ela é revelada

    private final String nome;
    private final Regiao regiao;
    private final int x;
    private final int y;
    private final List<Parada> adjacentes;
    private final List<Tesouro> tesouros = new ArrayList<>();
    private boolean revelada;
    private Jogador dono; // quem conquistou a cidade, ou null

    Cidade(String nome, Regiao regiao, int x, int y, List<Parada> adjacentes) {
        this.nome = nome;
        this.regiao = regiao;
        this.x = x;
        this.y = y;
        this.adjacentes = List.copyOf(adjacentes);
    }

    String nome() {
        return nome;
    }

    Regiao regiao() {
        return regiao;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    List<Parada> getAdjacentes() {
        return adjacentes;
    }

    boolean isAdjacente(Parada p) {
        return adjacentes.contains(p);
    }

    List<Tesouro> getTesouros() {
        return Collections.unmodifiableList(tesouros);
    }

    boolean isRevelada() {
        return revelada;
    }

    Jogador getDono() {
        return dono;
    }

    /** Volta ao começo da partida: escondida, sem tesouros e sem dono. */
    void reiniciar() {
        tesouros.clear();
        revelada = false;
        dono = null;
    }

    /** Regras, p. 10: a cidade é revelada com os tesouros tirados do saco. */
    void revelar(List<Tesouro> novos) {
        revelada = true;
        tesouros.addAll(novos);
    }

    void retirar(Tesouro t) {
        if (!tesouros.remove(t))
            throw new IllegalArgumentException("Erro em Cidade.retirar: " + nome + " não tem o tesouro "
                    + t.getNome() + ".");
    }

    void conquistar(Jogador j) {
        dono = j;
    }
}
