package model;

import static model.ObservadoIF.EVENTO;
import static model.ObservadoIF.NOMES;

import java.util.ArrayList;
import java.util.List;

import model.ObservadoIF.Evento;

// Ajuda dos testes: deixa a partida pronta para a primeira rodada e recolhe os eventos de uma ação.
final class Partidas {
    private Partidas() {
    }

    // Regras, p. 6: antes da primeira rodada, cada jogador, na ordem da partida, fica com 1 das 2 cartas
    // secretas. Aqui, a primeira.
    static void escolherSecretas(Jogo j) {
        for (int i = 0; i < j.get(NOMES).size(); i++)
            j.escolherCartaSecreta(i, j.jogador(i).getOpcoesSecretas().get(0).getId());
    }

    // Executa a ação e devolve os eventos avisados durante ela, na ordem.
    static List<Evento> eventos(Jogo j, Runnable acao) {
        List<Evento> eventos = new ArrayList<>();
        ObservadorIF observador = observado -> eventos.add(observado.get(EVENTO));
        j.add(observador);
        try {
            acao.run();
        } finally {
            j.remove(observador);
        }
        return eventos;
    }
}
