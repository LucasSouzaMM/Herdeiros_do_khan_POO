package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// Cartas de conselheiro secreto (regras, p. 6 e 19): as 2 que cada jogador recebe no começo, a escolha de 1 antes
// da primeira rodada e a medida de cada objetivo, que no fim da partida vira 0 a 3 votos.
class CartasSecretas {
    static final int POR_JOGADOR = 2;

    private final Mapa mapa;
    private final Yurts yurts;

    CartasSecretas(Mapa mapa, Yurts yurts) {
        this.mapa = mapa;
        this.yurts = yurts;
    }

    /** §5: as 10 cartas são embaralhadas, e cada jogador recebe 2. */
    void sortear(List<Jogador> jogadores, Random aleatorio) {
        List<CartaSecreta> cartas = new ArrayList<>(Arrays.asList(CartaSecreta.values()));
        Collections.shuffle(cartas, aleatorio);
        preparar(jogadores, cartas);
    }

    // Usado também pelos testes: o jogador i recebe as cartas 2i e 2i + 1 da ordem dada.
    void preparar(List<Jogador> jogadores, List<CartaSecreta> ordem) {
        for (int i = 0; i < jogadores.size(); i++)
            jogadores.get(i).receberCartasSecretas(ordem.subList(POR_JOGADOR * i, POR_JOGADOR * i + POR_JOGADOR));
    }

    /** O primeiro jogador, na ordem da partida, que ainda precisa escolher a carta secreta, ou -1 se todos já escolheram. */
    static int escolhendo(List<Jogador> jogadores) {
        for (int i = 0; i < jogadores.size(); i++)
            if (jogadores.get(i).getCartaSecreta() == null) return i;
        return -1;
    }

    /** O jogador fica com uma das 2 cartas que recebeu; a outra sai do jogo (regras, p. 6). */
    void escolher(Jogador j, CartaSecreta c) {
        if (j.getCartaSecreta() != null)
            throw new IllegalStateException("Erro em CartasSecretas.escolher: " + j.getNome()
                    + " já escolheu a sua carta secreta.");
        if (!j.getOpcoesSecretas().contains(c))
            throw new IllegalArgumentException("Erro em CartasSecretas.escolher: " + j.getNome() + " não recebeu a carta "
                    + c.getNome() + ".");
        j.escolherCartaSecreta(c);
    }

    // ---------------------------------------------------------------- medidas dos objetivos (regras, p. 19)

    /** A medida do objetivo da carta para o jogador agora (por exemplo, quantas cidades têm 1 yurt dele ao redor). */
    int medida(Jogador j, CartaSecreta c) {
        return switch (c) {
            case VIAJANTE -> maiorGrupoDeYurts(j);
            case SOLDADO -> melhorias(j).size();
            case COLECIONADOR -> simbolos(j).size();
            case SACERDOTE -> {
                int pares = 0;
                for (int n : simbolos(j).values()) pares += n / 2;
                yield pares;
            }
            case GOVERNADOR -> {
                int regioes = 0;
                for (Cor cor : List.of(Cor.VERMELHA, Cor.VERDE, Cor.AMARELA))
                    if (yurts.noMapa(j, cor.getRegiao()) >= 2 && j.getMelhorias(cor.coluna()).size() >= 2) regioes++;
                yield regioes;
            }
            case BAJULADOR -> {
                int provincias = 0;
                for (LocalKhan l : mapa.getLocaisKhan())
                    if (yurtsEm(j, l.getPropria().getAdjacentes()) >= 2) provincias++;
                yield provincias;
            }
            case PREFEITO, CIDADAO -> {
                int minimo = c == CartaSecreta.PREFEITO ? 2 : 1, cidades = 0;
                for (Cidade cidade : mapa.getCidades())
                    if (yurtsEm(j, cidade.getAdjacentes()) >= minimo) cidades++;
                yield cidades;
            }
            case CAVALEIRO -> {
                int n = 0;
                for (Melhoria m : melhorias(j))
                    if (m.getTipo() == Melhoria.Tipo.CAVALOS || m.getTipo() == Melhoria.Tipo.PUNHO) n++;
                yield n;
            }
            case MENSAGEIRO -> {
                int duplas = 0;
                for (Parada p : mapa.getParadas())
                    if (p.isDupla() && !p.getId().equals(Mapa.KARAKORUM) && p.temYurtDe(j)) duplas++;
                yield duplas;
            }
        };
    }

    /** Votos (0 a 3) que a carta secreta escolhida pelo jogador dá agora; 0 se ele ainda não escolheu. */
    int votos(Jogador j) {
        CartaSecreta c = j.getCartaSecreta();
        return c == null ? 0 : c.votos(medida(j, c));
    }

    // Todas as melhorias do jogador, nas 4 colunas.
    private static List<Melhoria> melhorias(Jogador j) {
        List<Melhoria> todas = new ArrayList<>();
        for (int c = 0; c < 4; c++) todas.addAll(j.getMelhorias(c));
        return todas;
    }

    // Quantas melhorias o jogador tem de cada símbolo. Cada tipo de tesouro virtual é um símbolo diferente.
    private static Map<String, Integer> simbolos(Jogador j) {
        Map<String, Integer> simbolos = new HashMap<>();
        for (Melhoria m : melhorias(j)) {
            String s = m.getTipo() == Melhoria.Tipo.TESOURO ? m.getTesouro().getNome() : m.getTipo().getNome();
            simbolos.merge(s, 1, Integer::sum);
        }
        return simbolos;
    }

    // Yurts do jogador nas paradas dadas (uma parada dupla pode ter 2 dele).
    private static int yurtsEm(Jogador j, Iterable<Parada> paradas) {
        int n = 0;
        for (Parada p : paradas)
            for (Jogador dono : p.getYurts())
                if (dono == j) n++;
        return n;
    }

    // O maior grupo de yurts do jogador ligados por rotas: paradas vizinhas com yurt dele ficam no mesmo grupo.
    private int maiorGrupoDeYurts(Jogador j) {
        Set<Parada> vistas = new HashSet<>();
        int maior = 0;
        for (Parada inicio : mapa.getParadas()) {
            if (!inicio.temYurtDe(j) || !vistas.add(inicio)) continue;
            int grupo = 0;
            Deque<Parada> fila = new ArrayDeque<>(List.of(inicio));
            while (!fila.isEmpty()) {
                Parada p = fila.poll();
                grupo += yurtsEm(j, List.of(p));
                for (Parada v : p.getVizinhas())
                    if (v.temYurtDe(j) && vistas.add(v)) fila.add(v);
            }
            maior = Math.max(maior, grupo);
        }
        return maior;
    }
}
