package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

// Pontuação final (regras, p. 14 a 16): influência nas 3 regiões pela tabela do tabuleiro, cartas secretas e desempate.
// Tabela (melhorias na coluna 4/3/2/1/0): mais yurts 3/2/2/1/1; 2º mais yurts 2/2/1/1/0.
public class PontuacaoTest {
    private final Mapa mapa = new Mapa();
    private final Yurts yurts = new Yurts(mapa);
    private final CartasSecretas secretas = new CartasSecretas(mapa, yurts);
    private final Pontuacao pontuacao = new Pontuacao(mapa, yurts, secretas);
    private final Jogador a = jogador(Herdeiro.TOLUI), b = jogador(Herdeiro.ALTANI), c = jogador(Herdeiro.JOCHI);
    private final List<Jogador> jogadores = List.of(a, b, c);
    private final List<Parada> china = new ArrayList<>();

    public PontuacaoTest() {
        for (Parada p : mapa.getParadas())
            if (p.getRegiao() == Regiao.CHINA) china.add(p);
    }

    private Jogador jogador(Herdeiro h) {
        Jogador j = new Jogador(h, 1);
        j.posicionarPeoes(mapa.getParada(Mapa.KARAKORUM));
        return j;
    }

    private int proxima = 0;

    // n yurts do jogador em paradas da China ainda sem yurts.
    private void yurtsNaChina(Jogador j, int n) {
        for (int i = 0; i < n; i++) china.get(proxima++).getYurts().add(j);
    }

    private static void melhoriasVermelhas(Jogador j, int n) {
        for (int i = 0; i < n; i++) j.adicionarMelhoria(0, Melhoria.porId("punho-vermelha"));
    }

    @Test(timeout = 1000)
    public void primeiroESegundoLugarPelaTabela() {
        yurtsNaChina(a, 3);
        yurtsNaChina(b, 1);
        melhoriasVermelhas(a, 2);
        assertEquals("Tolui é 1º na China com 2 melhorias vermelhas",
                2, pontuacao.influencia(a, Cor.VERMELHA, jogadores));
        assertEquals("Altani é 2ª na China com 0 melhorias vermelhas",
                0, pontuacao.influencia(b, Cor.VERMELHA, jogadores));
        assertEquals("Jochi não tem yurts na China", 0, pontuacao.influencia(c, Cor.VERMELHA, jogadores));
        melhoriasVermelhas(b, 1);
        assertEquals("Altani é 2ª com 1 melhoria vermelha", 1, pontuacao.influencia(b, Cor.VERMELHA, jogadores));
        melhoriasVermelhas(a, 2);
        assertEquals("Tolui é 1º com 4 melhorias vermelhas", 3, pontuacao.influencia(a, Cor.VERMELHA, jogadores));
        assertEquals("Tolui não tem yurts na Pérsia", 0, pontuacao.influencia(a, Cor.VERDE, jogadores));
    }

    @Test(timeout = 1000)
    public void semMelhoriasO1oGanha1Voto() {
        yurtsNaChina(c, 1);
        assertEquals("o 1º sem melhorias ganha 1 voto", 1, pontuacao.influencia(c, Cor.VERMELHA, jogadores));
    }

    // Empate no 1º lugar: todos são 1º e ninguém é 2º.
    @Test(timeout = 1000)
    public void empateNoPrimeiroNinguemESegundo() {
        yurtsNaChina(a, 2);
        yurtsNaChina(b, 2);
        yurtsNaChina(c, 1);
        melhoriasVermelhas(c, 4);
        assertEquals("Tolui empata no 1º lugar", 1, pontuacao.influencia(a, Cor.VERMELHA, jogadores));
        assertEquals("Altani empata no 1º lugar", 1, pontuacao.influencia(b, Cor.VERMELHA, jogadores));
        assertEquals("Jochi não é 2º quando há empate no 1º", 0, pontuacao.influencia(c, Cor.VERMELHA, jogadores));
    }

    // Empate no 2º lugar: todos são 2º.
    @Test(timeout = 1000)
    public void empateNoSegundoTodosSaoSegundos() {
        yurtsNaChina(a, 3);
        yurtsNaChina(b, 1);
        yurtsNaChina(c, 1);
        melhoriasVermelhas(b, 3);
        melhoriasVermelhas(c, 1);
        assertEquals("Altani empata no 2º lugar, com 3 melhorias", 2, pontuacao.influencia(b, Cor.VERMELHA, jogadores));
        assertEquals("Jochi empata no 2º lugar, com 1 melhoria", 1, pontuacao.influencia(c, Cor.VERMELHA, jogadores));
    }

