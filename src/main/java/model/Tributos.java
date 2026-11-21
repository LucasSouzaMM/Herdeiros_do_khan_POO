package model;

import java.util.ArrayList;
import java.util.List;

// Peças de tributo (§7.2, §7.3; regras, p. 3, 8 e 9): as províncias, o estoque limitado e o Khan, que leva
// tributos às províncias.
class Tributos {
    private final Mapa mapa;
    private int khan = -1; // província do Khan onde ele está (0 a 3), ou -1 se ainda está fora do tabuleiro (§5)

    Tributos(Mapa mapa) {
        this.mapa = mapa;
    }

    /** §5: 1 peça em cada província, e o Khan fora do tabuleiro. */
    void preparar() {
        mapa.prepararTributos();
        khan = -1;
    }

    int getKhan() {
        return khan;
    }

    /** Peças de tributo do tipo que estão no estoque, ao lado do tabuleiro (fora das províncias e das reservas). */
    int estoque(Tributo t, List<Jogador> jogadores) {
        int usadas = 0;
        for (Provincia p : mapa.getProvincias())
            if (p.getTipo() == t) usadas += p.getPecas();
        for (Jogador j : jogadores) usadas += j.getTributos(t);
        return t.getTotal() - usadas;
    }

    /**
     * Gasta 1 punho para pegar 1 peça de tributo da província. A província precisa ter peças e ser adjacente
     * a uma parada onde o pião esteve neste turno, antes, durante ou depois do movimento.
     */
    void pegar(Jogador j, Turno turno, Provincia p) {
        if (turno.getPunhos() == 0)
            throw new IllegalStateException("Erro em Tributos.pegar: " + j.getNome()
                    + " não tem mais punhos neste turno.");
        if (p.getPecas() == 0)
            throw new IllegalStateException("Erro em Tributos.pegar: a província " + p.getId() + " está vazia.");
        if (!adjacenteAoCaminho(p, turno))
            throw new IllegalArgumentException("Erro em Tributos.pegar: a província " + p.getId()
                    + " não é adjacente a nenhuma parada onde " + j.getNome() + " esteve neste turno.");
        turno.usarPunho();
        p.retirar();
        j.ganhar(p.getTipo());
    }

    private static boolean adjacenteAoCaminho(Provincia p, Turno turno) {
        for (Parada v : turno.getVisitadas())
            if (p.isAdjacente(v)) return true;
        return false;
    }

    /** Províncias de onde o jogador do turno pode pegar tributo agora (vazia se ele não tiver punhos). */
    List<Provincia> alcancaveis(Turno turno) {
        List<Provincia> provincias = new ArrayList<>();
        if (turno == null || turno.getPunhos() == 0) return provincias;
        for (Provincia p : mapa.getProvincias())
            if (p.getPecas() > 0 && adjacenteAoCaminho(p, turno)) provincias.add(p);
        return provincias;
    }

    /**
     * Move o Khan para a província do Khan indicada (0 a 3), quando a coluna ativada manda. Ele não pode ficar
     * onde está. A província e as 2 apontadas pelas setas recebem 1 peça de tributo cada, até o máximo de 3
     * por província e enquanto houver peças no estoque.
     * A área de melhorias, o 5º lugar do Khan, vem com as melhorias na 4ª iteração.
     */
    void moverKhan(Jogador j, Turno turno, int local, List<Jogador> jogadores) {
        if (!turno.isKhanPendente())
            throw new IllegalStateException("Erro em Tributos.moverKhan: a coluna " + (turno.getColuna() + 1) + " de "
                    + j.getNome() + " não manda mover o Khan, ou ele já foi movido neste turno.");
        LocalKhan l = getLocal(local);
        if (local == khan)
            throw new IllegalArgumentException("Erro em Tributos.moverKhan: o Khan já está nesta província."
                    + " Ele precisa ir para outra.");
        turno.moverKhan();
        khan = local;
        for (Provincia p : l.getProvincias())
            if (estoque(p.getTipo(), jogadores) > 0) p.receber(); // sem peças no estoque, a província fica como está
    }

    LocalKhan getLocal(int local) {
        if (local < 0 || local >= mapa.getLocaisKhan().size())
            throw new IllegalArgumentException("Erro em Tributos.getLocal: a província do Khan " + local
                    + " não existe (use 0 a " + (mapa.getLocaisKhan().size() - 1) + ").");
        return mapa.getLocaisKhan().get(local);
    }
}
