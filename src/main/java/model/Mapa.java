package model;

import static model.Regiao.CHINA;
import static model.Regiao.PERSIA;
import static model.Regiao.RUSSIA;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// O mapa do tabuleiro, copiado à mão da foto (src/main/resources/imagens/tabuleiro.jpg, 2905 x 2919 pixels).
// As coordenadas são pixels dessa imagem. Ids: r = Rússia, p = Pérsia, c = China.
class Mapa {
    static final int RAIO = 60; // raio de um medalhão
    static final String KARAKORUM = "karakorum";

    private final Map<String, Parada> paradas = new LinkedHashMap<>();
    private final List<Cidade> cidades = new ArrayList<>();

    Mapa() {
        parada(KARAKORUM, null, 220, 1450, 1300);

        // Rússia
        parada("r1", RUSSIA, RAIO, 490, 255);     // Kiev, em cima
        parada("r2", RUSSIA, RAIO, 125, 540);     // Kiev, à esquerda
        parada("r3", RUSSIA, RAIO, 470, 540);     // Kiev, embaixo
        parada("r4", RUSSIA, RAIO, 855, 170, 930, 250); // dupla, no alto
        parada("r5", RUSSIA, RAIO, 855, 555);     // Moscou, à esquerda
        parada("r6", RUSSIA, RAIO, 1275, 490);    // Moscou, em cima
        parada("r7", RUSSIA, RAIO, 1080, 845);    // Moscou, embaixo
        parada("r8", RUSSIA, RAIO, 1755, 225);    // Sarai, em cima
        parada("r9", RUSSIA, RAIO, 1560, 665);    // Sarai, à esquerda
        parada("r10", RUSSIA, RAIO, 1890, 590);   // Sarai, à direita
        parada("r11", RUSSIA, RAIO, 2185, 495, 2290, 495); // dupla, entre a Rússia e a China

        // Pérsia
        parada("p1", PERSIA, RAIO, 462, 845, 452, 948); // dupla, à esquerda
        parada("p2", PERSIA, RAIO, 840, 1195);    // Samarcanda, em cima
        parada("p3", PERSIA, RAIO, 570, 1410);    // Samarcanda, à esquerda
        parada("p4", PERSIA, RAIO, 952, 1552);    // Samarcanda, à direita
        parada("p5", PERSIA, RAIO, 215, 1490);    // Bagdá, em cima
        parada("p6", PERSIA, RAIO, 455, 1648);    // Bagdá, à direita
        parada("p7", PERSIA, RAIO, 180, 1910);    // Bagdá, embaixo
        parada("p8", PERSIA, RAIO, 675, 1880, 785, 1895); // dupla, embaixo
        parada("p9", PERSIA, RAIO, 1275, 1825);   // Cabul, em cima
        parada("p10", PERSIA, RAIO, 1475, 2050);  // Cabul, à direita
        parada("p11", PERSIA, RAIO, 1210, 2235);  // Cabul, embaixo

        // China
        parada("c1", CHINA, RAIO, 2495, 655);     // Kaesong, em cima
        parada("c2", CHINA, RAIO, 2760, 880);     // Kaesong, à direita
        parada("c3", CHINA, RAIO, 2525, 1070);    // Kaesong, embaixo
        parada("c4", CHINA, RAIO, 2145, 1255);    // Pequim, em cima
        parada("c5", CHINA, RAIO, 1890, 1600);    // Pequim, à esquerda
        parada("c6", CHINA, RAIO, 2170, 1680);    // Pequim, embaixo
        parada("c7", CHINA, RAIO, 2555, 1500, 2555, 1605); // dupla, à direita
        parada("c8", CHINA, RAIO, 1775, 2220, 1895, 2235); // dupla, embaixo
        parada("c9", CHINA, RAIO, 2525, 2030);    // Cantão, em cima
        parada("c10", CHINA, RAIO, 2235, 2230);   // Cantão, à esquerda
        parada("c11", CHINA, RAIO, 2585, 2410);   // Cantão, embaixo

        // Rotas da Rússia
        rota("r1", "r2");
        rota("r1", "r3");
        rota("r2", "r3");
        rota("r1", "r4");
        rota("r1", "r5");
        rota("r3", "p1");
        rota("r4", "r6");
        rota("r5", "r6");
        rota("r5", "r7");
        rota("r6", "r7");
        rota("r5", "p1");
        rota("r7", "p2");
        rota("r6", "r9");
        rota("r8", "r9");
        rota("r8", "r10");
        rota("r9", "r10");
        rota("r9", KARAKORUM);
        rota("r10", "r11");
        rota("r11", KARAKORUM);
        rota("r11", "c1");

        // Rotas da Pérsia
        rota("p1", "p2");
        rota("p1", "p5");
        rota("p2", "p3");
        rota("p2", "p4");
        rota("p3", "p4");
        rota("p3", "p5");
        rota("p4", KARAKORUM);
        rota("p4", "p9");
        rota("p5", "p6");
        rota("p5", "p7");
        rota("p6", "p7");
        rota("p6", "p8");
        rota("p8", "p9");
        rota("p9", "p10");
        rota("p9", "p11");
        rota("p10", "p11");
        rota("p9", "c5");
        rota("p10", "c8");

        // Rotas da China
        rota("c1", "c2");
        rota("c1", "c3");
        rota("c2", "c3");
        rota("c3", "c4");
        rota("c3", "c7");
        rota("c4", KARAKORUM);
        rota("c4", "c5");
        rota("c4", "c6");
        rota("c5", "c6");
        rota("c5", "c8");
        rota("c6", "c9");
        rota("c7", "c9");
        rota("c8", "c10");
        rota("c9", "c10");
        rota("c9", "c11");
        rota("c10", "c11");

        cidades.add(new Cidade("Kiev", RUSSIA, 300, 390));
        cidades.add(new Cidade("Moscou", RUSSIA, 1080, 620));
        cidades.add(new Cidade("Sarai", RUSSIA, 1720, 420));
        cidades.add(new Cidade("Samarcanda", PERSIA, 800, 1400));
        cidades.add(new Cidade("Bagdá", PERSIA, 260, 1700));
        cidades.add(new Cidade("Cabul", PERSIA, 1270, 2030));
        cidades.add(new Cidade("Pequim", CHINA, 2090, 1480));
        cidades.add(new Cidade("Kaesong", CHINA, 2530, 820));
        cidades.add(new Cidade("Cantão", CHINA, 2460, 2220));
    }

