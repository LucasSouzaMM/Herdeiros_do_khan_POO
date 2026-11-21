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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controller.Controlador;
import model.ObservadoIF;
import model.ObservadorIF;

// RF08 a RF13: janela do tabuleiro. O tabuleiro fica à esquerda e o painel do jogador da vez à direita.
// O painel é um observador da partida (cap. 17): ele se registra pelo controlador, lê os dados fixos uma vez e,
// a cada aviso do Model, busca com get o que desenha (contrato em ObservadoIF). As ações do jogador vão para o
// controlador, nunca direto para o Model.
@SuppressWarnings("serial")
class JanelaTabuleiro extends JFrame {
    // As coordenadas do modelo são pixels da imagem original (2905 x 2919). Na tela, o tabuleiro tem 980 de altura.
    private static final double ESCALA = 980.0 / 2919;
    private static final int TAB_L = (int) Math.round(2905 * ESCALA), TAB_A = 980;
    private static final int PAINEL_L = 380;
    private static final int CARTELA_X = TAB_L + 10, CARTELA_Y = 80, CARTELA_L = 360, CARTELA_A = 196;
    private static final int PEAO = 30;
    private static final int TRIBUTO = 30; // diâmetro de uma peça de tributo na tela
    // Centro de cada casa da trilha do Kurultai (1 a 17 votos) na imagem original: a fileira de cima vai da esquerda
    // para a direita até o 10 (fim com 2 jogadores) e a de baixo volta até o 17 (3, 4 e 5 jogadores em 14, 16 e 17).
    private static final int[][] TRILHA = {
            {1143, 2629}, {1333, 2629}, {1524, 2629}, {1701, 2629}, {1885, 2629}, {2068, 2629}, {2259, 2629},
            {2443, 2629}, {2626, 2629}, {2824, 2629},
            {2722, 2793}, {2538, 2793}, {2347, 2793}, {2164, 2793}, {1980, 2793}, {1796, 2793}, {1612, 2793}};
    private static final int TESOURO = 26; // diâmetro de uma peça de tesouro na tela
    private static final int PECA_CIDADE = 40; // lado da peça de cidade na tela
    // Posição de cada peça de tributo em volta do ícone da província, para 1, 2 e 3 peças.
    private static final int[][][] DESLOCAMENTO = {
            {{0, 0}},
            {{-16, 0}, {16, 0}},
            {{-16, 9}, {16, 9}, {0, -18}}};
    private static final boolean DEBUG = Boolean.getBoolean("mapa.debug"); // G21: java -Dmapa.debug=true ...

    private final Controlador ctrl = Controlador.getInstance();
    private final Painel painel = new Painel();
    private final JButton proximo = new JButton("Próximo");
    private final JanelaCartela cartela;
    private final JanelaCartas cartas;
    private String aviso; // erro da última ação, desenhado no painel (sem caixas de diálogo); é estado da janela

    // Grupo 1 (dados fixos), lido uma vez depois de se inscrever. Os mapas guardam a ordem do tabuleiro.
    private final Map<String, int[]> pontosParadas;
    private final Map<String, Integer> raiosParadas;
    private final Map<String, List<String>> vizinhasParadas;      // modo de depuração
    private final Map<String, int[]> pontosProvincias;
    private final Map<String, String> tiposProvincias;
    private final Map<String, List<String>> adjacentesProvincias; // modo de depuração
    private final List<int[]> pontosKhan;
    private final List<List<String>> provinciasKhan;
    private final Map<String, int[]> pontosCidades;
    private final List<String> tiposTesouro;

