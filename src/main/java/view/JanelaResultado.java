package view;

import static model.ObservadoIF.*;

import java.awt.BasicStroke;
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
import javax.swing.JPanel;

import controller.Controlador;
import model.ObservadoIF;
import model.ObservadorIF;

// Resultado da partida (regras, p. 14 a 16): para cada jogador, os votos da trilha do Kurultai, da influência em
// cada região e da carta secreta, o total e os critérios de desempate. A janela é um observador: abre sozinha
// quando a última rodada acaba (evento FIM).
// Depois do vencedor, ela pergunta se os jogadores querem uma nova partida (enunciado da 4ª iteração): "Sim" fecha
// o tabuleiro e abre a escolha dos herdeiros; "Não" encerra o programa. O X da janela não faz nada: é preciso
// responder. Ela não é modal, então dá para olhar o tabuleiro antes de responder.
// Ela lê a partida só por get (contrato em ObservadoIF): os nomes das regiões e das cartas uma vez, e o resto em
// cada notify.
@SuppressWarnings("serial")
class JanelaResultado extends JDialog implements ObservadorIF {
    private static final int MARGEM = 20, TOPO = 96, LINHA = 64;
    private static final int[] X = {MARGEM, 200, 290, 400, 510, 620, 790, 880}; // início de cada coluna da tabela
    private static final int L = 1030;

    private final JanelaTabuleiro tabuleiro;
    private final Painel painel = new Painel();
    private final JButton sim = new JButton("Sim");
    private final JButton nao = new JButton("Não");

    // Grupo 1, lido uma vez.
    private final List<String> regioes;
    private final Map<String, String> nomesSecretas;
    // Lidos em cada notify. As listas por jogador têm o jogador como índice.
    private List<String> nomes, secretas;
    private List<Integer> vencedores, votos, votosSecretas, medidasSecretas, totais, distancias;
    private List<List<Integer>> influencia, yurtsNasRegioes;
    private List<List<List<String>>> melhorias;
    private List<Map<String, Integer>> tesouros;
    private boolean terminada;

    JanelaResultado(JanelaTabuleiro dono) {
        super(dono, "Resultado da partida", false);
        tabuleiro = dono;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        painel.setLayout(null);
        sim.addActionListener(new NovaPartida());
        painel.add(sim);
        nao.addActionListener(new Sair());
        painel.add(nao);
        add(painel);
        ObservadoIF observado = Controlador.getInstance().registra(this);
        regioes = observado.get(NOMES_REGIOES);
        nomesSecretas = observado.get(NOMES_CARTAS_SECRETAS);
        notify(observado); // decisão A: a primeira leitura, logo depois de se inscrever
        if (terminada) abrir(); // uma partida carregada que já tinha terminado
    }

    @Override
    public void notify(ObservadoIF observado) {
        nomes = observado.get(NOMES);
        vencedores = observado.get(VENCEDORES);
        votos = observado.get(VOTOS);
        influencia = observado.get(VOTOS_INFLUENCIA);
        yurtsNasRegioes = observado.get(YURTS_NAS_REGIOES);
        melhorias = observado.get(MELHORIAS);
        secretas = observado.get(CARTAS_SECRETAS);
        votosSecretas = observado.get(VOTOS_SECRETAS);
        medidasSecretas = observado.get(MEDIDAS_SECRETAS);
        totais = observado.get(TOTAIS_FINAIS);
        tesouros = observado.get(TESOUROS);
        distancias = observado.get(DISTANCIAS_KARAKORUM);
        terminada = observado.get(PARTIDA_TERMINADA);
        Evento evento = observado.get(EVENTO);
        if (evento == Evento.FIM) abrir();
        else if (evento == Evento.PARTIDA) setVisible(false);
        else painel.repaint();
    }

