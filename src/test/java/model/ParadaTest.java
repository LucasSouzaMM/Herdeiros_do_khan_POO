package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class ParadaTest {

    @Test(timeout = 1000)
    public void paradaSimples() {
        Parada p = new Parada("r1", Regiao.RUSSIA, 10, 100, 200);
        assertFalse("r1 tem 1 medalhão só", p.isDupla());
        assertEquals("x do medalhão", 100, p.getX());
        assertEquals("y do medalhão", 200, p.getY());
        assertTrue("ponto na borda do raio está dentro", p.contem(110, 200));
        assertFalse("ponto 1 pixel fora do raio está fora", p.contem(111, 200));
    }

    @Test(timeout = 1000)
    public void paradaDuplaTemDoisMedalhoes() {
        Parada p = new Parada("r4", Regiao.RUSSIA, 10, 100, 200, 300, 200);
        assertTrue("r4 tem 2 medalhões", p.isDupla());
        assertEquals("o centro fica entre os 2 medalhões", 200, p.getX());
        assertTrue("ponto no 1º medalhão", p.contem(100, 200));
        assertTrue("ponto no 2º medalhão", p.contem(300, 205));
        assertFalse("ponto entre os medalhões está fora", p.contem(200, 200));
    }

    @Test(timeout = 1000)
    public void capacidadeDeYurts() {
        Mapa mapa = new Mapa();
        assertEquals("Karakorum não tem yurts", 0, mapa.getParada(Mapa.KARAKORUM).getCapacidadeYurts());
        assertEquals("parada simples tem 1 yurt", 1, mapa.getParada("r9").getCapacidadeYurts());
        assertEquals("parada dupla tem 2 yurts", 2, mapa.getParada("r11").getCapacidadeYurts());
    }
}
