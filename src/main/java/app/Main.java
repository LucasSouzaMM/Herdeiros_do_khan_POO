package app;

import javax.swing.SwingUtilities;

import view.JanelaInicial;
import view.Terminal;

// Inscreve o observador do terminal e abre a primeira janela do jogo. O terminal só exibe as mudanças da
// partida (Terminal) e os retornos e erros das ações (Controlador), conforme o RF02.
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Terminal();
                new JanelaInicial();
            }
        });
    }
}
