package app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import model.Jogo;

// Menu de terminal para testar cada função de Jogo (RF02). O usuário vê jogadores de 1 a n.
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static Jogo jogo;

    public static void main(String[] args) {
        while (true) {
            mostrar();
            System.out.println("""

                    1. Criar partida
                    2. Ativar coluna
                    3. Encerrar turno
                    4. Ver estado
                    5. Salvar partida
                    6. Carregar partida
                    0. Sair""");
            String opcao = in.nextLine().strip();
            if (opcao.equals("0")) return;
            try {
                switch (opcao) {
                    case "1" -> criar();
                    case "2" -> exigirJogo().ativarColuna(lerJogador(), lerInt("Coluna (0 a 3): "));
                    case "3" -> exigirJogo().encerrarTurno(lerJogador());
                    case "4" -> mostrar();
                    case "5" -> exigirJogo().salvar(Path.of(ler("Arquivo: ")));
                    case "6" -> jogo = Jogo.carregar(Path.of(ler("Arquivo: ")));
                    default -> System.out.println("Opção inválida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Erro em Main.main: não foi possível ler ou gravar o arquivo (" + e.getMessage() + ").");
            }
        }
    }

    private static void criar() {
        int n;
        while (true) { // T2: pede de novo até o número ser válido
            n = lerInt("Número de jogadores: ");
            try {
                Jogo.validarNumeroJogadores(n);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
        List<String> nomes = new ArrayList<>();
        for (int i = 1; i <= n; i++) nomes.add(ler("Nome do jogador " + i + ": "));
        jogo = new Jogo(nomes);
    }

    private static void mostrar() {
        if (jogo == null) return;
        for (int i = 0; i < jogo.getNumeroJogadores(); i++)
            System.out.printf("%s%d. %s: %d moeda(s)%n", i == jogo.getJogadorAtual() ? "> " : "  ",
                    i + 1, jogo.getNome(i), jogo.getMoedas(i));
        int coluna = jogo.getColunaAtivada();
        System.out.println("Coluna ativada: " + (coluna == -1 ? "nenhuma" : coluna));
        String nome = jogo.getNome(jogo.getJogadorAtual());
        System.out.printf("Vez de %s (jogador %d).%n", nome, jogo.getJogadorAtual() + 1);
        // Uma coluna por turno: depois de ativar, só resta encerrar o turno.
        System.out.println("Ações de " + nome + ": "
                + (coluna == -1 ? "2. Ativar coluna, 3. Encerrar turno" : "3. Encerrar turno"));
    }

    private static Jogo exigirJogo() {
        if (jogo == null) throw new IllegalStateException("Erro em Main.exigirJogo: crie ou carregue uma partida antes.");
        return jogo;
    }

    // Enter = jogador da vez. Um número permite testar ações fora da vez (T4, T7).
    private static int lerJogador() {
        int atual = jogo.getJogadorAtual();
        while (true) {
            String s = ler("Jogador que age (Enter = " + jogo.getNome(atual) + "): ").strip();
            if (s.isEmpty()) return atual;
            try {
                return Integer.parseInt(s) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Erro em Main.lerJogador: digite um número inteiro ou só Enter.");
            }
        }
    }

    private static String ler(String pergunta) {
        System.out.print(pergunta);
        return in.nextLine();
    }

    private static int lerInt(String pergunta) {
        while (true) {
            try {
                return Integer.parseInt(ler(pergunta).strip());
            } catch (NumberFormatException e) {
                System.out.println("Erro em Main.lerInt: digite um número inteiro.");
            }
        }
    }
}