    private void parada(String id, Regiao regiao, int raio, int... pontos) {
        paradas.put(id, new Parada(id, regiao, raio, pontos));
    }

    private void rota(String a, String b) {
        Parada pa = getParada(a), pb = getParada(b);
        pa.getVizinhas().add(pb);
        pb.getVizinhas().add(pa);
    }

    Parada getParada(String id) {
        Parada p = paradas.get(id);
        if (p == null)
            throw new IllegalArgumentException("Erro em Mapa.getParada: a parada " + id + " não existe.");
        return p;
    }

    Collection<Parada> getParadas() {
        return paradas.values();
    }

    /** Menor número de passos de a até b (busca em largura), ou -1 se não houver caminho. */
    int distancia(Parada a, Parada b) {
        Map<Parada, Integer> passos = new HashMap<>();
        Deque<Parada> fila = new ArrayDeque<>();
        passos.put(a, 0);
        fila.add(a);
        while (!fila.isEmpty()) {
            Parada p = fila.poll();
            if (p == b) return passos.get(p);
            for (Parada v : p.getVizinhas())
                if (passos.putIfAbsent(v, passos.get(p) + 1) == null) fila.add(v);
        }
        return -1;
    }

    /** A parada que contém o ponto (x, y) da imagem do tabuleiro, ou null. */
    Parada paradaEm(int x, int y) {
        for (Parada p : paradas.values())
            if (p.contem(x, y)) return p;
        return null;
    }

    List<Cidade> getCidades() {
        return cidades;
    }
}
