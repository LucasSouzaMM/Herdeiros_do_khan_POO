package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class TurnoTest {

    @Test(timeout = 1000)
    public void guardaJogadorEColuna() {
        Jogador tolui = new Jogador(Herdeiro.TOLUI, 1);
        Turno t = new Turno(tolui, 3);
        assertSame("o jogador do turno", tolui, t.getJogador());
        assertEquals("a coluna ativada", 3, t.getColuna());
    }

    @Test(timeout = 1000)
    public void comecaComOsCavalosDaColuna() {
        Jogador ogedei = new Jogador(Herdeiro.OGEDEI, 1);
        assertEquals("coluna 1 do Ogedei: 2 cavalos", 2, new Turno(ogedei, 0).getMovimentos());
        assertEquals("coluna 2 do Ogedei: 1 cavalo", 1, new Turno(ogedei, 1).getMovimentos());
    }

    @Test(timeout = 1000)
    public void gastaMovimentos() {
        Turno t = new Turno(new Jogador(Herdeiro.TOLUI, 1), 1); // 2 cavalos
        t.gastar(1);
        assertEquals("gastou 1 dos 2 movimentos", 1, t.getMovimentos());
        IllegalArgumentException e = assertThrows("não dá para gastar mais do que sobra",
                IllegalArgumentException.class, () -> t.gastar(2));
        assertTrue(e.getMessage(), e.getMessage().contains("Turno.gastar"));
        assertEquals("a recusa não gasta nada", 1, t.getMovimentos());
    }

    @Test(timeout = 1000)
    public void guardaPunhosKhanEParadasVisitadas() {
        Mapa mapa = new Mapa();
        Jogador tolui = new Jogador(Herdeiro.TOLUI, 1);
        tolui.setPosicao(mapa.getParada(Mapa.KARAKORUM));
        Turno t = new Turno(tolui, 2); // 1 cavalo, 2 punhos e Khan
        assertEquals("a coluna 3 do Tolui dá 2 punhos", 2, t.getPunhos());
        assertTrue("a coluna 3 do Tolui manda mover o Khan", t.isKhanPendente());
        assertEquals("a parada inicial já conta como visitada", 1, t.getVisitadas().size());
        t.andar(mapa.getParada("r9"));
        assertEquals("o único movimento foi gasto", 0, t.getMovimentos());
        assertEquals("Karakorum e r9", 2, t.getVisitadas().size());
        assertThrows("sem movimento não anda", IllegalArgumentException.class, () -> t.andar(mapa.getParada("r8")));
        assertFalse("a coluna 1 não manda mover o Khan", new Turno(tolui, 0).isKhanPendente());
    }

    @Test(timeout = 1000)
    public void rejeitaColunaForaDe0a3() {
        Jogador tolui = new Jogador(Herdeiro.TOLUI, 1);
        IllegalArgumentException e = assertThrows("a coluna 4 não existe",
                IllegalArgumentException.class, () -> new Turno(tolui, 4));
        assertTrue(e.getMessage(), e.getMessage().contains("Turno.Turno"));
        assertThrows("a coluna -1 não existe", IllegalArgumentException.class, () -> new Turno(tolui, -1));
    }
}
