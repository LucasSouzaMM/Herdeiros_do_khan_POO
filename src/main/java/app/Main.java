package app;

import javax.swing.SwingUtilities;

import view.JanelaInicial;

// Abre a primeira janela do jogo. O terminal só exibe os retornos das funções e os erros (RF02).
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new JanelaInicial();
            }
        });
    }
}
