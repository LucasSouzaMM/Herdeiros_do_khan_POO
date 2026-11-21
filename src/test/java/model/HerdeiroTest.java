package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class HerdeiroTest {

    // Cavalos das colunas vermelha, verde, amarela e branca, lidos da cartela de cada herdeiro.
    @Test
    public void cavalosDeCadaColuna() {
        assertArrayEquals(new int[] {1, 2, 1, 2}, cavalos(Herdeiro.ALTANI));
        assertArrayEquals(new int[] {1, 1, 2, 2}, cavalos(Herdeiro.JOCHI));
        assertArrayEquals(new int[] {1, 1, 2, 2}, cavalos(Herdeiro.CHAGATAI));
        assertArrayEquals(new int[] {1, 2, 1, 2}, cavalos(Herdeiro.TOLUI));
        assertArrayEquals(new int[] {2, 1, 1, 2}, cavalos(Herdeiro.OGEDEI));
    }

    // Punhos das colunas e a coluna com o Khan, lidos das cartelas.
    @Test
    public void punhosKhanEMoedaDeCadaColuna() {
        assertArrayEquals(new int[] {2, 1, 1, 0}, punhos(Herdeiro.ALTANI));
        assertArrayEquals(new int[] {1, 2, 1, 0}, punhos(Herdeiro.JOCHI));
        assertArrayEquals(new int[] {2, 1, 1, 0}, punhos(Herdeiro.CHAGATAI));
        assertArrayEquals(new int[] {1, 1, 2, 0}, punhos(Herdeiro.TOLUI));
        assertArrayEquals(new int[] {1, 1, 2, 0}, punhos(Herdeiro.OGEDEI));
        assertTrue(Herdeiro.ALTANI.khan(0) && Herdeiro.JOCHI.khan(1) && Herdeiro.TOLUI.khan(2));
        assertFalse(Herdeiro.OGEDEI.khan(0));
        for (Herdeiro h : Herdeiro.values()) {
            assertTrue(h.khan(Herdeiro.BRANCA));
            int khan = 0;
            for (int c = 0; c < 3; c++) {
                if (h.khan(c)) khan++;
                // A coluna do Khan é a de 2 punhos.
                assertEquals(h.khan(c), h.punhos(c) == 2);
            }
            assertEquals(1, khan);
        }
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

    private static int[] punhos(Herdeiro h) {
        int[] p = new int[4];
        for (int i = 0; i < 4; i++) p[i] = h.punhos(i);
        return p;
    }

    private static int[] cavalos(Herdeiro h) {
        int[] c = new int[4];
        for (int i = 0; i < 4; i++) c[i] = h.cavalos(i);
        return c;
    }
}
