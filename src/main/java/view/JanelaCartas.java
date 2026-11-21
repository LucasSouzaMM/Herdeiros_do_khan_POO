package view;

import static model.ObservadoIF.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controller.Controlador;
import model.ObservadoIF;
import model.ObservadorIF;

// Janela de cartas (3ª iteração, Observer): as 4 cartas de conselheiro aberto, os 7 conselheiros com o que cada um
// pede e os tesouros do jogador da vez. Ela é um observador da partida: abre sozinha quando alguém ganha uma
// carta, quando o pião da vez chega a um conselheiro a quem pode entregar tesouros e quando a partida termina.
// Ela lê a partida só por get (contrato em ObservadoIF): os conselheiros uma vez, e o resto em cada notify.
@SuppressWarnings("serial")
class JanelaCartas extends JDialog implements ObservadorIF {
    private static final int MARGEM = 15;
    private static final int CARTA_L = 150, CARTA_A = 235; // proporção das imagens das cartas
    private static final int L = MARGEM + 4 * (CARTA_L + MARGEM);
    private static final int CONSELHEIROS_Y = 345, LINHA = 36;

    private final Controlador ctrl = Controlador.getInstance();
    private final Painel painel = new Painel();
    private final List<JButton> entregar = new ArrayList<>();
    private String aviso; // erro da última ação desta janela (não é estado da partida)

    // Grupo 1, lido uma vez.
    private final Map<String, List<String>> pedidos;  // parada do conselheiro → tesouros que ele pede
    private final Map<String, Integer> votosPorEntrega; // parada do conselheiro → votos
    private final List<String> conselheiros;           // as chaves de pedidos, na ordem do tabuleiro
    private final List<String> tiposTesouro;
    // Lidos em cada notify.
    private List<String> nomes;
    private int atual;
    private Map<String, String> cartas;       // carta → descrição, na ordem da mesa
    private Map<String, Integer> donosCartas; // carta → jogador, ou -1
    private List<String> possiveis;           // conselheiros a quem o jogador da vez pode entregar agora
    private Map<String, Integer> tesouros;    // tesouros do jogador da vez
    private int votos, votosNoConselho, limiteVotos;

    JanelaCartas(JFrame dono) {
        super(dono, "Cartas e conselheiros", false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        ObservadoIF observado = ctrl.registra(this);
        pedidos = observado.get(PEDIDOS_CONSELHEIROS);
        votosPorEntrega = observado.get(VOTOS_CONSELHEIROS);
        conselheiros = List.copyOf(pedidos.keySet());
        tiposTesouro = observado.get(TIPOS_TESOURO);
        painel.setLayout(null);
        painel.setPreferredSize(new Dimension(L, CONSELHEIROS_Y + conselheiros.size() * LINHA + 110));
        for (int i = 0; i < conselheiros.size(); i++) {
            JButton b = new JButton("Entregar");
            b.setBounds(L - MARGEM - 110, CONSELHEIROS_Y + i * LINHA + 4, 110, LINHA - 8);
            b.addActionListener(new Entregar(conselheiros.get(i)));
            entregar.add(b);
            painel.add(b);
        }
        add(painel);
        pack();
        notify(observado); // decisão A: a primeira leitura, logo depois de se inscrever
    }

    // Observer: a janela busca com get os dados que desenha e decide se abre ou só se atualiza.
    @Override
    public void notify(ObservadoIF observado) {
        nomes = observado.get(NOMES);
        atual = observado.get(JOGADOR_ATUAL);
        cartas = observado.get(CARTAS_ABERTAS);
        donosCartas = observado.get(DONOS_CARTAS);
        possiveis = observado.get(CONSELHEIROS_PARA_ENTREGA);
        tesouros = observado.get(TESOUROS).get(atual);
        votos = observado.get(VOTOS).get(atual);
        votosNoConselho = observado.get(VOTOS_NO_CONSELHO);
        limiteVotos = observado.get(LIMITE_VOTOS);
        Evento evento = observado.get(EVENTO);
        if (evento != Evento.SELECAO) aviso = null;
        boolean chegouAoConselheiro = evento == Evento.MOVIMENTO
                && possiveis.contains(observado.get(POSICOES).get(atual));
        if (evento == Evento.CARTA || evento == Evento.FIM || chegouAoConselheiro) abrir();
        else atualizar();
    }

    void abrir() {
        atualizar();
        if (!isVisible()) {
            java.awt.Window dono = getOwner();
            setLocation(dono.getX() + 40, dono.getY() + 60); // à esquerda, sobre a Rússia e a Pérsia
        }
        setVisible(true);
        toFront();
    }

    private void atualizar() {
        for (int i = 0; i < conselheiros.size(); i++) entregar.get(i).setEnabled(possiveis.contains(conselheiros.get(i)));
        painel.repaint();
    }

    private class Entregar implements ActionListener {
        private final String parada;

        Entregar(String parada) {
            this.parada = parada;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            aviso = ctrl.entregarTesouros(parada);
            if (aviso != null) painel.repaint(); // sem erro, o notify já redesenhou
        }
    }

    private class Painel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            Desenho.suavizar(g2);
            g2.setColor(new Color(70, 45, 90));
            g2.fillRect(0, 0, getWidth(), getHeight());
            desenharCartas(g2);
            desenharConselheiros(g2);
            desenharReserva(g2);
        }

