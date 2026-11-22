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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controller.Controlador;
import model.ObservadoIF;
import model.ObservadorIF;

// Janela de cartas (Observer): as 4 cartas de conselheiro aberto, os 7 conselheiros com o que cada um pede e a sua
// peça de bônus, e, do jogador da vez, os tesouros, a carta secreta, as peças de bônus (com o botão "Usar") e o
// baú aberto. Ela é um observador da partida: abre sozinha quando alguém ganha uma carta, quando o pião da vez
// chega a um conselheiro a quem pode entregar tesouros, quando alguém ganha ou usa uma peça de bônus e quando a
// partida termina.
// Ela lê a partida só por get (contrato em ObservadoIF): os dados fixos uma vez, e o resto em cada notify.
@SuppressWarnings("serial")
class JanelaCartas extends JDialog implements ObservadorIF {
    private static final int MARGEM = 15;
    private static final int CARTA_L = 150, CARTA_A = 235; // proporção das imagens das cartas
    private static final int L = MARGEM + 4 * (CARTA_L + MARGEM);
    private static final int CONSELHEIROS_Y = 345, LINHA = 36;

    private final Controlador ctrl = Controlador.getInstance();
    private final Painel painel = new Painel();
    private final List<JButton> entregar = new ArrayList<>();
    private final Map<String, JButton> usar = new LinkedHashMap<>(); // um botão por peça de bônus
    private final List<JButton> bau = new ArrayList<>(); // os 3 tesouros do baú aberto
    private final int fimConselheiros; // y logo abaixo da lista de conselheiros
    private String aviso; // erro da última ação desta janela (não é estado da partida)

    // Grupo 1, lido uma vez.
    private final Map<String, List<String>> pedidos;  // parada do conselheiro → tesouros que ele pede
    private final Map<String, Integer> votosPorEntrega; // parada do conselheiro → votos
    private final List<String> conselheiros;           // as chaves de pedidos, na ordem do tabuleiro
    private final List<String> tiposTesouro;
    private final Map<String, String> nomesSecretas;   // carta secreta → nome
    // Lidos em cada notify.
    private List<String> nomes;
    private int atual;
    private Map<String, String> cartas;       // carta → descrição, na ordem da mesa
    private Map<String, Integer> donosCartas; // carta → jogador, ou -1
    private List<String> possiveis;           // conselheiros a quem o jogador da vez pode entregar agora
    private Map<String, Integer> tesouros;    // tesouros do jogador da vez
    private int votos, votosNoConselho, limiteVotos;
    private Map<String, String> bonusNosConselheiros; // conselheiro → peça de bônus ainda sobre ele, ou null
    private List<String> pecas, pecasUsadas, pecasUsaveis; // peças de bônus do jogador da vez
    private List<String> tesourosBau;
    private String secreta;                   // carta secreta do jogador da vez, ou null
    private int medidaSecreta, votosSecreta;