    void abrir() {
        int altura = TOPO + nomes.size() * LINHA + 110;
        painel.setPreferredSize(new Dimension(L, altura));
        sim.setBounds(L - MARGEM - 250, altura - 50, 120, 34);
        nao.setBounds(L - MARGEM - 120, altura - 50, 120, 34);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        toFront();
    }

    private static String plural(int n, String um, String varios) {
        return n + " " + (n == 1 ? um : varios);
    }

    // Sim: o tabuleiro e as suas janelas saem (esta também), e a escolha dos herdeiros abre para a partida nova.
    private class NovaPartida implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            tabuleiro.fechar();
            new JanelaHerdeiros();
        }
    }

    private class Sair implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
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
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            StringBuilder quem = new StringBuilder();
            for (int v : vencedores) quem.append(quem.length() > 0 ? " e " : "").append(nomes.get(v));
            Desenho.texto(g2, (vencedores.size() > 1 ? "Vencedores: " : "Novo Khan: ") + quem, MARGEM, 40);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            String[] titulos = {"Jogador", "Trilha", regioes.get(0), regioes.get(1), regioes.get(2), "Carta secreta",
                    "Total", "Desempate"};
            for (int k = 0; k < titulos.length; k++) Desenho.texto(g2, titulos[k], X[k], TOPO - 14);

            for (int i = 0; i < nomes.size(); i++) {
                int y = TOPO + i * LINHA, meio = y + LINHA / 2;
                String nome = nomes.get(i);
                g2.setColor(new Color(255, 255, 255, i % 2 == 0 ? 30 : 12));
                g2.fillRect(MARGEM - 8, y, L - 2 * MARGEM + 16, LINHA);
                if (vencedores.contains(i)) {
                    g2.setStroke(new BasicStroke(3));
                    g2.setColor(new Color(255, 215, 0));
                    g2.drawRect(MARGEM - 8, y + 1, L - 2 * MARGEM + 16, LINHA - 2);
                    g2.setStroke(new BasicStroke(1));
                }
                Desenho.peca(g2, Imagens.peao(nome), X[0] + 16, meio, 30, Imagens.cor(nome));
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                Desenho.texto(g2, nome, X[0] + 40, meio + 6);
                Desenho.texto(g2, String.valueOf(votos.get(i)), X[1] + 16, meio + 6);
                for (int c = 0; c < 3; c++) {
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                    Desenho.texto(g2, String.valueOf(influencia.get(i).get(c)), X[2 + c] + 16, meio);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                    Desenho.texto(g2, plural(yurtsNasRegioes.get(i).get(c), "yurt", "yurts") + ", "
                            + melhorias.get(i).get(c).size() + " melh.", X[2 + c], meio + 18);
                }
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                Desenho.texto(g2, String.valueOf(votosSecretas.get(i)), X[5] + 16, meio);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                if (secretas.get(i) != null)
                    Desenho.texto(g2, nomesSecretas.get(secretas.get(i)) + ": " + medidasSecretas.get(i), X[5], meio + 18);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
                Desenho.texto(g2, String.valueOf(totais.get(i)), X[6] + 14, meio + 8);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                int naoEntregues = 0; // o 1º desempate: os tesouros que o jogador ainda tem
                for (int n : tesouros.get(i).values()) naoEntregues += n;
                Desenho.texto(g2, plural(naoEntregues, "tesouro", "tesouros"), X[7], meio - 2);
                Desenho.texto(g2, plural(distancias.get(i), "passo", "passos") + " de Karakorum", X[7], meio + 14);
            }
            int y = TOPO + nomes.size() * LINHA + 26;
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            Desenho.texto(g2, "Influência: mais yurts na região e 2º lugar ganham votos pelas melhorias na coluna da mesma cor.",
                    MARGEM, y);
            Desenho.texto(g2, "Desempate: mais tesouros não entregues; depois, o pião mais perto de Karakorum.", MARGEM,
                    y + 18);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            Desenho.texto(g2, "Querem jogar uma nova partida?", MARGEM, getHeight() - 27);
        }
    }
}
