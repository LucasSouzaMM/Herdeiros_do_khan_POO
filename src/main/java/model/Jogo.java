package model;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Jogo {
    public static final int MIN_JOGADORES = 2;
    public static final int MAX_JOGADORES = 5;

    private final List<Jogador> jogadores = new ArrayList<>();
    private int jogadorAtual;
    private Turno turno;

    public Jogo(List<String> nomes) {
        validarNumeroJogadores(nomes.size());
        // §5: 1 moeda para o 1º e o 2º jogadores, 2 moedas para cada um dos outros.
        for (int i = 0; i < nomes.size(); i++)
            jogadores.add(new Jogador(nomes.get(i), i < 2 ? 1 : 2));
    }

    public static void validarNumeroJogadores(int n) {
        if (n < MIN_JOGADORES || n > MAX_JOGADORES)
            throw new IllegalArgumentException("Erro em Jogo.validarNumeroJogadores: " + n
                    + " jogadores não é válido (mínimo: " + MIN_JOGADORES + ", máximo: " + MAX_JOGADORES
                    + "). Tente novamente.");
    }

    public void ativarColuna(int jogador, int coluna) {
        exigirVez(jogador, "ativarColuna");
        if (turno != null)
            throw new IllegalStateException("Erro em Jogo.ativarColuna: " + getNome(jogador)
                    + " já ativou uma coluna neste turno.");
        turno = new Turno(jogadores.get(jogador), coluna);
    }

    public void encerrarTurno(int jogador) {
        exigirVez(jogador, "encerrarTurno");
        turno = null;
        jogadorAtual = (jogadorAtual + 1) % jogadores.size();
    }

    public int getNumeroJogadores() {
        return jogadores.size();
    }

    public int getJogadorAtual() {
        return jogadorAtual;
    }

    public String getNome(int jogador) {
        return buscar(jogador, "getNome").getNome();
    }

    public int getMoedas(int jogador) {
        return buscar(jogador, "getMoedas").getMoedas();
    }

    /** Retorna -1 se nenhuma coluna foi ativada neste turno. */
    public int getColunaAtivada() {
        return turno == null ? -1 : turno.getColuna();
    }

    public void salvar(Path arquivo) throws IOException {
        StringBuilder s = new StringBuilder();
        s.append("jogadores=").append(jogadores.size()).append('\n');
        s.append("jogadorAtual=").append(jogadorAtual).append('\n');
        s.append("coluna=").append(getColunaAtivada()).append('\n');
        for (int i = 0; i < jogadores.size(); i++) {
            s.append("jogador").append(i).append(".nome=").append(jogadores.get(i).getNome()).append('\n');
            s.append("jogador").append(i).append(".moedas=").append(jogadores.get(i).getMoedas()).append('\n');
        }
        Files.writeString(arquivo, s);
    }

    public static Jogo carregar(Path arquivo) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(arquivo)) {
            p.load(r);
        }
        int n = lerInt(p, "jogadores");
        validarNumeroJogadores(n);
        List<String> nomes = new ArrayList<>();
        for (int i = 0; i < n; i++)
            nomes.add(ler(p, "jogador" + i + ".nome"));

        Jogo jogo = new Jogo(nomes);
        for (int i = 0; i < n; i++)
            jogo.jogadores.set(i, new Jogador(nomes.get(i), lerInt(p, "jogador" + i + ".moedas")));
        jogo.jogadorAtual = lerInt(p, "jogadorAtual");
        jogo.buscar(jogo.jogadorAtual, "carregar");
        int coluna = lerInt(p, "coluna");
        if (coluna != -1)
            jogo.turno = new Turno(jogo.jogadores.get(jogo.jogadorAtual), coluna);
        return jogo;
    }

    private static String ler(Properties p, String chave) {
        String valor = p.getProperty(chave);
        if (valor == null)
            throw new IllegalArgumentException("Erro em Jogo.carregar: falta a chave '" + chave + "' no arquivo.");
        return valor;
    }

    private static int lerInt(Properties p, String chave) {
        String valor = ler(p, chave);
        try {
            return Integer.parseInt(valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Erro em Jogo.carregar: o valor de '" + chave
                    + "' não é um número inteiro (" + valor + ").");
        }
    }

    private Jogador buscar(int jogador, String funcao) {
        if (jogador < 0 || jogador >= jogadores.size())
            throw new IllegalArgumentException("Erro em Jogo." + funcao + ": o jogador " + (jogador + 1)
                    + " não existe (use 1 a " + jogadores.size() + ").");
        return jogadores.get(jogador);
    }

    private void exigirVez(int jogador, String funcao) {
        buscar(jogador, funcao);
        if (jogador != jogadorAtual)
            throw new IllegalStateException("Erro em Jogo." + funcao + ": não é a vez de " + getNome(jogador)
                    + ". A vez é de " + getNome(jogadorAtual) + ".");
    }
}