    // Grupos 2 a 6, lidos em cada notify. As listas por jogador têm o jogador como índice.
    private List<String> nomes;
    private int atual;
    private List<Integer> moedas, espadas, pecasYurt, yurtsRestantes, votos;
    private List<Map<String, Integer>> tesouros;
    private List<String> posicoes;
    private List<Integer> bloqueadas; // do jogador da vez
    private int colunaAtivada, colunaSelecionada, movimentos, punhos;
    private boolean khanPendente;
    private List<String> visitadas, destinos, provinciasAlcancaveis, cidadesAtacaveis, paradasParaYurt,
            conselheirosParaEntrega;
    private Map<String, List<Integer>> yurtsNasParadas;
    private List<String> cidadesReveladas;
    private Map<String, List<String>> tesourosCidades;
    private Map<String, Integer> donosCidades, pecasProvincias;
    private int cidadesNaPilha, posicaoKhan, votosNoConselho, turnosRestantes;
    private boolean fimDisparado, partidaTerminada;
    private List<Integer> vencedores;

    JanelaTabuleiro() {
        super("Herdeiros do Khan");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        painel.setLayout(null);
        painel.setPreferredSize(new Dimension(TAB_L + PAINEL_L, TAB_A));
        painel.addMouseListener(new Clique());

        JButton verCartas = new JButton("Ver cartas e conselheiros");
        verCartas.setBounds(TAB_L + 20, TAB_A - 210, PAINEL_L - 40, 36);
        verCartas.addActionListener(new VerCartas());
        painel.add(verCartas);
        JButton verCartela = new JButton("Ver cartela");
        verCartela.setBounds(TAB_L + 20, TAB_A - 160, PAINEL_L - 40, 36);
        verCartela.addActionListener(new VerCartela());
        painel.add(verCartela);
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
        ObservadoIF observado = ctrl.registra(painel);
        pontosParadas = observado.get(PONTOS_PARADAS);
        raiosParadas = observado.get(RAIOS_PARADAS);
        vizinhasParadas = observado.get(VIZINHAS_PARADAS);
        pontosProvincias = observado.get(PONTOS_PROVINCIAS);
        tiposProvincias = observado.get(TIPOS_PROVINCIAS);
        adjacentesProvincias = observado.get(ADJACENTES_PROVINCIAS);
        pontosKhan = observado.get(PONTOS_KHAN);
        provinciasKhan = observado.get(PROVINCIAS_KHAN);
        pontosCidades = observado.get(PONTOS_CIDADES);
        tiposTesouro = observado.get(TIPOS_TESOURO);
        painel.notify(observado); // decisão A: a primeira leitura, logo depois de se inscrever
        setVisible(true);
        cartas = new JanelaCartas(this); // abre sozinha quando houver algo a mostrar
        cartela = new JanelaCartela(this); // abre por cima do tabuleiro para o primeiro jogador
    }

    private static int tela(int coordenada) {
        return (int) Math.round(coordenada * ESCALA);
    }

