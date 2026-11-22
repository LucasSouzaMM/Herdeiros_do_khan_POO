package model;

import java.util.ArrayList;
import java.util.List;

// Yurts dos jogadores no tabuleiro (regras, p. 11): construção, os 12 de cada jogador e a contagem por região.
class Yurts {
    private final Mapa mapa;

    Yurts(Mapa mapa) {
        this.mapa = mapa;
    }

    /** O tabuleiro começa sem yurts. */
    void preparar() {
        for (Parada p : mapa.getParadas()) p.getYurts().clear();
    }

    /**
     * Gasta 1 peça de tributo de yurt para construir um yurt do jogador numa parada onde ele esteve neste turno,
     * mesmo que haja outro pião nela. Cabe 1 yurt por parada, 2 numa parada dupla, e nenhum em Karakorum.
     */
    void construir(Jogador j, Turno turno, Parada p) {
        if (turno.disponivel(Tributo.YURT) == 0)
            throw new IllegalStateException("Erro em Yurts.construir: " + j.getNome()
                    + " não tem peças de tributo de yurt.");
        if (restantes(j) == 0)
            throw new IllegalStateException("Erro em Yurts.construir: " + j.getNome() + " já usou os seus "
                    + Jogador.YURTS + " yurts.");
        if (!podeConstruirEm(j, turno, p))
            throw new IllegalArgumentException("Erro em Yurts.construir: " + j.getNome() + " não esteve na parada "
                    + p.getId() + " neste turno" + (j.getHerdeiro() == Herdeiro.TOLUI ? ", e ela não é vizinha do seu pião." : "."));
        if (p.getYurts().size() >= p.getCapacidadeYurts())
            throw new IllegalArgumentException("Erro em Yurts.construir: não cabe mais um yurt em " + p.getId()
                    + (p.getCapacidadeYurts() == 0 ? " (Karakorum não pode ter yurts)." : "."));
        turno.pagar(Tributo.YURT, 1); // o yurt virtual primeiro
        p.getYurts().add(j);
    }

    // Yurts que o jogador ainda pode colocar no tabuleiro (dos 12). O yurt de uma cidade conquistada também conta.
    // Desde o C2, sem yurt não há conquista; o mínimo 0 protege arquivos salvos antes dessa regra.
    int restantes(Jogador j) {
        return Math.max(0, Jogador.YURTS - noMapa(j, null));
    }

    /** Paradas onde o jogador do turno pode construir um yurt agora (vazia se ele não tiver peças de yurt). */
    List<Parada> paradasParaYurt(Turno turno) {
        List<Parada> paradas = new ArrayList<>();
        if (turno == null) return paradas;
        Jogador j = turno.getJogador();
        if (turno.disponivel(Tributo.YURT) == 0 || restantes(j) == 0) return paradas;
        for (Parada p : mapa.getParadas())
            if (podeConstruirEm(j, turno, p) && p.getYurts().size() < p.getCapacidadeYurts()) paradas.add(p);
        return paradas;
    }

    // Paradas onde o jogador esteve neste turno; o Tolui também pode construir nas vizinhas ao seu pião (cartela).
    private static boolean podeConstruirEm(Jogador j, Turno turno, Parada p) {
        if (turno.getVisitadas().contains(p)) return true;
        return j.getHerdeiro() == Herdeiro.TOLUI && j.getPosicao().getVizinhas().contains(p);
    }

    // Yurts do jogador no mapa (nas paradas e no meio das cidades conquistadas), numa região ou em todas (null).
    int noMapa(Jogador j, Regiao regiao) {
        int n = 0;
        for (Parada p : mapa.getParadas())
            if (regiao == null || p.getRegiao() == regiao)
                for (Jogador dono : p.getYurts())
                    if (dono == j) n++;
        for (Cidade c : mapa.getCidades())
            if (c.getDono() == j && (regiao == null || c.regiao() == regiao)) n++;
        return n;
    }
}
