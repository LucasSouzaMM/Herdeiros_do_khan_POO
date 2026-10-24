package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class MapaTest {

    private final Mapa mapa = new Mapa();

    // J16
    @Test
    public void temAsParadasDoTabuleiro() {
        assertEquals(34, mapa.getParadas().size());
        int duplas = 0;
        for (Parada p : mapa.getParadas()) if (p.isDupla()) duplas++;
        assertEquals(6, duplas);
        assertEquals(9, mapa.getCidades().size());
    }

    // J16
    @Test
    public void rotasLigamOsDoisLados() {
        for (Parada p : mapa.getParadas())
            for (Parada v : p.getVizinhas()) assertTrue(p.getId() + "-" + v.getId(), v.getVizinhas().contains(p));
    }

    // J16
    @Test
    public void todaParadaEAlcancavelDeKarakorum() {
        Parada k = mapa.getParada(Mapa.KARAKORUM);
        for (Parada p : mapa.getParadas()) assertTrue(p.getId(), mapa.distancia(k, p) >= 0);
    }

    @Test
    public void distanciaEOMenorNumeroDePassos() {
        Parada k = mapa.getParada(Mapa.KARAKORUM);
        assertEquals(0, mapa.distancia(k, k));
        assertEquals(1, mapa.distancia(k, mapa.getParada("r9")));
        assertEquals(2, mapa.distancia(k, mapa.getParada("r8")));
    }

    // §7.1: atravessar uma parada dupla custa só 1 movimento.
    @Test
    public void paradaDuplaCustaUmMovimento() {
        assertEquals(2, mapa.distancia(mapa.getParada(Mapa.KARAKORUM), mapa.getParada("c1"))); // via r11
    }

    // J17
    @Test
    public void paradaEmPontoDoTabuleiro() {
        assertEquals("r1", mapa.paradaEm(490, 255).getId());
        assertEquals("r1", mapa.paradaEm(490 + Mapa.RAIO, 255).getId());
        assertNull(mapa.paradaEm(490 + Mapa.RAIO + 1, 255));
        assertEquals("r4", mapa.paradaEm(855, 170).getId());
        assertEquals("r4", mapa.paradaEm(930, 250).getId());
        assertEquals(Mapa.KARAKORUM, mapa.paradaEm(1450, 1300).getId());
        assertNull(mapa.paradaEm(10, 10));
    }

    @Test
    public void rejeitaParadaInexistente() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> mapa.getParada("x1"));
        assertTrue(e.getMessage().contains("Mapa.getParada"));
    }
}
