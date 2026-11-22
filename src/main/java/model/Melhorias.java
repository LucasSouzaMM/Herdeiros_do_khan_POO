package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Melhorias (regras, p. 9, 11, 12, 16 e 17): a pilha, a área de melhorias com 5 espaços na parte de baixo do
// tabuleiro, a compra com 2 moedas, a melhoria grátis da linha completa e o Khan na área de melhorias.
class Melhorias {
    static final int ESPACOS = 5;
    static final int CUSTO = 2;          // moedas por melhoria
    static final int ESPACO_MOEDA = 1;   // espaço com o ícone da moeda (o 2º)
    static final int ESPACO_ESPADA = 3;  // espaço com o ícone da espada (o 4º)

    // Posições na imagem original do tabuleiro (2905 x 2919): centro de cada espaço da área, tamanho dos espaços,
    // a cabeça do Khan à esquerda da área e o lugar onde a pilha é desenhada, logo acima da área.
    static final int[][] PONTOS = {{304, 2800}, {569, 2800}, {831, 2800}, {1094, 2800}, {1359, 2800}};
    static final int LARGURA = 228, ALTURA = 120;
    static final int KHAN_X = 105, KHAN_Y = 2800;
    static final int PILHA_X = 440, PILHA_Y = 2677, PILHA_ALTURA = 76;

    private final List<Melhoria> pilha = new ArrayList<>(); // viradas para baixo, a do topo primeiro
    private final Melhoria[] area = new Melhoria[ESPACOS];   // null = espaço vazio

    /**
     * As 44 melhorias do jogo. As regras (p. 16) dão a quantidade de cada tipo, mas não a cor de cada peça.
     * SUPOSIÇÃO, a conferir com as imagens dos cartões ou com o jogo físico: cada tipo foi dividido o mais
     * igualmente possível entre as 3 cores. Para corrigir, basta mudar os números desta tabela
     * (peças vermelhas, verdes e amarelas de cada tipo) e a cor de cada tesouro virtual.
     */
    static List<Melhoria> todas() {
        List<Melhoria> todas = new ArrayList<>();
        adicionar(todas, Melhoria.Tipo.MOEDA, 3, 3, 2);   // 8
        adicionar(todas, Melhoria.Tipo.ESPADA, 2, 3, 3);  // 8
        adicionar(todas, Melhoria.Tipo.YURT, 3, 2, 3);    // 8
        adicionar(todas, Melhoria.Tipo.PUNHO, 2, 2, 2);   // 6
        adicionar(todas, Melhoria.Tipo.CAVALOS, 3, 3, 3); // 9
        todas.add(new Melhoria(Melhoria.Tipo.TESOURO, Cor.VERMELHA, Tesouro.BOI)); // 5, um de cada tesouro
        todas.add(new Melhoria(Melhoria.Tipo.TESOURO, Cor.VERDE, Tesouro.SEDA));
        todas.add(new Melhoria(Melhoria.Tipo.TESOURO, Cor.AMARELA, Tesouro.FERRO));
        todas.add(new Melhoria(Melhoria.Tipo.TESOURO, Cor.VERMELHA, Tesouro.PELE));
        todas.add(new Melhoria(Melhoria.Tipo.TESOURO, Cor.VERDE, Tesouro.ESPECIARIA));
        return todas;
    }

    private static void adicionar(List<Melhoria> lista, Melhoria.Tipo tipo, int vermelhas, int verdes, int amarelas) {
        for (int i = 0; i < vermelhas; i++) lista.add(new Melhoria(tipo, Cor.VERMELHA));
        for (int i = 0; i < verdes; i++) lista.add(new Melhoria(tipo, Cor.VERDE));
        for (int i = 0; i < amarelas; i++) lista.add(new Melhoria(tipo, Cor.AMARELA));
    }

    /** §5: a pilha é embaralhada e 5 melhorias são reveladas na área. */
    void sortear(Random aleatorio) {
        List<Melhoria> todas = todas();
        Collections.shuffle(todas, aleatorio);
        preparar(todas);
    }

    // Usado também pelos testes, para começar com melhorias conhecidas: as 5 primeiras vão para a área, da esquerda
    // para a direita, e as outras ficam na pilha, na ordem dada.
    void preparar(List<Melhoria> ordem) {
        pilha.clear();
        pilha.addAll(ordem);
        Arrays.fill(area, null);
        revelar();
    }

