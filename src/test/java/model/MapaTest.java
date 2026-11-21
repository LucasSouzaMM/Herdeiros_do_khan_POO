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

    // Cada cidade fica no anel de 3 paradas (Sarai: r8, r9 e r10).
    @Test
    public void cidadeTemAnelDeParadas() {
        java.util.List<String> anel = new java.util.ArrayList<>();
        for (Parada p : mapa.getCidade("Sarai").getAdjacentes()) anel.add(p.getId());
        assertEquals(java.util.List.of("r8", "r9", "r10"), anel);
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

    // §7.2: 12 províncias, 4 de cada tipo de tributo, cada uma na borda de 4 a 6 paradas.
    @Test
    public void temAsProvinciasDoTabuleiro() {
        assertEquals(12, mapa.getProvincias().size());
        for (Tributo t : Tributo.values()) {
            int n = 0;
            for (Provincia p : mapa.getProvincias()) if (p.getTipo() == t) n++;
            assertEquals(t.getNome(), 4, n);
        }
        for (Provincia p : mapa.getProvincias()) {
            int n = p.getAdjacentes().size();
            assertTrue(p.getId(), n >= 4 && n <= 6);
            assertEquals(0, p.getPecas()); // antes de começar a partida
        }
        assertTrue(mapa.getProvincia("t5").isAdjacente(mapa.getParada(Mapa.KARAKORUM)));
        assertFalse(mapa.getProvincia("t1").isAdjacente(mapa.getParada(Mapa.KARAKORUM)));
    }

    // §7.3: 4 províncias do Khan; as setas de cada uma apontam para um yurt, uma espada e uma moeda.
    @Test
    public void cadaKhanApontaParaUmTributoDeCadaTipo() {
        assertEquals(4, mapa.getLocaisKhan().size());
        java.util.Set<Provincia> todas = new java.util.HashSet<>();
        for (LocalKhan k : mapa.getLocaisKhan()) {
            java.util.Set<Tributo> tipos = java.util.EnumSet.noneOf(Tributo.class);
            for (Provincia p : k.getProvincias()) tipos.add(p.getTipo());
            assertEquals(3, tipos.size());
            todas.addAll(k.getProvincias());
        }
        assertEquals(12, todas.size()); // cada província recebe tributo de um só Khan
    }

    // Cada cidade fica no meio de um anel de 3 paradas.
    @Test
    public void cidadesTemOAnelDeParadas() {
        for (Cidade c : mapa.getCidades()) assertEquals(c.nome(), 3, c.getAdjacentes().size());
        assertTrue(mapa.getCidade("Kiev").isAdjacente(mapa.getParada("r2")));
        assertThrows(IllegalArgumentException.class, () -> mapa.getCidade("Roma"));
    }

    @Test
    public void provinciaRecebeAte3Pecas() {
        mapa.prepararTributos();
        Provincia p = mapa.getProvincia("t1");
        assertEquals(1, p.getPecas());
        assertTrue(p.receber());
        assertTrue(p.receber());
        assertFalse(p.receber());
        assertEquals(3, p.getPecas());
        p.retirar();
        p.retirar();
        p.retirar();
        IllegalStateException e = assertThrows(IllegalStateException.class, p::retirar);
        assertTrue(e.getMessage().contains("Provincia.retirar"));
        assertThrows(IllegalArgumentException.class, () -> mapa.getProvincia("t13"));
    }
}
