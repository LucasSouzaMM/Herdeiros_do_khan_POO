package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import model.Jogo;

// RF06, RF07: escolha dos herdeiros. A ordem em que eles são marcados é a ordem dos jogadores.
class JanelaHerdeiros extends JFrame {
    private static final int LARGURA = 420, ALTURA = 229, MARGEM = 10; // cada cartela, na proporção da imagem

    private final List<String> herdeiros = Jogo.getHerdeiros();
    private final List<String> escolhidos = new ArrayList<>();
    private final List<JCheckBox> caixas = new ArrayList<>();
    private final JRadioButton sortear = new JRadioButton("Sortear herdeiros");
    private final JComboBox<Integer> numero = new JComboBox<>(new Integer[] {2, 3, 4, 5});
    private final Painel painel = new Painel();

    JanelaHerdeiros() {
        super("Herdeiros do Khan - Herdeiros");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        painel.setLayout(null);
        painel.setPreferredSize(new Dimension(3 * LARGURA + 4 * MARGEM, 2 * ALTURA + 3 * MARGEM));

        for (int i = 0; i < herdeiros.size(); i++) {
            JCheckBox caixa = new JCheckBox(herdeiros.get(i));
            caixa.setBounds(x(i) + 8, y(i) + ALTURA - 32, 110, 24);
            caixa.addItemListener(new Marcar(herdeiros.get(i)));
            caixas.add(caixa);
            painel.add(caixa);
        }

        // A sexta célula da grade tem os controles.
        int cx = x(5) + 20, cy = y(5) + 20;
        JRadioButton escolher = new JRadioButton("Escolher herdeiros (na ordem dos jogadores)", true);
        escolher.setBounds(cx, cy, 380, 24);
        sortear.setBounds(cx, cy + 34, 160, 24);
        numero.setBounds(cx + 170, cy + 34, 60, 24);
        numero.setEnabled(false);
        ButtonGroup modo = new ButtonGroup();
        modo.add(escolher);
        modo.add(sortear);
        TrocarModo trocar = new TrocarModo();
        escolher.addActionListener(trocar);
        sortear.addActionListener(trocar);
        JButton jogar = new JButton("Jogar");
        jogar.setBounds(cx, cy + 130, 120, 36);
        jogar.addActionListener(new Jogar());
        painel.add(escolher);
        painel.add(sortear);
        painel.add(numero);
        painel.add(jogar);

        add(painel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private static int x(int i) {
        return MARGEM + (i % 3) * (LARGURA + MARGEM);
    }

    private static int y(int i) {
        return MARGEM + (i / 3) * (ALTURA + MARGEM);
    }

    private class Marcar implements ItemListener {
        private final String herdeiro;

        Marcar(String herdeiro) {
            this.herdeiro = herdeiro;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) escolhidos.add(herdeiro);
            else escolhidos.remove(herdeiro);
            painel.repaint();
        }
    }

    private class TrocarModo implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            numero.setEnabled(sortear.isSelected());
            for (JCheckBox caixa : caixas) caixa.setEnabled(!sortear.isSelected());
            painel.repaint();
        }
    }

    private class Jogar implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Jogo jogo;
            try {
                jogo = sortear.isSelected() ? Jogo.sortear((Integer) numero.getSelectedItem()) : new Jogo(escolhidos);
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage()); // G4: na janela, nada acontece
                return;
            }
            StringBuilder ordem = new StringBuilder("Partida criada. Ordem dos jogadores:");
            for (int i = 0; i < jogo.getNumeroJogadores(); i++) ordem.append(' ').append(i + 1).append(". ").append(jogo.getNome(i));
            System.out.println(ordem);
            dispose();
            new JanelaTabuleiro(jogo);
        }
    }

    private class Painel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            for (int i = 0; i < herdeiros.size(); i++) {
                g2.drawImage(Imagens.cartela(herdeiros.get(i)), x(i), y(i), LARGURA, ALTURA, null);
                int ordem = escolhidos.indexOf(herdeiros.get(i));
                if (ordem >= 0 && !sortear.isSelected()) {
                    g2.setColor(Imagens.cor(herdeiros.get(i)));
                    g2.fillOval(x(i) + LARGURA - 60, y(i) + 10, 50, 50);
                    g2.setColor(Color.WHITE);
                    g2.drawString((ordem + 1) + "º", x(i) + LARGURA - 52, y(i) + 45);
                }
            }
        }
    }
}