    /** Melhoria no espaço da área (0 a 4), ou null se o espaço está vazio. */
    Melhoria getArea(int espaco) {
        exigirEspaco(espaco, "getArea");
        return area[espaco];
    }

    int naPilha() {
        return pilha.size();
    }

    List<Melhoria> getPilha() {
        return Collections.unmodifiableList(pilha);
    }

    // Ao carregar uma partida salva: a área (null nos espaços vazios) e a pilha, na ordem gravada.
    void restaurar(Melhoria[] area, List<Melhoria> pilha) {
        System.arraycopy(area, 0, this.area, 0, ESPACOS);
        this.pilha.clear();
        this.pilha.addAll(pilha);
    }

    /** Tira a melhoria do topo da pilha, ou null se ela acabou (as 2 melhorias iniciais do Jochi). */
    Melhoria tirarDaPilha() {
        return pilha.isEmpty() ? null : pilha.remove(0);
    }

    /** Há alguma melhoria para pegar, na área ou na pilha. */
    boolean temDisponivel() {
        if (!pilha.isEmpty()) return true;
        for (Melhoria m : area)
            if (m != null) return true;
        return false;
    }

    /**
     * Compra com 2 moedas (reais ou virtuais) a melhoria do espaço da área (0 a 4) ou, com espaco = ESPACOS, a do
     * topo da pilha, e a coloca na coluna da sua cor, de baixo para cima (regras, p. 11). As outras melhorias da
     * área andam para a direita, e nenhuma nova é revelada. Se a compra completar uma linha, o jogador ganha uma
     * melhoria grátis para a coluna branca, que ele escolhe depois.
     */
    void comprar(Jogador j, Turno turno, int espaco) {
        exigirEspacoOuPilha(espaco, "comprar");
        String erro = erroDaCompra(j, turno, espaco);
        if (erro != null) throw new IllegalStateException("Erro em Melhorias.comprar: " + erro);
        Melhoria m = olhar(espaco);
        turno.pagar(Tributo.MOEDA, CUSTO);
        retirar(espaco);
        j.adicionarMelhoria(m.getCor().coluna(), m);
        premiarLinhas(j, turno);
    }

    /** Por que o jogador não pode comprar a melhoria do espaço (0 a 4, ou ESPACOS) agora, ou null se pode. */
    String erroDaCompra(Jogador j, Turno turno, int espaco) {
        if (turno.getMelhoriasGratis() > 0)
            return j.getNome() + " completou uma linha e precisa antes escolher a melhoria grátis da coluna branca.";
        String vazio = erroDoEspacoVazio(espaco);
        if (vazio != null) return vazio;
        Melhoria m = olhar(espaco);
        if (j.getMelhorias(m.getCor().coluna()).size() >= Jogador.MELHORIAS_POR_COLUNA)
            return "a coluna " + m.getCor().getNome() + " de " + j.getNome() + " já tem " + Jogador.MELHORIAS_POR_COLUNA
                    + " melhorias.";
        if (turno.disponivel(Tributo.MOEDA) < CUSTO)
            return "uma melhoria custa " + CUSTO + " moedas, e " + j.getNome() + " tem " + turno.disponivel(Tributo.MOEDA)
                    + ".";
        return null;
    }

    // Regras, p. 12: cada linha de 3 melhorias completa dá 1 melhoria grátis para a coluna branca.
    private static void premiarLinhas(Jogador j, Turno turno) {
        while (j.getLinhasPremiadas() < j.linhasCompletas()) {
            j.premiarLinha();
            turno.ganharMelhoriaGratis();
        }
    }

    /** A melhoria grátis da linha completa: a do espaço da área ou a do topo da pilha, na coluna branca, de qualquer cor. */
    void escolherGratis(Jogador j, Turno turno, int espaco) {
        exigirEspacoOuPilha(espaco, "escolherGratis");
        String erro = erroDaEscolhaGratis(j, turno, espaco);
        if (erro != null) throw new IllegalStateException("Erro em Melhorias.escolherGratis: " + erro);
        Melhoria m = olhar(espaco);
        retirar(espaco);
        j.adicionarMelhoria(Cor.BRANCA.coluna(), m);
        turno.usarMelhoriaGratis();
    }