    JanelaCartas(JFrame dono) {
        super(dono, "Cartas e conselheiros", false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        ObservadoIF observado = ctrl.registra(this);
        pedidos = observado.get(PEDIDOS_CONSELHEIROS);
        votosPorEntrega = observado.get(VOTOS_CONSELHEIROS);
        conselheiros = List.copyOf(pedidos.keySet());
        tiposTesouro = observado.get(TIPOS_TESOURO);
        nomesSecretas = observado.get(NOMES_CARTAS_SECRETAS);
        painel.setLayout(null);
        fimConselheiros = CONSELHEIROS_Y + conselheiros.size() * LINHA;
        painel.setPreferredSize(new Dimension(L, fimConselheiros + 300));
        for (int i = 0; i < conselheiros.size(); i++) {
            JButton b = new JButton("Entregar");
            b.setBounds(L - MARGEM - 110, CONSELHEIROS_Y + i * LINHA + 4, 110, LINHA - 8);
            b.addActionListener(new Entregar(conselheiros.get(i)));
            entregar.add(b);
            painel.add(b);
        }
        for (String peca : observado.get(DESCRICOES_BONUS).keySet()) {
            JButton b = new JButton("Usar");
            b.addActionListener(new Usar(peca));
            b.setVisible(false);
            usar.put(peca, b);
            painel.add(b);
        }
        for (int k = 0; k < 3; k++) {
            JButton b = new JButton();
            b.setBounds(232 + k * 150, fimConselheiros + 196, 120, 28);
            b.addActionListener(new EscolherBau(k));
            b.setVisible(false);
            bau.add(b);
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
        bonusNosConselheiros = observado.get(BONUS_NOS_CONSELHEIROS);
        pecas = observado.get(BONUS).get(atual);
        pecasUsadas = observado.get(BONUS_USADOS).get(atual);
        pecasUsaveis = observado.get(BONUS_USAVEIS);
        tesourosBau = observado.get(TESOUROS_BAU);
        secreta = observado.get(CARTAS_SECRETAS).get(atual);
        medidaSecreta = observado.get(MEDIDAS_SECRETAS).get(atual);
        votosSecreta = observado.get(VOTOS_SECRETAS).get(atual);
        Evento evento = observado.get(EVENTO);
        if (evento != Evento.SELECAO) aviso = null;
        boolean chegouAoConselheiro = evento == Evento.MOVIMENTO
                && possiveis.contains(observado.get(POSICOES).get(atual));
        if (evento == Evento.CARTA || evento == Evento.FIM || evento == Evento.BONUS_GANHO
                || evento == Evento.BONUS_USADO || evento == Evento.BAU || chegouAoConselheiro) abrir();
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
        // Botão "Usar" embaixo de cada peça de bônus do jogador da vez que ainda não foi usada.
        for (Map.Entry<String, JButton> e : usar.entrySet()) {
            int k = pecas.indexOf(e.getKey());
            boolean mostrar = k != -1 && !pecasUsadas.contains(e.getKey());
            e.getValue().setVisible(mostrar);
            if (mostrar) {
                e.getValue().setBounds(xBonus(k) - 40, fimConselheiros + 148, 80, 24);
                e.getValue().setEnabled(pecasUsaveis.contains(e.getKey()));
            }
        }
        for (int k = 0; k < bau.size(); k++) {
            bau.get(k).setVisible(k < tesourosBau.size());
            if (k < tesourosBau.size()) bau.get(k).setText(tesourosBau.get(k));
        }
        painel.repaint();
    }

    // Centro da k-ésima peça de bônus do jogador da vez.
    private static int xBonus(int k) {
        return MARGEM + 46 + k * 92;
    }

    private class Usar implements ActionListener {
        private final String peca;

        Usar(String peca) {
            this.peca = peca;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            aviso = ctrl.usarBonus(peca);
            if (aviso != null) painel.repaint(); // sem erro, o notify já redesenhou
        }
    }

    private class EscolherBau implements ActionListener {
        private final int indice;

        EscolherBau(int indice) {
            this.indice = indice;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (indice >= tesourosBau.size()) return;
            aviso = ctrl.escolherTesouroBau(tesourosBau.get(indice));
            if (aviso != null) painel.repaint(); // sem erro, o notify já redesenhou
        }
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
                if (Imagens.temCarta(carta)) g2.drawImage(Imagens.carta(carta), x, y, CARTA_L, CARTA_A, null);
                else Desenho.cartaSemImagem(g2, entrada.getValue(), x, y, CARTA_L, CARTA_A);
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

        // Regras, p. 12 e 13: cada conselheiro, os 3 tesouros que pede, os votos de cada entrega e a peça de bônus
        // que ainda está sobre ele (do primeiro a entregar ali).
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
                String peca = bonusNosConselheiros.get(c);
                if (peca != null) {
                    Desenho.texto(g2, "+", MARGEM + 352, meio + 5);
                    Desenho.bonus(g2, peca, MARGEM + 384, meio, LINHA - 6, false);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    Desenho.texto(g2, "bônus", MARGEM + 404, meio + 5);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                }
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

        // Do jogador da vez: tesouros, votos, a carta secreta, as peças de bônus e o baú aberto; e o aviso de erro.
        private void desenharReserva(Graphics2D g2) {
            int y = fimConselheiros + 30;
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            Desenho.texto(g2, "Tesouros de " + nomes.get(atual) + ":", MARGEM, y);
            int x = MARGEM + 190;
            for (String t : tiposTesouro)
                x = Desenho.contador(g2, Imagens.tesouro(t), tesouros.get(t), x, y);
            Desenho.texto(g2, "Votos: " + votos + "   Conselho: " + votosNoConselho + " de " + limiteVotos, MARGEM, y + 26);
            // A carta secreta só do jogador da vez, com quanto ela daria se a partida acabasse agora.
            if (secreta != null)
                Desenho.texto(g2, "Carta secreta: " + nomesSecretas.get(secreta) + " (" + medidaSecreta + " agora = "
                        + votosSecreta + (votosSecreta == 1 ? " voto)" : " votos)"), MARGEM + 300, y + 26);

            // Regras, p. 17: as peças de bônus do jogador da vez; as usadas ficam escuras.
            int topo = fimConselheiros + 82;
            Desenho.texto(g2, "Peças de bônus de " + nomes.get(atual) + (pecas.isEmpty() ? ": nenhuma" : ":"), MARGEM, topo);
            for (int k = 0; k < pecas.size(); k++)
                Desenho.bonus(g2, pecas.get(k), xBonus(k), topo + 34, 44, pecasUsadas.contains(pecas.get(k)));
            if (!tesourosBau.isEmpty()) {
                Desenho.texto(g2, "Baú: fique com 1", MARGEM, fimConselheiros + 216);
                for (int k = 0; k < tesourosBau.size(); k++)
                    Desenho.peca(g2, Imagens.tesouro(tesourosBau.get(k)), 215 + k * 150, fimConselheiros + 210, 24,
                            new Color(60, 40, 20));
            }
            if (aviso != null) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                Desenho.aviso(g2, aviso, MARGEM, fimConselheiros + 236, L - 2 * MARGEM);
            }
        }
    }
}
