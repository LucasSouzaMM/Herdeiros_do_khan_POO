package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class MapaTest {

    private final Mapa mapa = new Mapa();

    // J16
    @Test(timeout = 1000)
    public void temAsParadasDoTabuleiro() {
        assertEquals("34 paradas", 34, mapa.getParadas().size());
        int duplas = 0;
        for (Parada p : mapa.getParadas()) if (p.isDupla()) duplas++;
        assertEquals("6 paradas duplas", 6, duplas);
        assertEquals("9 cidades", 9, mapa.getCidades().size());
    }

    // Cada cidade fica no anel de 3 paradas (Sarai: r8, r9 e r10).
    @Test(timeout = 1000)
    public void cidadeTemAnelDeParadas() {
        java.util.List<String> anel = new java.util.ArrayList<>();
        for (Parada p : mapa.getCidade("Sarai").getAdjacentes()) anel.add(p.getId());
        assertEquals("o anel de Sarai", java.util.List.of("r8", "r9", "r10"), anel);
    }

    // J16
    @Test(timeout = 1000)
    public void rotasLigamOsDoisLados() {
        for (Parada p : mapa.getParadas())
            for (Parada v : p.getVizinhas()) assertTrue(p.getId() + "-" + v.getId(), v.getVizinhas().contains(p));
    }

    // J16
    @Test(timeout = 1000)
    public void todaParadaEAlcancavelDeKarakorum() {
        Parada k = mapa.getParada(Mapa.KARAKORUM);
        for (Parada p : mapa.getParadas()) assertTrue(p.getId(), mapa.distancia(k, p) >= 0);
    }

    @Test(timeout = 1000)
    public void distanciaEOMenorNumeroDePassos() {
        Parada k = mapa.getParada(Mapa.KARAKORUM);
        assertEquals("de Karakorum a Karakorum", 0, mapa.distancia(k, k));
        assertEquals("r9 é vizinha de Karakorum", 1, mapa.distancia(k, mapa.getParada("r9")));
        assertEquals("r8 fica a 2 passos de Karakorum", 2, mapa.distancia(k, mapa.getParada("r8")));
    }

    // §7.1: atravessar uma parada dupla custa só 1 movimento.
    @Test(timeout = 1000)
    public void paradaDuplaCustaUmMovimento() {
        assertEquals("c1 fica a 2 passos de Karakorum, passando pela parada dupla r11",
                2, mapa.distancia(mapa.getParada(Mapa.KARAKORUM), mapa.getParada("c1")));
    }

    // J17
    @Test(timeout = 1000)
    public void paradaEmPontoDoTabuleiro() {
        assertEquals("o centro de r1", "r1", mapa.paradaEm(490, 255).getId());
        assertEquals("a borda do raio de r1", "r1", mapa.paradaEm(490 + Mapa.RAIO, 255).getId());
        assertNull("1 pixel fora do raio de r1", mapa.paradaEm(490 + Mapa.RAIO + 1, 255));
        assertEquals("o 1º medalhão de r4", "r4", mapa.paradaEm(855, 170).getId());
        assertEquals("o 2º medalhão de r4", "r4", mapa.paradaEm(930, 250).getId());
        assertEquals("Karakorum", Mapa.KARAKORUM, mapa.paradaEm(1450, 1300).getId());
        assertNull("o canto do tabuleiro não tem parada", mapa.paradaEm(10, 10));
    }

    @Test(timeout = 1000)
    public void rejeitaParadaInexistente() {
        IllegalArgumentException e = assertThrows("parada que não existe é recusada",
                IllegalArgumentException.class, () -> mapa.getParada("x1"));
        assertTrue(e.getMessage(), e.getMessage().contains("Mapa.getParada"));
    }

    // §7.2: 12 províncias, 4 de cada tipo de tributo, cada uma na borda de 4 a 6 paradas.
    @Test(timeout = 1000)
    public void temAsProvinciasDoTabuleiro() {
        assertEquals("12 províncias", 12, mapa.getProvincias().size());
        for (Tributo t : Tributo.values()) {
            int n = 0;
            for (Provincia p : mapa.getProvincias()) if (p.getTipo() == t) n++;
            assertEquals(t.getNome(), 4, n);
        }
        for (Provincia p : mapa.getProvincias()) {
            int n = p.getAdjacentes().size();
            assertTrue(p.getId(), n >= 4 && n <= 6);
            assertEquals(p.getId() + ": sem peças antes de começar a partida", 0, p.getPecas());
        }
        assertTrue("t5 fica ao lado de Karakorum", mapa.getProvincia("t5").isAdjacente(mapa.getParada(Mapa.KARAKORUM)));
        assertFalse("t1 não fica ao lado de Karakorum",
                mapa.getProvincia("t1").isAdjacente(mapa.getParada(Mapa.KARAKORUM)));
    }

    // §7.3: 4 províncias do Khan; as setas de cada uma apontam para um yurt, uma espada e uma moeda.
    @Test(timeout = 1000)
    public void cadaKhanApontaParaUmTributoDeCadaTipo() {
        assertEquals("4 lugares do Khan", 4, mapa.getLocaisKhan().size());
        java.util.Set<Provincia> todas = new java.util.HashSet<>();
        for (LocalKhan k : mapa.getLocaisKhan()) {
            java.util.Set<Tributo> tipos = java.util.EnumSet.noneOf(Tributo.class);
            for (Provincia p : k.getProvincias()) tipos.add(p.getTipo());
            assertEquals("cada Khan aponta para 1 yurt, 1 espada e 1 moeda", 3, tipos.size());
            todas.addAll(k.getProvincias());
        }
        assertEquals("cada província recebe tributo de um só Khan", 12, todas.size());
    }

    // Cada cidade fica no meio de um anel de 3 paradas.
    @Test(timeout = 1000)
    public void cidadesTemOAnelDeParadas() {
        for (Cidade c : mapa.getCidades()) assertEquals(c.nome(), 3, c.getAdjacentes().size());
        assertTrue("Kiev fica ao lado de r2", mapa.getCidade("Kiev").isAdjacente(mapa.getParada("r2")));
        assertThrows("cidade que não existe é recusada", IllegalArgumentException.class, () -> mapa.getCidade("Roma"));
    }

    @Test(timeout = 1000)
    public void provinciaRecebeAte3Pecas() {
        mapa.prepararTributos();
        Provincia p = mapa.getProvincia("t1");
        assertEquals("t1 começa com 1 peça", 1, p.getPecas());
        assertTrue("2ª peça", p.receber());
        assertTrue("3ª peça", p.receber());
        assertFalse("a 4ª peça não cabe", p.receber());
        assertEquals("t1 fica com 3 peças", 3, p.getPecas());
        p.retirar();
        p.retirar();
        p.retirar();
        IllegalStateException e = assertThrows("província vazia não tem peça para tirar",
                IllegalStateException.class, p::retirar);
        assertTrue(e.getMessage(), e.getMessage().contains("Provincia.retirar"));
        assertThrows("província que não existe é recusada",
                IllegalArgumentException.class, () -> mapa.getProvincia("t13"));
    }
}