    /** Por que o jogador não pode escolher como grátis a melhoria do espaço agora, ou null se pode. */
    String erroDaEscolhaGratis(Jogador j, Turno turno, int espaco) {
        if (turno.getMelhoriasGratis() == 0) return j.getNome() + " não tem melhoria grátis para escolher.";
        String vazio = erroDoEspacoVazio(espaco);
        if (vazio != null) return vazio;
        if (j.getMelhorias(Cor.BRANCA.coluna()).size() >= Jogador.MELHORIAS_POR_COLUNA)
            return "a coluna branca de " + j.getNome() + " já está cheia.";
        return null;
    }

    private String erroDoEspacoVazio(int espaco) {
        if (espaco == ESPACOS) return pilha.isEmpty() ? "a pilha de melhorias acabou." : null;
        return area[espaco] == null ? "o espaço " + (espaco + 1) + " da área de melhorias está vazio." : null;
    }

    // A melhoria do espaço (0 a 4) ou do topo da pilha (ESPACOS). O espaço já foi conferido.
    private Melhoria olhar(int espaco) {
        return espaco == ESPACOS ? pilha.get(0) : area[espaco];
    }

    private static void exigirEspacoOuPilha(int espaco, String funcao) {
        if (espaco != ESPACOS) exigirEspaco(espaco, funcao);
    }

    // Tira a melhoria do lugar e anda as restantes da área para a direita, sem revelar novas (regras, p. 11).
    private void retirar(int espaco) {
        if (espaco == ESPACOS) {
            pilha.remove(0);
            return;
        }
        area[espaco] = null;
        int destino = ESPACOS - 1;
        for (int i = ESPACOS - 1; i >= 0; i--)
            if (area[i] != null) {
                Melhoria m = area[i];
                area[i] = null;
                area[destino--] = m;
            }
    }

    /**
     * §7.3: o Khan foi para a área de melhorias. Retorna os tributos dos espaços com ícone (moeda e espada) que
     * estavam vazios: o jogador os ganha do estoque. Depois, a pilha preenche os espaços vazios.
     */
    List<Tributo> receberKhan() {
        List<Tributo> ganhos = new ArrayList<>();
        if (area[ESPACO_MOEDA] == null) ganhos.add(Tributo.MOEDA);
        if (area[ESPACO_ESPADA] == null) ganhos.add(Tributo.ESPADA);
        revelar();
        return ganhos;
    }

    // Os espaços vazios recebem melhorias da pilha, da esquerda para a direita, enquanto houver.
    private void revelar() {
        for (int i = 0; i < ESPACOS && !pilha.isEmpty(); i++)
            if (area[i] == null) area[i] = pilha.remove(0);
    }

    private static void exigirEspaco(int espaco, String funcao) {
        if (espaco < 0 || espaco >= ESPACOS)
            throw new IllegalArgumentException("Erro em Melhorias." + funcao + ": o espaço " + espaco
                    + " não existe (use 0 a " + (ESPACOS - 1) + ", ou " + ESPACOS + " para a pilha).");
    }

    // ---------------------------------------------------------------- posições no tabuleiro

    /** Espaço da área (0 a 4) que contém o ponto (x, y) da imagem original, ESPACOS para a pilha, ou -1. */
    static int espacoEm(int x, int y) {
        for (int i = 0; i < ESPACOS; i++)
            if (Math.abs(x - PONTOS[i][0]) <= LARGURA / 2 && Math.abs(y - PONTOS[i][1]) <= ALTURA / 2) return i;
        if (Math.abs(x - PILHA_X) <= LARGURA / 2 && Math.abs(y - PILHA_Y) <= PILHA_ALTURA / 2) return ESPACOS;
        return -1;
    }

    /** A cabeça do Khan da área de melhorias contém o ponto (x, y) da imagem original. */
    static boolean khanEm(int x, int y) {
        long dx = x - KHAN_X, dy = y - KHAN_Y;
        return dx * dx + dy * dy <= (long) LocalKhan.RAIO * LocalKhan.RAIO;
    }
}
