package model;

import java.util.ArrayList;
import java.util.List;

// Pontuação final (regras, p. 14 a 16): os votos da trilha do Kurultai, a influência nas 3 regiões, as cartas de
// conselheiro secreto e o desempate. Só calcula, sem mudar o estado da partida.
class Pontuacao {
    // Votos pela influência numa região, conforme as melhorias (0 a 4) na coluna da cor da região (tabela do tabuleiro).
    private static final int[] PRIMEIRO = {1, 1, 2, 2, 3};
    private static final int[] SEGUNDO = {0, 1, 1, 2, 2};
    private static final List<Cor> CORES = List.of(Cor.VERMELHA, Cor.VERDE, Cor.AMARELA);

    private final Mapa mapa;
    private final Yurts yurts;
    private final CartasSecretas secretas;

    Pontuacao(Mapa mapa, Yurts yurts, CartasSecretas secretas) {
        this.mapa = mapa;
        this.yurts = yurts;
        this.secretas = secretas;
    }

    /**
     * Votos de influência do jogador na região da cor (vermelha = China, verde = Pérsia, amarela = Rússia):
     * quem tem mais yurts na região e quem tem o 2º maior número ganham votos pela tabela, conforme as melhorias
     * na sua coluna dessa cor. Empate no 1º lugar: todos são 1º e ninguém é 2º. Empate no 2º: todos são 2º.
     * Quem não tem yurt na região não ganha nada.
     */
    int influencia(Jogador j, Cor cor, List<Jogador> jogadores) {
        Regiao regiao = cor.getRegiao();
        int meus = yurts.noMapa(j, regiao);
        if (meus == 0) return 0;
        int maior = 0;
        for (Jogador o : jogadores) maior = Math.max(maior, yurts.noMapa(o, regiao));
        int empatadosNoMaior = 0, segundo = 0;
        for (Jogador o : jogadores) {
            int n = yurts.noMapa(o, regiao);
            if (n == maior) empatadosNoMaior++;
            else segundo = Math.max(segundo, n);
        }
        int melhorias = Math.min(j.getMelhorias(cor.coluna()).size(), Jogador.MELHORIAS_POR_COLUNA);
        if (meus == maior) return PRIMEIRO[melhorias];
        if (empatadosNoMaior == 1 && meus == segundo) return SEGUNDO[melhorias];
        return 0;
    }

    /** Soma dos votos de influência do jogador nas 3 regiões. */
    int influencia(Jogador j, List<Jogador> jogadores) {
        int total = 0;
        for (Cor cor : CORES) total += influencia(j, cor, jogadores);
        return total;
    }

    /** Total final: trilha do Kurultai + influência + carta secreta. */
    int total(Jogador j, List<Jogador> jogadores) {
        return j.getVotos() + influencia(j, jogadores) + secretas.votos(j);
    }

    /** Passos do pião do jogador até Karakorum (o 2º desempate); para a Altani, o pião mais perto. */
    int distanciaKarakorum(Jogador j) {
        int menor = Integer.MAX_VALUE;
        for (Parada p : j.getPosicoes()) menor = Math.min(menor, mapa.distancia(p, mapa.getParada(Mapa.KARAKORUM)));
        return menor;
    }

    /**
     * Vencedores (índices): o maior total. No empate, ganha quem tem mais tesouros não entregues; se ainda empatar,
     * quem está mais perto de Karakorum; se ainda empatar, a vitória é dividida (regras, p. 16).
     */
    List<Integer> vencedores(List<Jogador> jogadores) {
        List<Integer> vencedores = new ArrayList<>();
        for (int i = 0; i < jogadores.size(); i++) {
            if (vencedores.isEmpty()) {
                vencedores.add(i);
                continue;
            }
            int c = comparar(jogadores.get(i), jogadores.get(vencedores.get(0)), jogadores);
            if (c > 0) vencedores.clear();
            if (c >= 0) vencedores.add(i);
        }
        return vencedores;
    }

    // Positivo se a está na frente de b.
    private int comparar(Jogador a, Jogador b, List<Jogador> jogadores) {
        int c = Integer.compare(total(a, jogadores), total(b, jogadores));
        if (c != 0) return c;
        c = Integer.compare(a.getTotalTesouros(), b.getTotalTesouros());
        if (c != 0) return c;
        return Integer.compare(distanciaKarakorum(b), distanciaKarakorum(a)); // mais perto é melhor
    }
}