    // Regras, p. 15: as melhorias da coluna branca não contam, de nenhuma cor.
    @Test(timeout = 1000)
    public void colunaBrancaNaoContaParaAInfluencia() {
        yurtsNaChina(a, 1);
        a.adicionarMelhoria(3, Melhoria.porId("punho-vermelha"));
        a.adicionarMelhoria(3, Melhoria.porId("moeda-vermelha"));
        assertEquals("as melhorias da coluna branca não contam", 1, pontuacao.influencia(a, Cor.VERMELHA, jogadores));
    }

    // Regras, p. 10: o yurt no meio de uma cidade conquistada conta para a região dela.
    @Test(timeout = 1000)
    public void cidadeConquistadaContaNaRegiao() {
        mapa.getCidade("Pequim").conquistar(b);
        assertEquals("Pequim fica na China", Regiao.CHINA, mapa.getCidade("Pequim").regiao());
        assertEquals("o yurt no meio de Pequim conta para a China",
                1, pontuacao.influencia(b, Cor.VERMELHA, jogadores));
    }

    @Test(timeout = 1000)
    public void totalSomaTrilhaInfluenciaECartaSecreta() {
        a.ganharVotos(5);
        yurtsNaChina(a, 1);
        a.receberCartasSecretas(List.of(CartaSecreta.SOLDADO, CartaSecreta.VIAJANTE));
        a.escolherCartaSecreta(CartaSecreta.SOLDADO);
        melhoriasVermelhas(a, 4); // Soldado: 4 melhorias = 1 voto
        assertEquals("Soldado com 4 melhorias vermelhas: 3 votos de influência", 3, pontuacao.influencia(a, jogadores));
        assertEquals("trilha + influência + carta secreta", 5 + 3 + 1, pontuacao.total(a, jogadores));
    }

    // ---------------------------------------------------------------- vencedor e desempate (regras, p. 16)

    @Test(timeout = 1000)
    public void venceOMaiorTotal() {
        b.ganharVotos(3);
        a.ganharVotos(2);
        yurtsNaChina(a, 2); // + 1 de influência: empata com Altani
        a.ganharTesouro(Tesouro.SEDA); // e ganha no desempate pelos tesouros
        assertEquals("Tolui empata com Altani e vence pelos tesouros", List.of(0), pontuacao.vencedores(jogadores));
    }

    @Test(timeout = 1000)
    public void desempatePelaDistanciaAKarakorum() {
        a.ganharVotos(2);
        b.ganharVotos(2);
        a.setPosicao(mapa.getParada("r8")); // 2 passos de Karakorum
        b.setPosicao(mapa.getParada("r9")); // 1 passo
        assertEquals("r8 fica a 2 passos de Karakorum", 2, pontuacao.distanciaKarakorum(a));
        assertEquals("Altani vence por estar mais perto de Karakorum", List.of(1), pontuacao.vencedores(jogadores));
    }

    @Test(timeout = 1000)
    public void empateCompletoDivideAVitoria() {
        a.ganharVotos(2);
        c.ganharVotos(2);
        assertEquals("empate completo: Tolui e Jochi vencem", List.of(0, 2), pontuacao.vencedores(jogadores));
    }

    // ---------------------------------------------------------------- pela Façade

    @Test(timeout = 1000)
    public void pontuacaoPeloJogo() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        j.prepararSecretas(List.of("soldado", "viajante", "prefeito", "mensageiro"));
        Partidas.escolherSecretas(j);
        j.jogador(0).ganharVotos(4);
        for (int i = 0; i < 4; i++) j.jogador(0).adicionarMelhoria(2, Melhoria.porId("punho-amarela"));
        assertEquals("sem yurts na Rússia, as melhorias amarelas não dão votos",
                0, (int) j.get(VOTOS_INFLUENCIA).get(0).get(2));
        assertEquals("trilha + influência + Soldado com 4 melhorias", 4 + 0 + 1, (int) j.get(TOTAIS_FINAIS).get(0));
        assertEquals("a 1ª região", "China", Jogo.getInstance().get(NOMES_REGIOES).get(0));
        assertEquals("a 2ª região", "Pérsia", Jogo.getInstance().get(NOMES_REGIOES).get(1));
        assertEquals("a 3ª região", "Rússia", Jogo.getInstance().get(NOMES_REGIOES).get(2));
        assertEquals("Tolui não tem yurts na Pérsia", 0, (int) j.get(YURTS_NAS_REGIOES).get(0).get(1));
        assertEquals("Altani está em Karakorum", 0, (int) j.get(DISTANCIAS_KARAKORUM).get(1));
        assertEquals("Tolui vence", List.of(0), j.get(VENCEDORES));
        assertEquals("a coluna branca não tem região", 3, j.get(VOTOS_INFLUENCIA).get(0).size());
        assertEquals("3 regiões", 3, j.get(NOMES_REGIOES).size());
    }
}
