package view;

import static model.ObservadoIF.*;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controller.Controlador;
import model.ObservadoIF;
import model.ObservadorIF;

// Janela da cartela com as colunas (3ª iteração, Observer). Ela é um observador da partida:
// abre sozinha quando começa o turno de um jogador, para ele escolher a coluna, e fecha quando a coluna é ativada.
// Antes da primeira rodada, ela espera todos escolherem a carta secreta (regras, p. 6).
// Ela lê a partida só por get (contrato em ObservadoIF): a coluna branca uma vez, e o resto em cada notify.
@SuppressWarnings("serial")
class JanelaCartela extends JDialog implements ObservadorIF {
    private static final int L = 720, A = (int) Math.round(L * 229.0 / 420); // proporção da imagem da cartela
    private static final int MARGEM = 12, TOPO = 44;

    private final Controlador ctrl = Controlador.getInstance();
    private final Painel painel = new Painel();
    private final JButton confirmar = new JButton("Confirmar coluna");
    private String aviso; // erro da última ação desta janela (não é estado da partida)

    // Lidos com get: o grupo 1 uma vez, o resto em cada notify.
    private final int colunaReativacao;
    private String nome;               // herdeiro do jogador da vez
    private List<Integer> bloqueadas;  // colunas bloqueadas do jogador da vez
    private List<List<String>> melhorias; // melhorias de cada coluna do jogador da vez
    private int escolhendoSecreta;     // jogador que escolhe a carta secreta, ou -1
    private int ativada, selecionada;  // -1 quando não há

    JanelaCartela(JFrame dono) {
        super(dono, "Cartela", false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        painel.setLayout(null);
        painel.setPreferredSize(new Dimension(L + 2 * MARGEM, TOPO + A + 92));
        painel.addMouseListener(new Clique());
        confirmar.setBounds(L + MARGEM - 200, TOPO + A + 34, 200, 40);
        confirmar.addActionListener(new Confirmar());
        painel.add(confirmar);
        add(painel);
        pack();
        ObservadoIF observado = ctrl.registra(this);
        colunaReativacao = observado.get(COLUNA_REATIVACAO);
        notify(observado); // decisão A: a primeira leitura, logo depois de se inscrever
    }

    // Observer: a janela busca com get os dados que desenha e decide se abre, fecha ou só se redesenha.
    @Override
    public void notify(ObservadoIF observado) {
        int atual = observado.get(JOGADOR_ATUAL);
        nome = observado.get(NOMES).get(atual);
        bloqueadas = observado.get(COLUNAS_BLOQUEADAS).get(atual);
        melhorias = observado.get(MELHORIAS).get(atual);
        escolhendoSecreta = observado.get(JOGADOR_ESCOLHENDO_SECRETA);
        ativada = observado.get(COLUNA_ATIVADA);
        selecionada = observado.get(COLUNA_SELECIONADA);
        Evento evento = observado.get(EVENTO);
        if (evento == Evento.TURNO || evento == Evento.PARTIDA || evento == Evento.SECRETA) abrir();
        else if (evento == Evento.COLUNA) setVisible(false);
        else atualizar();
    }

    void abrir() {
        if (escolhendoSecreta != -1) return; // a cartela só abre depois que todos escolheram a carta secreta
        aviso = null;
        atualizar();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        toFront();
    }

    private void atualizar() {
        setTitle("Cartela de " + nome);
        confirmar.setEnabled(ativada == -1 && selecionada != -1);
        painel.repaint();
    }

    private class Clique extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int coluna = Desenho.colunaEm(e.getX(), e.getY(), MARGEM, TOPO, L, A);
            if (coluna == -1) return;
            aviso = ctrl.selecionarColuna(coluna);
            if (aviso != null) painel.repaint(); // sem erro, o notify já redesenhou
        }
    }

    private class Confirmar implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            aviso = ctrl.confirmarColuna();
            if (aviso != null) painel.repaint(); // sem erro, o notify já fechou a janela
        }
    }

    private class Painel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            Desenho.suavizar(g2);
            g2.setColor(Imagens.cor(nome));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            String titulo = ativada != -1 ? nome + " já ativou a coluna " + (ativada + 1)
                    : nome + ": clique numa coluna livre e confirme";
            Desenho.texto(g2, titulo, MARGEM, 30);
            Desenho.cartela(g2, nome, melhorias, bloqueadas, ativada, selecionada, MARGEM, TOPO, L, A);

            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            Desenho.texto(g2, "A coluna branca (" + (colunaReativacao + 1)
                    + ") devolve as peças de ativação e libera as outras colunas.", MARGEM, TOPO + A + 22);
            if (aviso != null) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                Desenho.aviso(g2, aviso, MARGEM, TOPO + A + 30, L - 215);
            }
        }
    }
}
