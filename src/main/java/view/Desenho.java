package view;

import java.awt.BasicStroke;
import java.awt.Color;
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
     * Cartela do jogador da vez com a peça de ativação sobre cada coluna bloqueada e sobre a coluna ativada
     * neste turno. A coluna ativada tem borda branca; a coluna marcada, ainda não ativada, tem borda tracejada.
     * ativada e selecionada valem -1 quando não há coluna.
     */
    static void cartela(Graphics2D g2, String nome, List<Integer> bloqueadas, int ativada, int selecionada,
            int x, int y, int l, int a) {
        g2.drawImage(Imagens.cartela(nome), x, y, l, a, null);
        int topo = y + a / 50, altura = a - 2 * (a / 50);
        for (int c = 0; c < COLUNAS.length; c++) {
            int x1 = x + (int) (COLUNAS[c][0] * l), x2 = x + (int) (COLUNAS[c][1] * l);
            int largura = x2 - x1;
            boolean foiAtivada = c == ativada;
            if (bloqueadas.contains(c) || foiAtivada) {
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fillRect(x1, topo, largura, altura);
                int d = (int) (largura * 0.8);
                peca(g2, Imagens.ativacao(nome), x1 + largura / 2, topo + altura / 2, d, Color.WHITE);
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
