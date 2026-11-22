package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class HerdeiroTest {

    // Cavalos das colunas vermelha, verde, amarela e branca, lidos da cartela de cada herdeiro.
    @Test(timeout = 1000)
    public void cavalosDeCadaColuna() {
        assertArrayEquals("cavalos da Altani", new int[] {1, 2, 1, 2}, cavalos(Herdeiro.ALTANI));
        assertArrayEquals("cavalos do Jochi", new int[] {1, 1, 2, 2}, cavalos(Herdeiro.JOCHI));
        assertArrayEquals("cavalos da Chagatai", new int[] {1, 1, 2, 2}, cavalos(Herdeiro.CHAGATAI));
        assertArrayEquals("cavalos do Tolui", new int[] {1, 2, 1, 2}, cavalos(Herdeiro.TOLUI));
        assertArrayEquals("cavalos do Ogedei", new int[] {2, 1, 1, 2}, cavalos(Herdeiro.OGEDEI));
    }

    // Punhos das colunas e a coluna com o Khan, lidos das cartelas.
    @Test(timeout = 1000)
    public void punhosKhanEMoedaDeCadaColuna() {
        assertArrayEquals("punhos da Altani", new int[] {2, 1, 1, 0}, punhos(Herdeiro.ALTANI));
        assertArrayEquals("punhos do Jochi", new int[] {1, 2, 1, 0}, punhos(Herdeiro.JOCHI));
        assertArrayEquals("punhos da Chagatai", new int[] {2, 1, 1, 0}, punhos(Herdeiro.CHAGATAI));
        assertArrayEquals("punhos do Tolui", new int[] {1, 1, 2, 0}, punhos(Herdeiro.TOLUI));
        assertArrayEquals("punhos do Ogedei", new int[] {1, 1, 2, 0}, punhos(Herdeiro.OGEDEI));
        assertTrue("o Khan na coluna vermelha da Altani, verde do Jochi e amarela do Tolui",
                Herdeiro.ALTANI.khan(0) && Herdeiro.JOCHI.khan(1) && Herdeiro.TOLUI.khan(2));
        // Moeda virtual impressa na cartela: uma coluna colorida de cada herdeiro.
        assertTrue("a moeda virtual impressa de cada herdeiro",
                Herdeiro.ALTANI.moeda(2) && Herdeiro.JOCHI.moeda(0) && Herdeiro.CHAGATAI.moeda(1)
                && Herdeiro.TOLUI.moeda(0) && Herdeiro.OGEDEI.moeda(1));
        for (Herdeiro h : Herdeiro.values()) {
            int moedas = 0;
            for (int c = 0; c < 4; c++)
                if (h.moeda(c)) moedas++;
            assertEquals(h.getNome() + ": 1 coluna com moeda", 1, moedas);
            assertFalse(h.getNome() + ": a coluna branca não tem moeda", h.moeda(Herdeiro.BRANCA));
        }
        assertFalse("a coluna vermelha do Ogedei não tem Khan", Herdeiro.OGEDEI.khan(0));
        for (Herdeiro h : Herdeiro.values()) {
            assertTrue(h.getNome() + ": a coluna branca tem Khan", h.khan(Herdeiro.BRANCA));
            int khan = 0;
            for (int c = 0; c < 3; c++) {
                if (h.khan(c)) khan++;
                // A coluna do Khan é a de 2 punhos.
                assertEquals(h.getNome() + ", coluna " + c + ": o Khan fica na coluna de 2 punhos",
                        h.khan(c), h.punhos(c) == 2);
            }
            assertEquals(h.getNome() + ": 1 coluna colorida com Khan", 1, khan);
        }
    }

    @Test(timeout = 1000)
    public void buscaPeloNome() {
        assertEquals("porNome acha a Chagatai", Herdeiro.CHAGATAI, Herdeiro.porNome("Chagatai"));
        assertEquals("o nome da Chagatai", "Chagatai", Herdeiro.CHAGATAI.getNome());
    }

    @Test(timeout = 1000)
    public void rejeitaNomeInexistente() {
        IllegalArgumentException e = assertThrows("nome que não existe é recusado",
                IllegalArgumentException.class, () -> Herdeiro.porNome("Fulano"));
        assertTrue(e.getMessage(), e.getMessage().contains("Herdeiro.porNome"));
        assertTrue(e.getMessage(), e.getMessage().contains("Fulano"));
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
