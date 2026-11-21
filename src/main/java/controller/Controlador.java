package controller;

import static model.ObservadoIF.JOGADOR_ATUAL;
import static model.ObservadoIF.POSICOES;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import model.Jogo;
import model.ObservadoIF;
import model.ObservadorIF;

// Controller (MVC), Singleton (cap. 17): recebe as ações da interface e chama a Façade do Model (Jogo).
// Cada ação devolve null quando dá certo, ou a mensagem do erro, que a janela pode desenhar.
// O terminal mostra o resultado do pedido: os erros e as mensagens de salvar e carregar (RF02). As mudanças da
// partida não são escritas aqui: o observador view.Terminal as escreve quando o Model avisa.
// O controlador também lê a partida só por get(chave), como as janelas.
public class Controlador {
    private static Controlador ctrl = null;

    private final Jogo jogo = Jogo.getInstance();

    private Controlador() {
    }

    public static Controlador getInstance() {
        if (ctrl == null) ctrl = new Controlador();
        return ctrl;
    }

    /**
     * Inscreve o observador no Model e devolve o observado só como ObservadoIF (Ctrl.registra no cap. 17).
     * Quem precisa desenhar o estado atual chama o próprio notify com o valor devolvido.
     */
    public ObservadoIF registra(ObservadorIF observador) {
        jogo.add(observador);
        return jogo;
    }

    public void remove(ObservadorIF observador) {
        jogo.remove(observador);
    }

    // ---------------------------------------------------------------- partida

    public String novaPartida(List<String> herdeiros) {
        try {
            jogo.novaPartida(herdeiros);
        } catch (IllegalArgumentException e) {
            return erro(e);
        }
        return null;
    }

    public String sortear(int jogadores) {
        try {
            jogo.sortear(jogadores);
        } catch (IllegalArgumentException e) {
            return erro(e);
        }
        return null;
    }

    public String carregar(Path arquivo) {
        try {
            Jogo.carregar(arquivo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            erro(e);
            return "Arquivo inválido: " + arquivo.getFileName();
        } catch (IOException e) {
            System.out.println("Erro em Controlador.carregar: não foi possível ler o arquivo (" + e.getMessage() + ").");
            return "Não foi possível ler o arquivo " + arquivo.getFileName();
        }
        System.out.println("Partida carregada de " + arquivo + ".");
        return null;
    }

    public String salvar(Path arquivo) {
        try {
            jogo.salvar(arquivo);
        } catch (IOException e) {
            System.out.println("Erro em Controlador.salvar: não foi possível gravar o arquivo (" + e.getMessage() + ").");
            return "Não foi possível salvar em " + arquivo.getFileName();
        }
        System.out.println("Partida salva em " + arquivo + ".");
        return null;
    }

    // ---------------------------------------------------------------- turno

    /** Clique numa coluna da cartela: marca ou desmarca a coluna, sem ativá-la. */
    public String selecionarColuna(int coluna) {
        try {
            jogo.selecionarColuna(atual(), coluna);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return erro(e);
        }
        return null;
    }

    public String confirmarColuna() {
        try {
            jogo.confirmarColuna(atual());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return erro(e);
        }
        return null;
    }

    /**
     * Clique no tabuleiro, em coordenadas da imagem original: numa parada, o pião anda (no próprio pião,
     * constrói um yurt); numa cabeça do Khan, o Khan vai para lá; no ícone de uma província, o jogador pega
     * 1 peça de tributo. Fora disso, nada acontece.
     */
    public String clicarTabuleiro(int x, int y) {
        int atual = atual();
        String parada = jogo.paradaEm(x, y);
        int khan = parada == null ? jogo.localKhanEm(x, y) : -1;
        String provincia = parada == null && khan == -1 ? jogo.provinciaEm(x, y) : null;
        try {
            if (parada != null && parada.equals(jogo.get(POSICOES).get(atual))) {
                return construirYurt(x, y); // clique no próprio pião: constrói um yurt onde ele está
            } else if (parada != null) {
                jogo.mover(atual, parada);
            } else if (khan != -1) {
                jogo.moverKhan(atual, khan);
            } else if (provincia != null) {
                jogo.pegarTributo(atual, provincia);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return erro(e);
        }
        return null;
    }

    /** Clique com o botão direito numa parada (ou no próprio pião): constrói um yurt lá. */
    public String construirYurt(int x, int y) {
        String parada = jogo.paradaEm(x, y);
        if (parada == null) return null;
        try {
            jogo.construirYurt(atual(), parada);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return erro(e);
        }
        return null;
    }

    /** Clique num tesouro de uma cidade: toma o tesouro com espadas e, se for o último, conquista a cidade. */
    public String atacarCidade(String cidade, String tesouro) {
        try {
            jogo.atacarCidade(atual(), cidade, tesouro);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return erro(e);
        }
        return null;
    }

    /** Entrega ao conselheiro da parada os tesouros que ele pede. */
    public String entregarTesouros(String parada) {
        try {
            jogo.entregarTesouros(atual(), parada);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return erro(e);
        }
        return null;
    }

    public String proximo() {
        try {
            jogo.encerrarTurno(atual());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return erro(e);
        }
        return null;
    }

    // As ações da interface são sempre do jogador da vez.
    private int atual() {
        return jogo.get(JOGADOR_ATUAL);
    }

    // A mensagem completa vai para o terminal. Na janela aparece só o texto depois de "Erro em Classe.metodo: ".
    private static String erro(RuntimeException e) {
        System.out.println(e.getMessage());
        String m = e.getMessage();
        int i = m.indexOf(": ");
        return m.startsWith("Erro em ") && i >= 0 ? m.substring(i + 2) : m;
    }
}
