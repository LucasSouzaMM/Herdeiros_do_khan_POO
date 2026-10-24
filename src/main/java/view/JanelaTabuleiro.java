package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import model.Jogo;

// RF08 a RF13: janela do tabuleiro. O tabuleiro fica à esquerda e o painel do jogador da vez à direita.
class JanelaTabuleiro extends JFrame {
    // As coordenadas do modelo são pixels da imagem original (2905 x 2919). Na tela, o tabuleiro tem 980 de altura.
    private static final double ESCALA = 980.0 / 2919;
    private static final int TAB_L = (int) Math.round(2905 * ESCALA), TAB_A = 980;
    private static final int PAINEL_L = 380;
    private static final int CARTELA_X = TAB_L + 10, CARTELA_Y = 80, CARTELA_L = 360, CARTELA_A = 196;
    // Início e fim de cada coluna na cartela, em fração da largura da imagem.
    private static final double[][] COLUNAS = {{0.260, 0.414}, {0.440, 0.592}, {0.615, 0.766}, {0.810, 0.962}};
    private static final int PEAO = 30;
    private static final boolean DEBUG = Boolean.getBoolean("mapa.debug"); // G21: java -Dmapa.debug=true ...

    private final Jogo jogo;
    private final Painel painel = new Painel();
    private final JButton proximo = new JButton("Próximo");

    JanelaTabuleiro(Jogo jogo) {
        super("Herdeiros do Khan");
        this.jogo = jogo;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        painel.setLayout(null);
        painel.setPreferredSize(new Dimension(TAB_L + PAINEL_L, TAB_A));
        painel.addMouseListener(new Clique());

        proximo.setBounds(TAB_L + 20, TAB_A - 110, PAINEL_L - 40, 36);
        proximo.addActionListener(new Proximo());
        painel.add(proximo);
        JButton salvar = new JButton("Salvar jogo");
        salvar.setBounds(TAB_L + 20, TAB_A - 60, PAINEL_L - 40, 36);
        salvar.addActionListener(new Salvar());
        painel.add(salvar);

        add(painel);
        pack();
        setLocationRelativeTo(null);
        atualizar();
        setVisible(true);
        System.out.println("Vez de " + jogo.getNome(jogo.getJogadorAtual()) + ".");
    }

    // RF13, G16: o botão Próximo só funciona depois de ativar uma coluna.
    private void atualizar() {
        proximo.setEnabled(jogo.getColunaAtivada() != -1);
        painel.repaint();
    }

    private void avisarSeAVezMudou(int antes) {
        if (jogo.getJogadorAtual() != antes)
            System.out.println("Vez de " + jogo.getNome(jogo.getJogadorAtual()) + ".");
    }

    private static int tela(int coordenada) {
        return (int) Math.round(coordenada * ESCALA);
    }

    private static int colunaEm(int x, int y) {
        if (y < CARTELA_Y || y > CARTELA_Y + CARTELA_A) return -1;
        double f = (double) (x - CARTELA_X) / CARTELA_L;
        for (int i = 0; i < COLUNAS.length; i++)
            if (f >= COLUNAS[i][0] && f <= COLUNAS[i][1]) return i;
        return -1;
    }

