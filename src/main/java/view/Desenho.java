package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

// Desenhos com Java2D usados por mais de uma janela: a cartela com as peças de ativação, peças redondas e texto.
class Desenho {
    // Início e fim de cada coluna na cartela, em fração da largura da imagem.
    private static final double[][] COLUNAS = {{0.260, 0.414}, {0.440, 0.592}, {0.615, 0.766}, {0.810, 0.962}};
    // Início e fim de cada espaço de melhoria, de baixo para cima, e da área das ações impressas (embaixo), em fração
    // da altura da imagem.
    private static final double[][] LINHAS = {{0.585, 0.729}, {0.421, 0.565}, {0.258, 0.402}, {0.094, 0.238}};
    private static final double[] BASE = {0.759, 0.903};

    private Desenho() {
    }

    static void suavizar(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** Coluna (0 a 3) da cartela desenhada em (x, y, l, a) que contém o ponto (px, py), ou -1. */
    static int colunaEm(int px, int py, int x, int y, int l, int a) {
        if (py < y || py > y + a) return -1;
        double f = (double) (px - x) / l;
        for (int i = 0; i < COLUNAS.length; i++)
            if (f >= COLUNAS[i][0] && f <= COLUNAS[i][1]) return i;
        return -1;
    }

    /**
     * Cartela do jogador com as melhorias de cada coluna, de baixo para cima, e a peça de ativação sobre as ações
     * impressas de cada coluna usada (bloqueada) e da coluna ativada neste turno. A coluna ativada tem borda
     * branca; a coluna marcada, ainda não ativada, tem borda tracejada. melhorias tem uma lista por coluna (0 a 3);
     * ativada e selecionada valem -1 quando não há coluna.
     */
    static void cartela(Graphics2D g2, String nome, List<List<String>> melhorias, List<Integer> bloqueadas,
            int ativada, int selecionada, int x, int y, int l, int a) {
        g2.drawImage(Imagens.cartela(nome), x, y, l, a, null);
        int topo = y + a / 50, altura = a - 2 * (a / 50);
        for (int c = 0; c < COLUNAS.length; c++) {
            int x1 = x + (int) (COLUNAS[c][0] * l), x2 = x + (int) (COLUNAS[c][1] * l);
            int largura = x2 - x1;
            List<String> naColuna = melhorias.get(c);
            for (int k = 0; k < naColuna.size(); k++) {
                int y1 = y + (int) (LINHAS[k][0] * a), y2 = y + (int) (LINHAS[k][1] * a);
                melhoria(g2, naColuna.get(k), x1 + largura / 2, (y1 + y2) / 2, largura, y2 - y1);
            }
            boolean foiAtivada = c == ativada;
            if (bloqueadas.contains(c) || foiAtivada) {
                g2.setColor(new Color(0, 0, 0, 70));
                g2.fillRect(x1, topo, largura, altura);
                int b1 = y + (int) (BASE[0] * a), b2 = y + (int) (BASE[1] * a);
                int d = Math.min((int) ((b2 - b1) * 1.1), (int) (largura * 0.8));
                peca(g2, Imagens.ativacao(nome), x1 + largura / 2, (b1 + b2) / 2, d, Color.WHITE);
            }
            if (foiAtivada) {
                g2.setStroke(new BasicStroke(4));
                g2.setColor(Color.WHITE);
                g2.drawRect(x1, topo, largura, altura);
            } else if (c == selecionada) {
                g2.setColor(new Color(255, 215, 0, 100));
                g2.fillRect(x1, topo, largura, altura);
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                        new float[] {8f, 6f}, 0f));
                g2.setColor(new Color(255, 140, 0));
                g2.drawRect(x1, topo, largura, altura);
            }
        }
        g2.setStroke(new BasicStroke(1));
    }

    /**
     * Peça de melhoria (regras, p. 16), desenhada com formas e imagens do material porque não temos a imagem das
     * peças: um retângulo na cor da melhoria com o ícone do que ela dá. Moeda, espada, yurt e tesouro são virtuais
     * e levam um raio amarelo, como a moeda virtual impressa na cartela. O id tem a forma "<tipo>-<cor>".
     * (cx, cy) é o centro, e l e a são a largura e a altura.
     */
    static void melhoria(Graphics2D g2, String id, int cx, int cy, int l, int a) {
        int traco = id.lastIndexOf('-');
        String tipo = id.substring(0, traco), cor = id.substring(traco + 1);
        int x = cx - l / 2, y = cy - a / 2, arco = Math.max(4, a / 6);
        g2.setColor(Imagens.corMelhoria(cor));
        g2.fillRoundRect(x, y, l, a, arco, arco);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(70, 40, 20));
        g2.drawRoundRect(x, y, l, a, arco, arco);
        g2.setStroke(new BasicStroke(1));
        int d = (int) (Math.min(l * 0.45, a * 0.8));
        Color borda = new Color(90, 60, 30);
        switch (tipo) {
            case "cavalos" -> {
                peca(g2, Imagens.icone("cavalo"), cx - d / 2 - 1, cy, d, borda);
                peca(g2, Imagens.icone("cavalo"), cx + d / 2 + 1, cy, d, borda);
            }
            case "punho" -> peca(g2, Imagens.icone("punho"), cx, cy, d, borda);
            case "moeda", "espada", "yurt" -> {
                peca(g2, Imagens.tributo(tipo), cx, cy, d, borda);
                raio(g2, cx + d / 2 + 2, cy - d / 4, Math.max(8, d / 2));
            }
            default -> { // tesouro virtual
                peca(g2, Imagens.tesouro(tipo), cx, cy, d, borda);
                raio(g2, cx + d / 2 + 2, cy - d / 4, Math.max(8, d / 2));
            }
        }
    }

    /**
     * Peça de bônus (regras, p. 17), desenhada com formas e ícones do material porque não temos a imagem das
     * peças: um quadrado roxo, como o espaço do bônus no tabuleiro, com os ícones do que a peça dá.
     * (cx, cy) é o centro e d é o lado. Uma peça usada fica escura.
     */
    static void bonus(Graphics2D g2, String id, int cx, int cy, int d, boolean usada) {
        int x = cx - d / 2, y = cy - d / 2, arco = Math.max(4, d / 5);
        g2.setColor(new Color(92, 74, 140));
        g2.fillRoundRect(x, y, d, d, arco, arco);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(235, 205, 120));
        g2.drawRoundRect(x, y, d, d, arco, arco);
        g2.setStroke(new BasicStroke(1));
        java.util.List<BufferedImage> icones = new java.util.ArrayList<>();
        switch (id) {
            case "trono" -> icones.add(Imagens.icone("voto"));
            case "espadas" -> icones.addAll(java.util.List.of(Imagens.tributo("espada"), Imagens.tributo("espada")));
            case "moedas" -> icones.addAll(java.util.List.of(Imagens.tributo("moeda"), Imagens.tributo("moeda")));
            case "yurts" -> icones.addAll(java.util.List.of(Imagens.tributo("yurt"), Imagens.tributo("yurt")));
            case "cavalos-punho" -> icones.addAll(java.util.List.of(Imagens.icone("cavalo"), Imagens.icone("punho")));
            case "punhos" -> icones.addAll(java.util.List.of(Imagens.icone("punho"), Imagens.icone("punho")));
            default -> icones.addAll(java.util.List.of(Imagens.tesouro("seda"), Imagens.tesouro("ferro"),
                    Imagens.tesouro("boi"))); // baú: 3 tesouros
        }
        int n = icones.size(), lado = (int) (d * (n == 1 ? 0.7 : n == 2 ? 0.44 : 0.3));
        for (int k = 0; k < n; k++) {
            int ix = cx + (int) Math.round((k - (n - 1) / 2.0) * (lado + 2));
            peca(g2, icones.get(k), ix, cy - (id.equals("cavalos-punho") ? d / 10 : 0), lado, new Color(60, 40, 80));
        }
        Font antes = g2.getFont();
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, d / 5)));
        if (id.equals("trono")) texto(g2, "+1", x + 3, y + d - 4);
        if (id.equals("cavalos-punho")) texto(g2, "3", cx - lado - 4, cy + d / 2 - 3);
        g2.setFont(antes);
        if (usada) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(x, y, d, d, arco, arco);
        }
    }

    /**
     * Carta de conselheiro secreto (regras, p. 19), desenhada porque o material não tem as imagens: o nome do
     * conselheiro, o objetivo e a tabela de quanto é preciso para 1, 2 e 3 votos (faixas).
     */
    static void cartaSecreta(Graphics2D g2, String nome, String objetivo, int[] faixas, int x, int y, int l, int a) {
        g2.setColor(new Color(60, 40, 80));
        g2.fillRoundRect(x, y, l, a, 16, 16);
        g2.setColor(new Color(236, 222, 190));
        g2.fillRoundRect(x + 6, y + 6, l - 12, a - 12, 12, 12);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(110, 80, 40));
        g2.drawRoundRect(x + 10, y + 10, l - 20, a - 20, 10, 10);
        g2.setStroke(new BasicStroke(1));
        Font antes = g2.getFont();
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 22));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(70, 40, 20));
        g2.drawString(nome, x + (l - fm.stringWidth(nome)) / 2, y + 44);
        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 15));
        int ty = linhasCentralizadas(g2, objetivo, x, y + 80, l);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        for (int v = 0; v < 3; v++) {
            int ly = Math.max(ty + 20, y + a - 110) + v * 30;
            for (int k = 0; k <= v; k++)
                g2.drawImage(Imagens.icone("voto"), x + 30 + k * 22, ly - 18, 24, 24, null);
            g2.setColor(new Color(70, 40, 20));
            g2.drawString("a partir de " + faixas[v], x + 110, ly);
        }
        g2.setFont(antes);
    }

    // Texto quebrado em linhas centralizadas na largura l. Retorna o y depois da última linha.
    private static int linhasCentralizadas(Graphics2D g2, String s, int x, int y, int l) {
        java.awt.FontMetrics fm = g2.getFontMetrics();
        java.util.List<String> linhas = new java.util.ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : s.split(" ")) {
            if (linha.length() > 0 && fm.stringWidth(linha + " " + palavra) > l - 30) {
                linhas.add(linha.toString());
                linha.setLength(0);
            }
            if (linha.length() > 0) linha.append(' ');
            linha.append(palavra);
        }
        linhas.add(linha.toString());
        g2.setColor(new Color(70, 40, 20));
        for (String t : linhas) {
            g2.drawString(t, x + (l - fm.stringWidth(t)) / 2, y);
            y += fm.getHeight();
        }
        return y;
    }

    /** Carta de conselheiro aberto sem imagem no material: um cartão com o texto do objetivo. */
    static void cartaSemImagem(Graphics2D g2, String descricao, int x, int y, int l, int a) {
        g2.setColor(new Color(236, 222, 190));
        g2.fillRoundRect(x, y, l, a, 14, 14);
        g2.setStroke(new BasicStroke(3));
        g2.setColor(new Color(110, 80, 40));
        g2.drawRoundRect(x + 4, y + 4, l - 8, a - 8, 10, 10);
        g2.setStroke(new BasicStroke(1));
        g2.drawImage(Imagens.icone("voto"), x + l / 2 - 25, y + 18, 50, 50, null);
        Font antes = g2.getFont();
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 16));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        java.util.List<String> linhas = new java.util.ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : descricao.replace("Primeiro a ", "").split(" ")) {
            if (linha.length() > 0 && fm.stringWidth(linha + " " + palavra) > l - 24) {
                linhas.add(linha.toString());
                linha.setLength(0);
            }
            if (linha.length() > 0) linha.append(' ');
            linha.append(palavra);
        }
        linhas.add(linha.toString());
        g2.setColor(new Color(70, 40, 20));
        int ty = y + 100;
        for (String s : linhas) {
            g2.drawString(s, x + (l - fm.stringWidth(s)) / 2, ty);
            ty += fm.getHeight();
        }
        g2.setFont(antes);
    }

    // Raio amarelo que marca uma peça virtual: (cx, cy) é o centro e a é a altura.
    private static void raio(Graphics2D g2, int cx, int cy, int a) {
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        p.moveTo(cx + a * 0.15, cy - a / 2.0);
        p.lineTo(cx - a * 0.25, cy + a * 0.05);
        p.lineTo(cx, cy + a * 0.05);
        p.lineTo(cx - a * 0.15, cy + a / 2.0);
        p.lineTo(cx + a * 0.25, cy - a * 0.05);
        p.lineTo(cx, cy - a * 0.05);
        p.closePath();
        g2.setColor(new Color(255, 220, 0));
        g2.fill(p);
        g2.setColor(new Color(60, 60, 160));
        g2.draw(p);
    }

    /** Peça redonda: a imagem recortada num círculo de diâmetro d centrado em (cx, cy), com borda. */
    static void peca(Graphics2D g2, BufferedImage imagem, int cx, int cy, int d, Color borda) {
        Shape antes = g2.getClip();
        Ellipse2D circulo = new Ellipse2D.Double(cx - d / 2.0, cy - d / 2.0, d, d);
        g2.clip(circulo);
        g2.drawImage(imagem, cx - d / 2, cy - d / 2, d, d, null);
        g2.setClip(antes);
        g2.setStroke(new BasicStroke(3));
        g2.setColor(borda);
        g2.draw(circulo);
        g2.setStroke(new BasicStroke(1));
    }

    /**
     * Pião do Khan, desenhado só com formas porque não há imagem dele no material: cabeça redonda e corpo
     * em trapézio, dourados com contorno escuro. (cx, cy) é o centro e a é a altura.
     */
    static void khan(Graphics2D g2, int cx, int cy, int a) {
        double r = a * 0.2;
        java.awt.geom.Path2D corpo = new java.awt.geom.Path2D.Double();
        corpo.moveTo(cx - a * 0.32, cy + a / 2.0);
        corpo.lineTo(cx + a * 0.32, cy + a / 2.0);
        corpo.lineTo(cx + a * 0.12, cy - a * 0.05);
        corpo.lineTo(cx - a * 0.12, cy - a * 0.05);
        corpo.closePath();
        Ellipse2D cabeca = new Ellipse2D.Double(cx - r, cy - a / 2.0, 2 * r, 2 * r);
        g2.setColor(new Color(255, 200, 40));
        g2.fill(corpo);
        g2.fill(cabeca);
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(70, 30, 0));
        g2.draw(corpo);
        g2.draw(cabeca);
        g2.setStroke(new BasicStroke(1));
    }

    /**
     * Yurt de um jogador, desenhado só com formas porque o material não tem imagem dos yurts: parede retangular
     * e teto em cúpula na cor do jogador, com porta e contorno escuros. (cx, cy) é o centro e l é a largura.
     */
    static void yurt(Graphics2D g2, int cx, int cy, int l, Color cor) {
        int a = (int) (l * 0.8);
        int topo = cy - a / 2, paredeY = cy - a / 10;
        java.awt.geom.Path2D forma = new java.awt.geom.Path2D.Double();
        forma.moveTo(cx - l / 2.0, cy + a / 2.0);
        forma.lineTo(cx - l / 2.0, paredeY);
        forma.quadTo(cx - l * 0.35, topo, cx, topo);
        forma.quadTo(cx + l * 0.35, topo, cx + l / 2.0, paredeY);
        forma.lineTo(cx + l / 2.0, cy + a / 2.0);
        forma.closePath();
        g2.setColor(cor);
        g2.fill(forma);
        g2.setColor(new Color(40, 20, 0));
        g2.fillRect(cx - l / 8, cy + a / 8, l / 4, a / 2 - a / 8);
        g2.setStroke(new BasicStroke(2));
        g2.draw(forma);
        g2.drawLine(cx - l / 2, paredeY, cx + l / 2, paredeY);
        g2.setStroke(new BasicStroke(1));
    }

    /** Pequena peça (tributo ou tesouro) com a quantidade ao lado, para a reserva dos jogadores. Retorna o x seguinte. */
    static int contador(Graphics2D g2, BufferedImage imagem, int n, int x, int y) {
        peca(g2, imagem, x + 11, y - 6, 22, new Color(90, 60, 30));
        texto(g2, String.valueOf(n), x + 25, y);
        return x + 25 + g2.getFontMetrics().stringWidth(String.valueOf(n)) + 10;
    }

    /** Mensagem de erro numa caixa branca de largura l, quebrada em linhas. Substitui as caixas de diálogo. */
    static void aviso(Graphics2D g2, String s, int x, int y, int l) {
        java.awt.FontMetrics fm = g2.getFontMetrics();
        java.util.List<String> linhas = new java.util.ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : s.split(" ")) {
            if (linha.length() > 0 && fm.stringWidth(linha + " " + palavra) > l - 16) {
                linhas.add(linha.toString());
                linha.setLength(0);
            }
            if (linha.length() > 0) linha.append(' ');
            linha.append(palavra);
        }
        linhas.add(linha.toString());
        int alturaLinha = fm.getHeight();
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(x, y, l, linhas.size() * alturaLinha + 10, 10, 10);
        g2.setColor(new Color(180, 0, 0));
        for (int i = 0; i < linhas.size(); i++)
            g2.drawString(linhas.get(i), x + 8, y + 5 + fm.getAscent() + i * alturaLinha);
    }

    /** Texto branco com sombra preta, legível sobre qualquer fundo. */
    static void texto(Graphics2D g2, String s, int x, int y) {
        g2.setColor(Color.BLACK);
        g2.drawString(s, x + 1, y + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(s, x, y);
    }
}
