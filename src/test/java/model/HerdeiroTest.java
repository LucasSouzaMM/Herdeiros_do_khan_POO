package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class HerdeiroTest {

    // Horses of the red, green, yellow and white columns, read from each player board image.
    @Test
    public void cavalosDeCadaColuna() {
        assertArrayEquals(new int[] {1, 2, 1, 2}, cavalos(Herdeiro.ALTANI));
        assertArrayEquals(new int[] {1, 1, 2, 2}, cavalos(Herdeiro.JOCHI));
        assertArrayEquals(new int[] {1, 1, 2, 2}, cavalos(Herdeiro.CHAGATAI));
        assertArrayEquals(new int[] {1, 2, 1, 2}, cavalos(Herdeiro.TOLUI));
        assertArrayEquals(new int[] {2, 1, 1, 2}, cavalos(Herdeiro.OGEDEI));
    }

    @Test
    public void buscaPeloNome() {
        assertEquals(Herdeiro.CHAGATAI, Herdeiro.porNome("Chagatai"));
        assertEquals("Chagatai", Herdeiro.CHAGATAI.getNome());
    }

    @Test
    public void rejeitaNomeInexistente() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Herdeiro.porNome("Fulano"));
        assertTrue(e.getMessage().contains("Herdeiro.porNome"));
        assertTrue(e.getMessage().contains("Fulano"));
    }

    private static int[] cavalos(Herdeiro h) {
        int[] c = new int[4];
        for (int i = 0; i < 4; i++) c[i] = h.cavalos(i);
        return c;
    }
}