    // RF11, RF12: clique numa coluna da cartela ou numa parada do tabuleiro. Uma jogada inválida não muda nada na tela.
    private class Clique extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int atual = jogo.getJogadorAtual();
            String nome = jogo.getNome(atual);
            try {
                if (e.getX() < TAB_L) {
                    String parada = jogo.paradaEm((int) (e.getX() / ESCALA), (int) (e.getY() / ESCALA));
                    if (parada == null) return;
                    jogo.mover(atual, parada);
                    System.out.println(nome + " moveu para " + parada + ". Movimentos restantes: " + jogo.getMovimentos() + ".");
                } else {
                    int coluna = colunaEm(e.getX(), e.getY());
                    if (coluna == -1) return;
                    jogo.ativarColuna(atual, coluna);
                    System.out.println(nome + " ativou a coluna " + (coluna + 1) + ": " + jogo.getMovimentos() + " movimento(s).");
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                System.out.println(ex.getMessage());
            }
            avisarSeAVezMudou(atual);
            atualizar();
        }
    }

    private class Proximo implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int atual = jogo.getJogadorAtual();
            try {
                jogo.encerrarTurno(atual);
                System.out.println(jogo.getNome(atual) + " encerrou o turno.");
            } catch (IllegalStateException ex) {
                System.out.println(ex.getMessage());
            }
            avisarSeAVezMudou(atual);
            atualizar();
        }
    }

    // RF08
    private class Salvar implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser escolha = new JFileChooser(".");
            if (escolha.showSaveDialog(JanelaTabuleiro.this) != JFileChooser.APPROVE_OPTION) return;
            try {
                jogo.salvar(escolha.getSelectedFile().toPath());
                System.out.println("Partida salva em " + escolha.getSelectedFile() + ".");
            } catch (IOException ex) {
                System.out.println("Erro em JanelaTabuleiro.Salvar: não foi possível gravar o arquivo (" + ex.getMessage() + ").");
            }
        }
    }

    private class Painel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(Imagens.tabuleiro(), 0, 0, TAB_L, TAB_A, null);
            if (DEBUG) desenharMapa(g2);
            desenharPioes(g2);
            desenharPainel(g2);
        }

        // RF09, G20: os piões de uma parada ficam lado a lado. Numa parada dupla, cada um fica num medalhão.
        private void desenharPioes(Graphics2D g2) {
            Map<String, List<Integer>> porParada = new LinkedHashMap<>();
            for (int i = 0; i < jogo.getNumeroJogadores(); i++) {
                List<Integer> pioes = porParada.get(jogo.getPosicao(i));
                if (pioes == null) {
                    pioes = new ArrayList<>();
                    porParada.put(jogo.getPosicao(i), pioes);
                }
                pioes.add(i);
            }
            for (Map.Entry<String, List<Integer>> p : porParada.entrySet()) {
                int[] pontos = jogo.getPontos(p.getKey());
                List<Integer> pioes = p.getValue();
                for (int k = 0; k < pioes.size(); k++) {
                    int x, y;
                    if (pontos.length == 4 && pioes.size() <= 2) {
                        x = tela(pontos[2 * k]);
                        y = tela(pontos[2 * k + 1]);
                    } else {
                        x = tela(pontos[0]) + (int) Math.round((k - (pioes.size() - 1) / 2.0) * (PEAO + 4));
                        y = tela(pontos[1]);
                    }
                    desenharPeao(g2, jogo.getNome(pioes.get(k)), x, y, PEAO);
                }
            }
        }

        private void desenharPeao(Graphics2D g2, String herdeiro, int x, int y, int tamanho) {
            Shape antes = g2.getClip();
            Ellipse2D circulo = new Ellipse2D.Double(x - tamanho / 2.0, y - tamanho / 2.0, tamanho, tamanho);
            g2.clip(circulo);
            g2.drawImage(Imagens.peao(herdeiro), x - tamanho / 2, y - tamanho / 2, tamanho, tamanho, null);
            g2.setClip(antes);
            g2.setStroke(new BasicStroke(3));
            g2.setColor(Imagens.cor(herdeiro));
            g2.draw(circulo);
        }

        // RF10: o painel tem a cor do jogador da vez, a sua cartela e os movimentos restantes.
        private void desenharPainel(Graphics2D g2) {
            int atual = jogo.getJogadorAtual();
            String nome = jogo.getNome(atual);
            g2.setColor(Imagens.cor(nome));
            g2.fillRect(TAB_L, 0, PAINEL_L, TAB_A);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            texto(g2, "Vez de " + nome, TAB_L + 15, 38);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            int coluna = jogo.getColunaAtivada();
            texto(g2, coluna == -1 ? "Clique em uma coluna da cartela" : "Movimentos restantes: " + jogo.getMovimentos(),
                    TAB_L + 15, 64);

            g2.drawImage(Imagens.cartela(nome), CARTELA_X, CARTELA_Y, CARTELA_L, CARTELA_A, null);
            if (coluna != -1) {
                int x1 = CARTELA_X + (int) (COLUNAS[coluna][0] * CARTELA_L);
                int x2 = CARTELA_X + (int) (COLUNAS[coluna][1] * CARTELA_L);
                g2.setStroke(new BasicStroke(4));
                g2.setColor(Color.WHITE);
                g2.drawRect(x1, CARTELA_Y + 4, x2 - x1, CARTELA_A - 8);
            }

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            texto(g2, "Jogadores", TAB_L + 15, 310);
            for (int i = 0; i < jogo.getNumeroJogadores(); i++) {
                int y = 345 + i * 40;
                desenharPeao(g2, jogo.getNome(i), TAB_L + 30, y - 6, 28);
                texto(g2, (i + 1) + ". " + jogo.getNome(i) + (i == atual ? "  (vez)" : ""), TAB_L + 55, y);
            }
        }

        private void texto(Graphics2D g2, String s, int x, int y) {
            g2.setColor(Color.BLACK);
            g2.drawString(s, x + 1, y + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(s, x, y);
        }

        // G21: cada parada com o seu id e a sua área de clique, e cada rota como uma linha.
        private void desenharMapa(Graphics2D g2) {
            g2.setStroke(new BasicStroke(3));
            g2.setColor(new Color(0, 90, 255, 200));
            for (String id : jogo.getParadas())
                for (String v : jogo.getVizinhas(id))
                    if (id.compareTo(v) < 0) {
                        int[] a = centro(id), b = centro(v);
                        g2.drawLine(a[0], a[1], b[0], b[1]);
                    }
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            for (String id : jogo.getParadas()) {
                int[] pontos = jogo.getPontos(id);
                int r = tela(jogo.getRaio(id));
                g2.setColor(Color.MAGENTA);
                for (int i = 0; i < pontos.length; i += 2)
                    g2.drawOval(tela(pontos[i]) - r, tela(pontos[i + 1]) - r, 2 * r, 2 * r);
                int[] c = centro(id);
                texto(g2, id, c[0] - 8, c[1] + 5);
            }
        }

        private int[] centro(String id) {
            int[] p = jogo.getPontos(id);
            return p.length == 4 ? new int[] {tela((p[0] + p[2]) / 2), tela((p[1] + p[3]) / 2)}
                    : new int[] {tela(p[0]), tela(p[1])};
        }
    }
}
