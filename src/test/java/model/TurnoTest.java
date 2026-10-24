package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class TurnoTest {

    @Test
    public void guardaJogadorEColuna() {
        Jogador tolui = new Jogador(Herdeiro.TOLUI, 1);
        Turno t = new Turno(tolui, 3);
        assertSame(tolui, t.getJogador());
        assertEquals(3, t.getColuna());
    }

    @Test
    public void comecaComOsCavalosDaColuna() {
        Jogador ogedei = new Jogador(Herdeiro.OGEDEI, 1);
        assertEquals(2, new Turno(ogedei, 0).getMovimentos());
        assertEquals(1, new Turno(ogedei, 1).getMovimentos());
    }

    @Test
    public void gastaMovimentos() {
        Turno t = new Turno(new Jogador(Herdeiro.TOLUI, 1), 1); // 2 horses
        t.gastar(1);
        assertEquals(1, t.getMovimentos());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> t.gastar(2));
        assertTrue(e.getMessage().contains("Turno.gastar"));
        assertEquals(1, t.getMovimentos());
    }

    @Test
    public void rejeitaColunaForaDe0a3() {
        Jogador tolui = new Jogador(Herdeiro.TOLUI, 1);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Turno(tolui, 4));
        assertTrue(e.getMessage().contains("Turno.Turno"));
        assertThrows(IllegalArgumentException.class, () -> new Turno(tolui, -1));
    }
}
