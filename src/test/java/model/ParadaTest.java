package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class ParadaTest {

    @Test
    public void paradaSimples() {
        Parada p = new Parada("r1", Regiao.RUSSIA, 10, 100, 200);
        assertFalse(p.isDupla());
        assertEquals(100, p.getX());
        assertEquals(200, p.getY());
        assertTrue(p.contem(110, 200));
        assertFalse(p.contem(111, 200));
    }

    @Test
    public void paradaDuplaTemDoisMedalhoes() {
        Parada p = new Parada("r4", Regiao.RUSSIA, 10, 100, 200, 300, 200);
        assertTrue(p.isDupla());
        assertEquals(200, p.getX());
        assertTrue(p.contem(100, 200));
        assertTrue(p.contem(300, 205));
        assertFalse(p.contem(200, 200));
    }
}
