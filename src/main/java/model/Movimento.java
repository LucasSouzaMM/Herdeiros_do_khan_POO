package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Regras do movimento dos piões (§7.1; regras, p. 7): aonde o pião chega com 1 movimento e onde ele não pode parar.
class Movimento {
    private Movimento() {
    }

    // Paradas aonde o pião chega com 1 movimento: as vizinhas e, através de paradas com yurts do jogador,
    // as vizinhas delas (busca em largura que só continua por paradas com yurt dele).
    static Set<Parada> alcance(Jogador j) {
        Set<Parada> alcance = new LinkedHashSet<>();
        Deque<Parada> fila = new ArrayDeque<>(j.getPosicao().getVizinhas());
        alcance.addAll(fila);
        while (!fila.isEmpty()) {
            Parada p = fila.poll();
            if (!p.temYurtDe(j)) continue;
            for (Parada v : p.getVizinhas())
                if (alcance.add(v)) fila.add(v);
        }
        alcance.remove(j.getPosicao());
        return alcance;
    }

    // §7.1: não se termina numa parada com outro pião, exceto numa parada dupla (2 piões) e em Karakorum (todos).
    static boolean lotada(Parada p, Jogador quem, List<Jogador> jogadores) {
        if (p.getId().equals(Mapa.KARAKORUM)) return false;
        int outros = 0;
        for (Jogador j : jogadores)
            if (j != quem && j.getPosicao() == p) outros++;
        return outros >= (p.isDupla() ? 2 : 1);
    }

    /** Paradas para onde o jogador do turno pode andar agora (vazia se ele não tiver movimentos). */
    static List<Parada> destinos(Turno turno, List<Jogador> jogadores) {
        List<Parada> destinos = new ArrayList<>();
        if (turno == null || turno.getMovimentos() == 0) return destinos;
        Jogador j = turno.getJogador();
        for (Parada p : alcance(j))
            if (turno.getMovimentos() > 1 || !lotada(p, j, jogadores)) destinos.add(p);
        return destinos;
    }
}
