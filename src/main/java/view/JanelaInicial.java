package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controller.Controlador;

// RF05: primeira janela. Começa uma nova partida ou continua uma partida salva.
public class JanelaInicial extends JFrame {
    private static final int LADO = 800;

    private final Painel painel = new Painel();
    private String aviso; // RF05, CA06: aviso de arquivo inválido, desenhado na própria janela

    public JanelaInicial() {
        super("Herdeiros do Khan");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        painel.setLayout(null);
        painel.setPreferredSize(new Dimension(LADO, LADO));
        JButton novo = new JButton("Novo jogo");
        novo.setBounds(LADO / 2 - 100, LADO / 2 - 60, 200, 40);
        novo.addActionListener(new NovoJogo());
        painel.add(novo);
        JButton continuar = new JButton("Continuar");
        continuar.setBounds(LADO / 2 - 100, LADO / 2, 200, 40);
        continuar.addActionListener(new Continuar());
        painel.add(continuar);

        add(painel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class NovoJogo implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
            new JanelaHerdeiros();
        }
    }

    // A4: o nome e a pasta são livres; só o tipo (.txt) é predefinido, e "Todos os arquivos" continua na lista.
    // Usado também para salvar (JanelaTabuleiro).
    static JFileChooser seletorTxt() {
        JFileChooser escolha = new JFileChooser(".");
        escolha.setFileFilter(new FileNameExtensionFilter("Partida salva (*.txt)", "txt"));
        return escolha;
    }

    private class Continuar implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            aviso = null;
            JFileChooser escolha = seletorTxt();
            if (escolha.showOpenDialog(JanelaInicial.this) != JFileChooser.APPROVE_OPTION) {
                painel.repaint();
                return;
            }
            aviso = Controlador.getInstance().carregar(escolha.getSelectedFile().toPath());
            if (aviso == null) {
                dispose();
                new JanelaTabuleiro();
                return;
            }
            painel.repaint();
        }
    }

    private class Painel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(Imagens.tabuleiro(), 0, 0, LADO, LADO, null);
            if (aviso != null) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                int largura = g2.getFontMetrics().stringWidth(aviso);
                g2.setColor(Color.WHITE);
                g2.fillRect((LADO - largura) / 2 - 10, LADO / 2 + 60, largura + 20, 34);
                g2.setColor(Color.RED);
                g2.drawString(aviso, (LADO - largura) / 2, LADO / 2 + 83);
            }
        }
    }
}
