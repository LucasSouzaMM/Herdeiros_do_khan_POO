package model;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class Jogo {
    public static final int MIN_JOGADORES = 2;
    public static final int MAX_JOGADORES = 5;

    private final Mapa mapa = new Mapa();
    private final List<Jogador> jogadores = new ArrayList<>();
    private int jogadorAtual;
    private Turno turno;

    /** Cria a partida com os herdeiros na ordem dos jogadores (por exemplo, "Tolui", "Altani"). */
    public Jogo(List<String> herdeiros) {
        validarNumeroJogadores(herdeiros.size());
        Set<Herdeiro> escolhidos = EnumSet.noneOf(Herdeiro.class);
        for (int i = 0; i < herdeiros.size(); i++) {
            Herdeiro h = Herdeiro.porNome(herdeiros.get(i));
            if (!escolhidos.add(h))
                throw new IllegalArgumentException("Erro em Jogo.Jogo: o herdeiro " + h.getNome()
                        + " foi escolhido mais de uma vez.");
            // §5: 1 moeda para o 1º e o 2º jogadores, 2 moedas para cada um dos outros.
            Jogador j = new Jogador(h, i < 2 ? 1 : 2);
            j.setPosicao(mapa.getParada(Mapa.KARAKORUM)); // §5: todos os piões começam em Karakorum.
            jogadores.add(j);
        }
    }

    /** Cria a partida com n jogadores e sorteia o herdeiro de cada um (RF07). */
    public static Jogo sortear(int n) {
        validarNumeroJogadores(n);
        List<Herdeiro> todos = Arrays.asList(Herdeiro.values());
        Collections.shuffle(todos);
        List<String> nomes = new ArrayList<>();
        for (Herdeiro h : todos.subList(0, n)) nomes.add(h.getNome());
        return new Jogo(nomes);
    }

    public static void validarNumeroJogadores(int n) {
        if (n < MIN_JOGADORES || n > MAX_JOGADORES)
            throw new IllegalArgumentException("Erro em Jogo.validarNumeroJogadores: " + n
                    + " jogadores não é válido (mínimo: " + MIN_JOGADORES + ", máximo: " + MAX_JOGADORES
                    + "). Tente novamente.");
    }

    /** Ativa a coluna (0 a 3, da esquerda para a direita na cartela) e dá os movimentos dos seus cavalos. */
    public void ativarColuna(int jogador, int coluna) {
        exigirVez(jogador, "ativarColuna");
        if (turno != null)
            throw new IllegalStateException("Erro em Jogo.ativarColuna: " + getNome(jogador)
                    + " já ativou uma coluna neste turno.");
        turno = new Turno(jogadores.get(jogador), coluna);
    }

    /**
     * Move o pião do jogador para a parada (§7.1), gastando a menor distância até ela.
     * O turno passa sozinho quando os movimentos acabam.
     */
    public void mover(int jogador, String paradaId) {
        exigirColuna(jogador, "mover");
        Jogador j = jogadores.get(jogador);
        Parada destino = mapa.getParada(paradaId);
        if (destino == j.getPosicao())
            throw new IllegalArgumentException("Erro em Jogo.mover: o pião de " + j.getNome() + " já está em " + paradaId + ".");
        int passos = mapa.distancia(j.getPosicao(), destino);
        if (passos < 0 || passos > turno.getMovimentos())
            throw new IllegalArgumentException("Erro em Jogo.mover: " + paradaId + " está a " + passos + " passo(s), mas "
                    + j.getNome() + " só tem " + turno.getMovimentos() + " movimento(s).");
        if (!cabe(destino))
            throw new IllegalArgumentException("Erro em Jogo.mover: a parada " + paradaId + " já está ocupada.");
        j.setPosicao(destino);
        turno.gastar(passos);
        if (turno.getMovimentos() == 0) passarVez();
    }

    // §7.1: não se termina numa parada com outro pião, exceto numa parada dupla (2 piões) e em Karakorum (todos).
    private boolean cabe(Parada p) {
        if (p.getId().equals(Mapa.KARAKORUM)) return true;
        int pioes = 0;
        for (Jogador j : jogadores)
            if (j.getPosicao() == p) pioes++;
        return pioes < (p.isDupla() ? 2 : 1);
    }

    public void encerrarTurno(int jogador) {
        exigirColuna(jogador, "encerrarTurno");
        passarVez();
    }

    private void passarVez() {
        turno = null;
        jogadorAtual = (jogadorAtual + 1) % jogadores.size();
    }

    /** Retorna o id da parada que contém o ponto (x, y) da imagem original do tabuleiro, ou null. */
    public String paradaEm(int x, int y) {
        Parada p = mapa.paradaEm(x, y);
        return p == null ? null : p.getId();
    }

    /** Nomes dos 5 herdeiros, na ordem das cartelas. */
    public static List<String> getHerdeiros() {
        List<String> nomes = new ArrayList<>();
        for (Herdeiro h : Herdeiro.values()) nomes.add(h.getNome());
        return nomes;
    }

    /** Ids de todas as paradas do mapa. */
    public List<String> getParadas() {
        List<String> ids = new ArrayList<>();
        for (Parada p : mapa.getParadas()) ids.add(p.getId());
        return ids;
    }

    /** Centro de cada medalhão da parada na imagem original do tabuleiro: x1, y1[, x2, y2]. */
    public int[] getPontos(String paradaId) {
        return mapa.getParada(paradaId).getPontos();
    }

    /** Raio da área de clique da parada, em pixels da imagem original. */
    public int getRaio(String paradaId) {
        return mapa.getParada(paradaId).getRaio();
    }

    public List<String> getVizinhas(String paradaId) {
        List<String> ids = new ArrayList<>();
        for (Parada p : mapa.getParada(paradaId).getVizinhas()) ids.add(p.getId());
        return ids;
    }

    public int getNumeroJogadores() {
        return jogadores.size();
    }

    public int getJogadorAtual() {
        return jogadorAtual;
    }

    /** Retorna o nome do herdeiro do jogador. */
    public String getNome(int jogador) {
        return buscar(jogador, "getNome").getNome();
    }

    public int getMoedas(int jogador) {
        return buscar(jogador, "getMoedas").getMoedas();
    }

    /** Retorna o id da parada onde está o pião do jogador. */
    public String getPosicao(int jogador) {
        return buscar(jogador, "getPosicao").getPosicao().getId();
    }

    /** Retorna -1 se nenhuma coluna foi ativada neste turno. */
    public int getColunaAtivada() {
        return turno == null ? -1 : turno.getColuna();
    }

    /** Retorna 0 se nenhuma coluna foi ativada neste turno. */
    public int getMovimentos() {
        return turno == null ? 0 : turno.getMovimentos();
    }

    public void salvar(Path arquivo) throws IOException {
        StringBuilder s = new StringBuilder();
        s.append("jogadores=").append(jogadores.size()).append('\n');
        s.append("jogadorAtual=").append(jogadorAtual).append('\n');
        s.append("coluna=").append(getColunaAtivada()).append('\n');
        s.append("movimentos=").append(getMovimentos()).append('\n');
        for (int i = 0; i < jogadores.size(); i++) {
            Jogador j = jogadores.get(i);
            s.append("jogador").append(i).append(".herdeiro=").append(j.getNome()).append('\n');
            s.append("jogador").append(i).append(".moedas=").append(j.getMoedas()).append('\n');
            s.append("jogador").append(i).append(".posicao=").append(j.getPosicao().getId()).append('\n');
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
        List<String> herdeiros = new ArrayList<>();
        for (int i = 0; i < n; i++)
            herdeiros.add(ler(p, "jogador" + i + ".herdeiro").strip());

        Jogo jogo = new Jogo(herdeiros);
        for (int i = 0; i < n; i++) {
            Jogador j = new Jogador(jogo.jogadores.get(i).getHerdeiro(), lerInt(p, "jogador" + i + ".moedas"));
            j.setPosicao(jogo.mapa.getParada(ler(p, "jogador" + i + ".posicao").strip()));
            jogo.jogadores.set(i, j);
        }
        jogo.jogadorAtual = lerInt(p, "jogadorAtual");
        jogo.buscar(jogo.jogadorAtual, "carregar");
        int coluna = lerInt(p, "coluna");
        if (coluna != -1)
            jogo.turno = new Turno(jogo.jogadores.get(jogo.jogadorAtual), coluna, lerInt(p, "movimentos"));
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

    private void exigirColuna(int jogador, String funcao) {
        exigirVez(jogador, funcao);
        if (turno == null)
            throw new IllegalStateException("Erro em Jogo." + funcao + ": " + getNome(jogador)
                    + " ainda não ativou uma coluna neste turno.");
    }

    private void exigirVez(int jogador, String funcao) {
        buscar(jogador, funcao);
        if (jogador != jogadorAtual)
            throw new IllegalStateException("Erro em Jogo." + funcao + ": não é a vez de " + getNome(jogador)
                    + ". A vez é de " + getNome(jogadorAtual) + ".");
    }
}