        // Regras, p. 18: as 4 cartas da partida. A carta já ganha fica escura, com o nome de quem a cumpriu.
        private void desenharCartas(Graphics2D g2) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            Desenho.texto(g2, "Cartas de conselheiro aberto (1 voto para o primeiro)", MARGEM, 26);
            int i = 0;
            for (Map.Entry<String, String> entrada : cartas.entrySet()) {
                String carta = entrada.getKey();
                int x = MARGEM + i * (CARTA_L + MARGEM), y = 38;
                g2.drawImage(Imagens.carta(carta), x, y, CARTA_L, CARTA_A, null);
                int dono = donosCartas.get(carta);
                if (dono != -1) {
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.fillRect(x, y, CARTA_L, CARTA_A);
                    g2.setColor(Imagens.cor(nomes.get(dono)));
                    g2.fillRect(x, y + CARTA_A / 2 - 16, CARTA_L, 32);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                    Desenho.texto(g2, "Ganha por " + nomes.get(dono), x + 8, y + CARTA_A / 2 + 5);
                }
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                linhas(g2, entrada.getValue(), x, y + CARTA_A + 16, CARTA_L);
                i++;
            }
        }

        private void linhas(Graphics2D g2, String s, int x, int y, int largura) {
            StringBuilder linha = new StringBuilder();
            for (String palavra : s.split(" ")) {
                if (linha.length() > 0 && g2.getFontMetrics().stringWidth(linha + " " + palavra) > largura) {
                    Desenho.texto(g2, linha.toString(), x, y);
                    y += 15;
                    linha.setLength(0);
                }
                if (linha.length() > 0) linha.append(' ');
                linha.append(palavra);
            }
            Desenho.texto(g2, linha.toString(), x, y);
        }

        // Regras, p. 12 e 13: cada conselheiro, os 3 tesouros que pede e os votos de cada entrega.
        private void desenharConselheiros(Graphics2D g2) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            Desenho.texto(g2, "Conselheiros (entregue na parada onde esteve neste turno)", MARGEM, CONSELHEIROS_Y - 12);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            for (int i = 0; i < conselheiros.size(); i++) {
                String c = conselheiros.get(i);
                int y = CONSELHEIROS_Y + i * LINHA, meio = y + LINHA / 2;
                g2.setColor(new Color(255, 255, 255, i % 2 == 0 ? 25 : 10));
                g2.fillRect(MARGEM - 5, y, L - 2 * MARGEM + 10, LINHA);
                Desenho.texto(g2, c.equals("karakorum") ? "Karakorum" : "Parada " + c, MARGEM, meio + 5);
                List<String> pedido = pedidos.get(c);
                for (int k = 0; k < pedido.size(); k++) {
                    int cx = MARGEM + 150 + k * 32;
                    if (pedido.get(k).equals("qualquer")) qualquer(g2, cx, meio);
                    else Desenho.peca(g2, Imagens.tesouro(pedido.get(k)), cx, meio, 28, new Color(60, 40, 20));
                }
                int v = votosPorEntrega.get(c);
                Desenho.texto(g2, "=  " + v + (v == 1 ? " voto" : " votos"), MARGEM + 260, meio + 5);
            }
        }

        // "Qualquer tesouro": não há peça para isso, então é um círculo com ponto de interrogação.
        private void qualquer(Graphics2D g2, int cx, int cy) {
            g2.setColor(new Color(200, 170, 110));
            g2.fillOval(cx - 14, cy - 14, 28, 28);
            g2.setColor(new Color(60, 40, 20));
            g2.drawOval(cx - 14, cy - 14, 28, 28);
            g2.drawString("?", cx - 4, cy + 6);
        }

        // Tesouros e votos do jogador da vez, a trilha do Kurultai e o aviso de erro.
        private void desenharReserva(Graphics2D g2) {
            int y = CONSELHEIROS_Y + conselheiros.size() * LINHA + 30;
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            Desenho.texto(g2, "Tesouros de " + nomes.get(atual) + ":", MARGEM, y);
            int x = MARGEM + 190;
            for (String t : tiposTesouro)
                x = Desenho.contador(g2, Imagens.tesouro(t), tesouros.get(t), x, y);
            Desenho.texto(g2, "Votos: " + votos + "   Conselho: " + votosNoConselho + " de " + limiteVotos, MARGEM, y + 26);
            if (aviso != null) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                Desenho.aviso(g2, aviso, MARGEM, y + 36, L - 2 * MARGEM);
            }
        }
    }
}
