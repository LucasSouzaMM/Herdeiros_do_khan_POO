package view;

import static model.ObservadoIF.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controller.Controlador;
import model.ObservadoIF;
import model.ObservadorIF;

// Escolha das cartas de conselheiro secreto (regras, p. 6), antes da primeira rodada. Cada jogador, na ordem da
// partida, vê as suas 2 cartas e fica com 1. Para os outros não verem, as cartas só aparecem depois que o jogador
// clica em "Mostrar as cartas de ...". A janela é um observador: passa para o próximo jogador a cada escolha e
// fecha quando todos escolheram (então a cartela do primeiro jogador abre).
// Ela lê a partida só por get (contrato em ObservadoIF): as cartas uma vez, e quem escolhe em cada notify.
@SuppressWarnings("serial")
class JanelaSecretas extends JDialog implements ObservadorIF {
    private static final int MARGEM = 20, CARTA_L = 220, CARTA_A = 300;
    private static final int L = 3 * MARGEM + 2 * CARTA_L, TOPO = 70;

    private final Controlador ctrl = Controlador.getInstance();
    private final Painel painel = new Painel();
    private final JButton mostrar = new JButton();
    private final JButton[] ficar = new JButton[2];
    private boolean reveladas; // as cartas do jogador que está escolhendo estão à vista (estado da janela)
    private String aviso;      // erro da última ação desta janela (não é estado da partida)

    // Grupo 1, lido uma vez.
    private final Map<String, String> nomesCartas, objetivos;
    private final Map<String, int[]> faixas;
    // Lidos em cada notify.
    private int quem;             // jogador que escolhe agora, ou -1
    private String nome;          // herdeiro de quem escolhe
    private List<String> opcoes;  // as 2 cartas de quem escolhe

    JanelaSecretas(JFrame dono) {
        super(dono, "Cartas secretas", false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // a partida só começa depois das escolhas
        setResizable(false);
        painel.setLayout(null);
        painel.setPreferredSize(new Dimension(L, TOPO + CARTA_A + 110));
        mostrar.setBounds(L / 2 - 140, TOPO + CARTA_A / 2 - 20, 280, 40);
        mostrar.addActionListener(new Mostrar());
        painel.add(mostrar);
        for (int k = 0; k < 2; k++) {
            ficar[k] = new JButton("Ficar com esta");
            ficar[k].setBounds(x(k) + CARTA_L / 2 - 80, TOPO + CARTA_A + 14, 160, 34);
            ficar[k].addActionListener(new Ficar(k));
            painel.add(ficar[k]);
        }
        add(painel);
        pack();
        ObservadoIF observado = ctrl.registra(this);
        nomesCartas = observado.get(NOMES_CARTAS_SECRETAS);
        objetivos = observado.get(OBJETIVOS_CARTAS_SECRETAS);
        faixas = observado.get(FAIXAS_CARTAS_SECRETAS);
        notify(observado); // decisão A: a primeira leitura, logo depois de se inscrever
    }

    private static int x(int k) {
        return MARGEM + k * (CARTA_L + MARGEM);
    }

    // Observer: a cada escolha, as cartas se escondem para o próximo jogador; quando todos escolheram, a janela fecha.
    @Override
    public void notify(ObservadoIF observado) {
        quem = observado.get(JOGADOR_ESCOLHENDO_SECRETA);
        if (quem != -1) {
            nome = observado.get(NOMES).get(quem);
            opcoes = observado.get(OPCOES_SECRETAS).get(quem);
        }
        Evento evento = observado.get(EVENTO);
        if (evento == Evento.SECRETA || evento == Evento.PARTIDA) {
            reveladas = false;
            aviso = null;
            abrir();
        }
    }

    private void abrir() {
        if (quem == -1) {
            setVisible(false);
            return;
        }
        setTitle("Carta secreta de " + nome);
        mostrar.setText("Mostrar as cartas de " + nome);
        mostrar.setVisible(!reveladas);
        for (JButton b : ficar) b.setVisible(reveladas);
        painel.repaint();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        toFront();
    }

    private class Mostrar implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            reveladas = true;
            abrir();
        }
    }

    private class Ficar implements ActionListener {
        private final int indice;

        Ficar(int indice) {
            this.indice = indice;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (quem == -1) return;
            aviso = ctrl.escolherCartaSecreta(opcoes.get(indice));
            if (aviso != null) painel.repaint(); // sem erro, o notify já passou para o próximo jogador
        }
    }

    private class Painel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (quem == -1) return;
            Graphics2D g2 = (Graphics2D) g;
            Desenho.suavizar(g2);
            g2.setColor(Imagens.cor(nome));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            Desenho.texto(g2, nome + ": fique com 1 das 2 cartas secretas", MARGEM, 32);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            Desenho.texto(g2, reveladas ? "No fim da partida, a carta dá de 0 a 3 votos. A outra sai do jogo."
                    : "Os outros jogadores não devem olhar.", MARGEM, 56);
            for (int k = 0; k < opcoes.size(); k++) {
                String carta = opcoes.get(k);
                if (reveladas) Desenho.cartaSecreta(g2, nomesCartas.get(carta), objetivos.get(carta), faixas.get(carta),
                        x(k), TOPO, CARTA_L, CARTA_A);
                else {
                    g2.setColor(new Color(60, 40, 80));
                    g2.fillRoundRect(x(k), TOPO, CARTA_L, CARTA_A, 16, 16);
                    g2.setColor(new Color(235, 205, 120));
                    g2.drawRoundRect(x(k) + 6, TOPO + 6, CARTA_L - 12, CARTA_A - 12, 12, 12);
                }
            }
            if (aviso != null) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                Desenho.aviso(g2, aviso, MARGEM, TOPO + CARTA_A + 56, L - 2 * MARGEM);
            }
        }
    }
}