    // RF11, RF12: clique numa parada do tabuleiro (anda) ou na cartela do painel (abre a janela da cartela).
    private class Clique extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getX() < TAB_L) {
                String[] tesouro = tesouroEm(e.getX(), e.getY());
                if (javax.swing.SwingUtilities.isRightMouseButton(e))
                    aviso = ctrl.construirYurt((int) (e.getX() / ESCALA), (int) (e.getY() / ESCALA));
                else if (tesouro != null) aviso = ctrl.atacarCidade(tesouro[0], tesouro[1]);
                else aviso = ctrl.clicarTabuleiro((int) (e.getX() / ESCALA), (int) (e.getY() / ESCALA));
                painel.repaint(); // só pelo aviso, que é estado da janela; as mudanças da partida chegam pelo notify
            } else if (Desenho.colunaEm(e.getX(), e.getY(), CARTELA_X, CARTELA_Y, CARTELA_L, CARTELA_A) != -1) {
                cartela.abrir();
            }
        }
    }

    // Cidade e tipo do tesouro desenhado no ponto (x, y) da tela, ou null. A posição dos tesouros é da interface.
    private String[] tesouroEm(int x, int y) {
        for (String cidade : pontosCidades.keySet()) {
            if (!cidadesReveladas.contains(cidade) || donosCidades.get(cidade) != -1) continue;
            List<String> naCidade = tesourosCidades.get(cidade);
            for (int k = 0; k < naCidade.size(); k++) {
                int[] p = posicaoTesouro(cidade, k, naCidade.size());
                if ((x - p[0]) * (x - p[0]) + (y - p[1]) * (y - p[1]) <= TESOURO * TESOURO / 4)
                    return new String[] {cidade, naCidade.get(k)};
            }
        }
        return null;
    }

    // Os tesouros ficam numa fileira logo abaixo da peça da cidade.
    private int[] posicaoTesouro(String cidade, int k, int n) {
        int[] c = pontosCidades.get(cidade);
        return new int[] {tela(c[0]) + (int) Math.round((k - (n - 1) / 2.0) * (TESOURO + 3)), tela(c[1]) + 24};
    }

    private class VerCartas implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            cartas.abrir();
        }
    }

    private class VerCartela implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            cartela.abrir();
        }
    }

    // RF13, G16: o botão Próximo só funciona depois de ativar uma coluna.
    private class Proximo implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            aviso = ctrl.proximo();
            painel.repaint();
        }
    }

    // RF08
    private class Salvar implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser escolha = new JFileChooser(".");
            if (escolha.showSaveDialog(JanelaTabuleiro.this) != JFileChooser.APPROVE_OPTION) return;
            aviso = ctrl.salvar(escolha.getSelectedFile().toPath());
            painel.repaint();
        }
    }

    private class Painel extends JPanel implements ObservadorIF {
        // Observer: a cada mudança da partida, busca com get tudo o que desenha, apaga o aviso antigo e se redesenha.
        @Override
        public void notify(ObservadoIF observado) {
            nomes = observado.get(NOMES);
            atual = observado.get(JOGADOR_ATUAL);
            moedas = observado.get(MOEDAS);
            espadas = observado.get(ESPADAS);
            pecasYurt = observado.get(PECAS_YURT);
            yurtsRestantes = observado.get(YURTS_RESTANTES);
            tesouros = observado.get(TESOUROS);
            votos = observado.get(VOTOS);
            posicoes = observado.get(POSICOES);
            bloqueadas = observado.get(COLUNAS_BLOQUEADAS).get(atual);
            colunaAtivada = observado.get(COLUNA_ATIVADA);
            colunaSelecionada = observado.get(COLUNA_SELECIONADA);
            movimentos = observado.get(MOVIMENTOS);
            punhos = observado.get(PUNHOS);
            khanPendente = observado.get(KHAN_PENDENTE);
            visitadas = observado.get(VISITADAS);
            destinos = observado.get(DESTINOS);
            provinciasAlcancaveis = observado.get(PROVINCIAS_ALCANCAVEIS);
            cidadesAtacaveis = observado.get(CIDADES_ATACAVEIS);
            paradasParaYurt = observado.get(PARADAS_PARA_YURT);
            conselheirosParaEntrega = observado.get(CONSELHEIROS_PARA_ENTREGA);
            yurtsNasParadas = observado.get(YURTS_NAS_PARADAS);
            cidadesReveladas = observado.get(CIDADES_REVELADAS);
            tesourosCidades = observado.get(TESOUROS_CIDADES);
            donosCidades = observado.get(DONOS_CIDADES);
            cidadesNaPilha = observado.get(CIDADES_NA_PILHA);
            pecasProvincias = observado.get(PECAS_PROVINCIAS);
            posicaoKhan = observado.get(POSICAO_KHAN);
            votosNoConselho = observado.get(VOTOS_NO_CONSELHO);
            fimDisparado = observado.get(FIM_DISPARADO);
            turnosRestantes = observado.get(TURNOS_RESTANTES);
            partidaTerminada = observado.get(PARTIDA_TERMINADA);
            vencedores = observado.get(VENCEDORES);
            if (observado.get(EVENTO) != Evento.SELECAO) aviso = null;
            proximo.setEnabled(colunaAtivada != -1);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            Desenho.suavizar(g2);
            g2.drawImage(Imagens.tabuleiro(), 0, 0, TAB_L, TAB_A, null);
            if (DEBUG) desenharMapa(g2);
            desenharProvincias(g2);
            desenharCidades(g2);
            desenharYurts(g2);
            desenharCaminho(g2);
            desenharKhan(g2);
            desenharPioes(g2);
            desenharTrilha(g2);
            desenharPainel(g2);
        }

        // Regras, p. 14: trilha do Kurultai. A ficha de cada jogador fica na casa dos seus votos e a ficha neutra
        // (branca) na casa dos votos de todos somados. As fichas não têm imagem: são círculos na cor do jogador.
        // Conselheiros com anel roxo: o jogador da vez pode entregar tesouros lá (pela janela de cartas).
        private void desenharTrilha(Graphics2D g2) {
            g2.setStroke(new BasicStroke(4));
            g2.setColor(new Color(190, 60, 255));
            for (String c : conselheirosParaEntrega) anel(g2, c, 14);
            g2.setStroke(new BasicStroke(1));

            Map<Integer, Integer> naCasa = new java.util.HashMap<>();
            ficha(g2, votosNoConselho, Color.WHITE, naCasa);
            for (int i = 0; i < nomes.size(); i++)
                ficha(g2, votos.get(i), Imagens.cor(nomes.get(i)), naCasa);

            if (partidaTerminada) {
                List<String> quem = new ArrayList<>();
                for (int v : vencedores) quem.add(nomes.get(v));
                String texto = "Fim de jogo! Vencedor" + (quem.size() > 1 ? "es" : "") + ": " + String.join(", ", quem)
                        + " (" + votos.get(vencedores.get(0)) + " votos)";
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
                int largura = g2.getFontMetrics().stringWidth(texto) + 40;
                g2.setColor(new Color(0, 0, 0, 190));
                g2.fillRoundRect((TAB_L - largura) / 2, TAB_A / 2 - 40, largura, 70, 20, 20);
                Desenho.texto(g2, texto, (TAB_L - largura) / 2 + 20, TAB_A / 2 + 5);
            }
        }

        private void ficha(Graphics2D g2, int votos, Color cor, Map<Integer, Integer> naCasa) {
            if (votos <= 0) return; // com 0 votos, a ficha fica no assento ao lado da trilha
            int casa = Math.min(votos, TRILHA.length);
            int k = naCasa.merge(casa, 1, Integer::sum) - 1; // fichas na mesma casa ficam em leque
            int cx = tela(TRILHA[casa - 1][0]) - 12 + (k % 3) * 12, cy = tela(TRILHA[casa - 1][1]) - 8 + (k / 3) * 12;
            g2.setColor(cor);
            g2.fillOval(cx - 9, cy - 9, 18, 18);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(cx - 9, cy - 9, 18, 18);
            g2.setStroke(new BasicStroke(1));
        }

        // Regras, p. 11: os yurts construídos ficam no canto de cima do medalhão da parada, na cor do dono (um em cada
        // medalhão numa parada dupla). Anel amarelo: parada onde o jogador da vez pode construir um yurt agora.
        private void desenharYurts(Graphics2D g2) {
            for (String id : pontosParadas.keySet()) {
                int[] p = pontosParadas.get(id);
                List<Integer> donos = yurtsNasParadas.get(id);
                for (int k = 0; k < donos.size(); k++) {
                    int m = 2 * Math.min(k, p.length / 2 - 1);
                    Desenho.yurt(g2, tela(p[m]) + 19, tela(p[m + 1]) - 19, 24, Imagens.cor(nomes.get(donos.get(k))));
                }
            }
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f));
            g2.setColor(new Color(255, 240, 0));
            for (String id : paradasParaYurt) anel(g2, id, 10);
            g2.setStroke(new BasicStroke(1));
        }

        // Regras, p. 10: cada cidade revelada mostra a sua peça e os tesouros que ainda tem (clique num tesouro para
        // tomá-lo). Anel vermelho: dá para atacar agora. Cidade conquistada: yurt do jogador no meio dela.
        private void desenharCidades(Graphics2D g2) {
            for (String cidade : pontosCidades.keySet()) {
                if (!cidadesReveladas.contains(cidade)) continue;
                int[] c = pontosCidades.get(cidade);
                int cx = tela(c[0]), cy = tela(c[1]);
                int dono = donosCidades.get(cidade);
                if (dono != -1) {
                    Desenho.yurt(g2, cx, cy, 34, Imagens.cor(nomes.get(dono)));
                    continue;
                }
                if (cidadesAtacaveis.contains(cidade)) {
                    g2.setStroke(new BasicStroke(4));
                    g2.setColor(new Color(230, 30, 30));
                    g2.drawOval(cx - 48, cy - 42, 96, 96);
                    g2.setStroke(new BasicStroke(1));
                }
                int topo = cy - PECA_CIDADE / 2 - 12;
                g2.drawImage(Imagens.cidade(cidade), cx - PECA_CIDADE / 2, topo, PECA_CIDADE, PECA_CIDADE, null);
                g2.setColor(new Color(60, 40, 20));
                g2.drawRect(cx - PECA_CIDADE / 2, topo, PECA_CIDADE, PECA_CIDADE);
                List<String> naCidade = tesourosCidades.get(cidade);
                for (int k = 0; k < naCidade.size(); k++) {
                    int[] p = posicaoTesouro(cidade, k, naCidade.size());
                    Desenho.peca(g2, Imagens.tesouro(naCidade.get(k)), p[0], p[1], TESOURO, new Color(60, 40, 20));
                }
            }
        }

        // §7.2: as peças de tributo de cada província ficam sobre o seu ícone (até 3). Anel azul: dá para pegar agora.
        private void desenharProvincias(Graphics2D g2) {
            for (String id : pontosProvincias.keySet()) {
                int[] p = pontosProvincias.get(id);
                int cx = tela(p[0]), cy = tela(p[1]);
                int n = pecasProvincias.get(id);
                for (int k = 0; k < n; k++) {
                    int[] d = DESLOCAMENTO[n - 1][k];
                    Desenho.peca(g2, Imagens.tributo(tiposProvincias.get(id)), cx + d[0], cy + d[1], TRIBUTO,
                            new Color(90, 60, 30));
                }
                if (provinciasAlcancaveis.contains(id)) {
                    g2.setStroke(new BasicStroke(4));
                    g2.setColor(new Color(0, 200, 255));
                    int r = n <= 1 ? TRIBUTO / 2 + 6 : TRIBUTO + 4;
                    g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
                    g2.setStroke(new BasicStroke(1));
                }
            }
        }

        // §7.3: o Khan não tem imagem no material; é desenhado como um pião dourado sobre a cabeça impressa.
        // Quando a coluna manda mover o Khan, as cabeças para onde ele pode ir ganham um anel laranja.
        private void desenharKhan(Graphics2D g2) {
            if (khanPendente) {
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {9f, 5f}, 0f));
                g2.setColor(new Color(255, 120, 0));
                for (int i = 0; i < pontosKhan.size(); i++) {
                    if (i == posicaoKhan) continue;
                    int[] p = pontosKhan.get(i);
                    g2.drawOval(tela(p[0]) - 28, tela(p[1]) - 28, 56, 56);
                }
                g2.setStroke(new BasicStroke(1));
            }
            if (posicaoKhan != -1) {
                int[] p = pontosKhan.get(posicaoKhan);
                Desenho.khan(g2, tela(p[0]), tela(p[1]), 40);
            }
        }

        // Paradas por onde o pião da vez passou neste turno (anel tracejado) e para onde ele pode andar (anel verde).
        private void desenharCaminho(Graphics2D g2) {
            Color cor = Imagens.cor(nomes.get(atual));
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {7f, 5f}, 0f));
            g2.setColor(cor);
            for (String id : visitadas) anel(g2, id, 6);
            g2.setStroke(new BasicStroke(4));
            g2.setColor(new Color(60, 255, 60));
            for (String id : destinos) anel(g2, id, 2);
            g2.setStroke(new BasicStroke(1));
        }

        private void anel(Graphics2D g2, String id, int folga) {
            int[] p = pontosParadas.get(id);
            int r = Math.min(tela(raiosParadas.get(id)), 40) + folga;
            for (int i = 0; i < p.length; i += 2)
                g2.drawOval(tela(p[i]) - r, tela(p[i + 1]) - r, 2 * r, 2 * r);
        }

        // RF09, G20: os piões de uma parada ficam lado a lado. Numa parada dupla, cada um fica num medalhão.
        private void desenharPioes(Graphics2D g2) {
            Map<String, List<Integer>> porParada = new LinkedHashMap<>();
            for (int i = 0; i < nomes.size(); i++)
                porParada.computeIfAbsent(posicoes.get(i), k -> new ArrayList<>()).add(i);
            for (Map.Entry<String, List<Integer>> p : porParada.entrySet()) {
                int[] pontos = pontosParadas.get(p.getKey());
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
                    peao(g2, pioes.get(k), x, y, PEAO);
                }
            }
        }

        private void peao(Graphics2D g2, int jogador, int x, int y, int tamanho) {
            String nome = nomes.get(jogador);
            Desenho.peca(g2, Imagens.peao(nome), x, y, tamanho, Imagens.cor(nome));
        }

        // RF10: o painel tem a cor do jogador da vez, a sua cartela e o que falta fazer no turno.
        private void desenharPainel(Graphics2D g2) {
            String nome = nomes.get(atual);
            g2.setColor(Imagens.cor(nome));
            g2.fillRect(TAB_L, 0, PAINEL_L, TAB_A);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            Desenho.texto(g2, "Vez de " + nome, TAB_L + 15, 38);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            int coluna = colunaAtivada;
            Desenho.texto(g2, coluna == -1 ? "Escolha uma coluna na cartela" : "Coluna " + (coluna + 1) + " ativada",
                    TAB_L + 15, 64);
            Desenho.cartela(g2, nome, bloqueadas, coluna, colunaSelecionada, CARTELA_X, CARTELA_Y, CARTELA_L, CARTELA_A);

            int y = CARTELA_Y + CARTELA_A + 30;
            if (coluna != -1) {
                Desenho.texto(g2, "Movimentos restantes: " + movimentos, TAB_L + 15, y);
                Desenho.texto(g2, "Punhos: " + punhos + (punhos > 0 ? "  (anéis azuis)" : ""), TAB_L + 15, y + 24);
                Desenho.texto(g2, "Khan: " + (khanPendente ? "mover (anéis laranja)" : "-"), TAB_L + 15, y + 48);
                if (!cidadesAtacaveis.isEmpty())
                    Desenho.texto(g2, "Ataque: clique num tesouro (anel vermelho)", TAB_L + 15, y + 72);
                if (!paradasParaYurt.isEmpty())
                    Desenho.texto(g2, "Yurt: botão direito (anel amarelo)", TAB_L + 15, y + 96);
            }
            if (aviso != null) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                Desenho.aviso(g2, aviso, TAB_L + 10, y + 106, PAINEL_L - 20);
            }

            // Reserva de cada jogador: moedas, espadas e yurts (tributos) e, embaixo, os tesouros tomados.
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            int topo = 480;
            Desenho.texto(g2, "Jogadores", TAB_L + 15, topo);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            for (int i = 0; i < nomes.size(); i++) {
                int yi = topo + 30 + i * 58;
                peao(g2, i, TAB_L + 30, yi + 6, 28);
                Desenho.texto(g2, (i + 1) + ". " + nomes.get(i) + (i == atual ? " (vez)" : ""), TAB_L + 55, yi);
                int x = TAB_L + 200;
                x = Desenho.contador(g2, Imagens.tributo("moeda"), moedas.get(i), x, yi);
                x = Desenho.contador(g2, Imagens.tributo("espada"), espadas.get(i), x, yi);
                Desenho.contador(g2, Imagens.tributo("yurt"), pecasYurt.get(i), x, yi);
                x = TAB_L + 55;
                for (String t : tiposTesouro)
                    x = Desenho.contador(g2, Imagens.tesouro(t), tesouros.get(i).get(t), x, yi + 26);
                Desenho.yurt(g2, x + 9, yi + 20, 18, Imagens.cor(nomes.get(i)));
                Desenho.texto(g2, String.valueOf(yurtsRestantes.get(i)), x + 22, yi + 26);
            }
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            Desenho.texto(g2, "Cidades na pilha: " + cidadesNaPilha, TAB_L + 15, TAB_A - 222);
            if (fimDisparado && !partidaTerminada) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                Desenho.texto(g2, "Fim disparado! Faltam " + turnosRestantes + " turno(s) depois deste.",
                        TAB_L + 15, TAB_A - 242);
            }
        }

        // G21: cada parada com o seu id e a sua área de clique, e cada rota como uma linha.
        private void desenharMapa(Graphics2D g2) {
            g2.setStroke(new BasicStroke(3));
            g2.setColor(new Color(0, 90, 255, 200));
            for (String id : pontosParadas.keySet())
                for (String v : vizinhasParadas.get(id))
                    if (id.compareTo(v) < 0) {
                        int[] a = centro(id), b = centro(v);
                        g2.drawLine(a[0], a[1], b[0], b[1]);
                    }
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            for (String id : pontosParadas.keySet()) {
                int[] pontos = pontosParadas.get(id);
                int r = tela(raiosParadas.get(id));
                g2.setColor(Color.MAGENTA);
                for (int i = 0; i < pontos.length; i += 2)
                    g2.drawOval(tela(pontos[i]) - r, tela(pontos[i + 1]) - r, 2 * r, 2 * r);
                int[] c = centro(id);
                Desenho.texto(g2, id, c[0] - 8, c[1] + 5);
            }
            // Províncias: uma linha laranja do ícone até cada parada da sua borda; Khan: até as 3 províncias.
            g2.setStroke(new BasicStroke(2));
            for (String id : pontosProvincias.keySet()) {
                int[] p = pontosProvincias.get(id);
                g2.setColor(Color.ORANGE);
                for (String a : adjacentesProvincias.get(id)) {
                    int[] c = centro(a);
                    g2.drawLine(tela(p[0]), tela(p[1]), c[0], c[1]);
                }
                Desenho.texto(g2, id, tela(p[0]) - 8, tela(p[1]) - 20);
            }
            g2.setColor(Color.RED);
            for (int i = 0; i < pontosKhan.size(); i++) {
                int[] k = pontosKhan.get(i);
                for (String id : provinciasKhan.get(i)) {
                    int[] p = pontosProvincias.get(id);
                    g2.drawLine(tela(k[0]), tela(k[1]), tela(p[0]), tela(p[1]));
                }
            }
            g2.setStroke(new BasicStroke(1));
        }

        private int[] centro(String id) {
            int[] p = pontosParadas.get(id);
            return p.length == 4 ? new int[] {tela((p[0] + p[2]) / 2), tela((p[1] + p[3]) / 2)}
                    : new int[] {tela(p[0]), tela(p[1])};
        }
    }
}
