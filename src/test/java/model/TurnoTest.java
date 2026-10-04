package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class TurnoTest {

    @Test
    public void guardaJogadorEColuna() {
        Jogador ana = new Jogador("Ana", 1);
        Turno t = new Turno(ana, 3);
        assertSame(ana, t.getJogador());
        assertEquals(3, t.getColuna());
    }

    @Test
    public void rejeitaColunaForaDe0a3() {
        Jogador ana = new Jogador("Ana", 1);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Turno(ana, 4));
        assertTrue(e.getMessage().contains("Turno.Turno"));
        assertThrows(IllegalArgumentException.class, () -> new Turno(ana, -1));
    }
}
