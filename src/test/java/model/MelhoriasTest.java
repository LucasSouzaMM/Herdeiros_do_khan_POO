package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

// Melhorias (regras, p. 9, 11, 12, 16 e 17). Tolui é o jogador 0 e começa com 1 moeda. As colunas de Tolui:
// 0 (vermelha) 1 cavalo, 1 punho e a moeda virtual; 1 (verde) 2 cavalos e 1 punho; 2 (amarela) 1 cavalo, 2 punhos
// e o Khan; 3 (branca) 2 cavalos e o Khan.
public class MelhoriasTest {

    // As 5 primeiras vão para a área, da esquerda para a direita; as outras ficam na pilha, nessa ordem.
    private static final List<String> MELHORIAS = List.of("cavalos-vermelha", "moeda-verde", "punho-amarela",
            "espada-vermelha", "seda-verde", "yurt-amarela", "boi-vermelha", "cavalos-verde");

    private static Jogo jogo() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        j.prepararCartas(List.of("yurts10", "cidades3", "regioes2", "yurts6"));
        j.prepararMelhorias(MELHORIAS);
        j.prepararBonus(List.of()); // sem peças de bônus: o trono daria 1 voto a mais nas entregas
        Partidas.escolherSecretas(j);
        return j;
    }

    private static void darMoedas(Jogo j, int jogador, int n) {
        for (int i = 0; i < n; i++) j.jogador(jogador).ganhar(Tributo.MOEDA);
    }

    // ESPACOS_COMPRAVEIS usa a mesma conferência da compra: sem moedas, ou com a coluna da cor cheia, não dá.
    @Test(timeout = 1000)
    public void espacosCompraveisSeguemAsRegrasDaCompra() {
        Jogo j = jogo();
        assertEquals("antes de ativar a coluna, nada é comprável", List.of(), j.get(ESPACOS_COMPRAVEIS));
        for (String id : List.of("punho-vermelha", "yurt-vermelha", "moeda-vermelha", "boi-vermelha"))
            j.jogador(0).adicionarMelhoria(0, Melhoria.porId(id)); // coluna vermelha cheia
        darMoedas(j, 0, 1); // 2 moedas
        j.ativarColuna(0, 1); // Tolui: a coluna verde não dá moeda virtual
        // Área: cavalos-vermelha, moeda-verde, punho-amarela, espada-vermelha, seda-verde; pilha: yurt-amarela.
        assertEquals("a coluna vermelha está cheia: só os espaços 1, 2, 4 e a pilha",
                List.of(1, 2, 4, Melhorias.ESPACOS), j.get(ESPACOS_COMPRAVEIS));
        IllegalStateException e = assertThrows("não compra melhoria vermelha com a coluna vermelha cheia",
                IllegalStateException.class, () -> j.comprarMelhoria(0, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("já tem 4"));
        j.comprarMelhoria(0, 1);
        assertEquals("sem moedas, nada é comprável", List.of(), j.get(ESPACOS_COMPRAVEIS));
    }

    // ---------------------------------------------------------------- peças e preparação

    // Regras, p. 16: 8 moedas, 5 tesouros (um de cada), 8 espadas, 8 yurts, 6 punhos e 9 de dois cavalos.
    @Test(timeout = 1000)
    public void jogoTem44Melhorias() {
        List<Melhoria> todas = Melhorias.todas();
        assertEquals("44 melhorias", 44, todas.size());
        Map<Melhoria.Tipo, Integer> porTipo = new EnumMap<>(Melhoria.Tipo.class);
        for (Melhoria m : todas) {
            porTipo.merge(m.getTipo(), 1, Integer::sum);
            assertNotEquals(m.getId() + ": nenhuma melhoria é branca", Cor.BRANCA, m.getCor());
        }
        assertEquals("8 de moeda", Integer.valueOf(8), porTipo.get(Melhoria.Tipo.MOEDA));
        assertEquals("5 de tesouro", Integer.valueOf(5), porTipo.get(Melhoria.Tipo.TESOURO));
        assertEquals("8 de espada", Integer.valueOf(8), porTipo.get(Melhoria.Tipo.ESPADA));
        assertEquals("8 de yurt", Integer.valueOf(8), porTipo.get(Melhoria.Tipo.YURT));
        assertEquals("6 de punho", Integer.valueOf(6), porTipo.get(Melhoria.Tipo.PUNHO));
        assertEquals("9 de cavalos", Integer.valueOf(9), porTipo.get(Melhoria.Tipo.CAVALOS));
        for (Tesouro t : Tesouro.values()) {
            int n = 0;
            for (Melhoria m : todas)
                if (m.getTesouro() == t) n++;
            assertEquals(t.getNome() + ": 1 tesouro virtual de cada tipo", 1, n);
        }
    }

    @Test(timeout = 1000)
    public void partidaComeca5MelhoriasNaArea() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        assertEquals("44 - 5 na área", 39, (int) j.get(MELHORIAS_NA_PILHA));
        for (String id : j.get(AREA_MELHORIAS)) assertNotNull("a área começa cheia", id);
        for (int c = 0; c < 4; c++) assertEquals("coluna " + c + ": Tolui começa sem melhorias",
                List.of(), j.get(ObservadoIF.MELHORIAS).get(0).get(c));
    }

    @Test(timeout = 1000)
    public void idDaMelhoriaIdaEVolta() {
        for (Melhoria m : Melhorias.todas()) assertEquals(m.getId() + ": o id volta para a mesma melhoria",
                m, Melhoria.porId(m.getId()));
        assertEquals("o id da seda verde", "seda-verde", Melhoria.porId("seda-verde").getId());
        assertEquals("a seda verde é um tesouro virtual",
                Melhoria.Tipo.TESOURO, Melhoria.porId("seda-verde").getTipo());
        assertThrows("não existe ouro", IllegalArgumentException.class, () -> Melhoria.porId("ouro-verde"));
        assertThrows("não existe melhoria branca",
                IllegalArgumentException.class, () -> Melhoria.porId("moeda-branca"));
        assertThrows("o id precisa da cor", IllegalArgumentException.class, () -> Melhoria.porId("moeda"));
    }

    @Test(timeout = 1000)
    public void coresDasColunasERegioes() {
        assertEquals("vermelha é a China", Regiao.CHINA, Cor.VERMELHA.getRegiao());
        assertEquals("verde é a Pérsia", Regiao.PERSIA, Cor.VERDE.getRegiao());
        assertEquals("amarela é a Rússia", Regiao.RUSSIA, Cor.AMARELA.getRegiao());
        assertNull("a branca não tem região", Cor.BRANCA.getRegiao());
        for (int c = 0; c < 4; c++) assertEquals("coluna " + c, c, Cor.daColuna(c).coluna());
        assertThrows("a coluna 5 não existe", IllegalArgumentException.class, () -> Cor.daColuna(4));
    }

    // ---------------------------------------------------------------- compra

    // Regras, p. 11 e 16: 2 moedas por melhoria. A moeda virtual da coluna vale 1 moeda e é gasta primeiro.
    @Test(timeout = 1000)
    public void compraComMoedaVirtualEMoedaReal() {
        Jogo j = jogo();
        j.ativarColuna(0, 0); // Tolui: coluna vermelha, com a moeda virtual
        assertEquals("a coluna vermelha do Tolui tem moeda virtual", 1, (int) j.get(VIRTUAIS).get("moeda"));
        j.comprarMelhoria(0, 2); // punho-amarela
        assertEquals("a moeda virtual foi gasta primeiro", 0, (int) j.get(VIRTUAIS).get("moeda"));
        assertEquals("e a 1 moeda real", 0, (int) j.get(MOEDAS).get(0));
        assertEquals("vai para a coluna da sua cor",
                List.of("punho-amarela"), j.get(ObservadoIF.MELHORIAS).get(0).get(2));
        // As melhorias da esquerda andam para a direita; nenhuma nova é revelada.
        assertEquals("as melhorias da esquerda andam para a direita; nenhuma nova é revelada",
                java.util.Arrays.asList(null, "cavalos-vermelha", "moeda-verde", "espada-vermelha", "seda-verde"),
                j.get(AREA_MELHORIAS));
        assertEquals("a pilha não muda", 3, (int) j.get(MELHORIAS_NA_PILHA));
    }

    @Test(timeout = 1000)
    public void compraSemMoedasERecusada() {
        Jogo j = jogo();
        j.ativarColuna(0, 1); // sem moeda virtual; Tolui tem 1 moeda
        IllegalStateException e = assertThrows("1 moeda não compra",
                IllegalStateException.class, () -> j.comprarMelhoria(0, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("Melhorias.comprar"));
        assertTrue(e.getMessage(), e.getMessage().contains("2 moedas"));
        assertEquals("nada foi pago", 1, (int) j.get(MOEDAS).get(0));
        assertEquals("a melhoria continua na área", "cavalos-vermelha", j.get(AREA_MELHORIAS).get(0));
    }

    @Test(timeout = 1000)
    public void compraSoDepoisDeAtivarUmaColunaENaVez() {
        Jogo j = jogo();
        darMoedas(j, 0, 3);
        assertThrows("não compra antes de ativar a coluna", IllegalStateException.class, () -> j.comprarMelhoria(0, 0));
        j.ativarColuna(0, 1);
        assertThrows("não é a vez de Altani", IllegalStateException.class, () -> j.comprarMelhoria(1, 0));
        assertThrows("o espaço 6 não existe", IllegalArgumentException.class, () -> j.comprarMelhoria(0, 6));
    }

    @Test(timeout = 1000)
    public void compraDoTopoDaPilha() {
        Jogo j = jogo();
        darMoedas(j, 0, 1);
        j.ativarColuna(0, 1);
        j.comprarMelhoria(0, Melhorias.ESPACOS); // yurt-amarela, a primeira da pilha
        assertEquals("yurt-amarela, a 1ª da pilha, vai para a coluna amarela",
                List.of("yurt-amarela"), j.get(ObservadoIF.MELHORIAS).get(0).get(2));
        assertEquals("a pilha tinha 3", 2, (int) j.get(MELHORIAS_NA_PILHA));
        assertEquals("a área não muda", MELHORIAS.subList(0, 5), j.get(AREA_MELHORIAS));
    }

    @Test(timeout = 1000)
    public void espacoVazioEPilhaVaziaSaoRecusados() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        j.prepararMelhorias(List.of("moeda-verde"));
        Partidas.escolherSecretas(j);
        darMoedas(j, 0, 5);
        j.ativarColuna(0, 1);
        assertThrows("o espaço 3 está vazio", IllegalStateException.class, () -> j.comprarMelhoria(0, 3));
        assertThrows("a pilha está vazia", IllegalStateException.class, () -> j.comprarMelhoria(0, Melhorias.ESPACOS));
        j.comprarMelhoria(0, 0);
        assertEquals("a área ficou vazia",
                java.util.Arrays.asList(null, null, null, null, null), j.get(AREA_MELHORIAS));
    }

    // Cada coluna tem 4 espaços de melhoria.
    @Test(timeout = 1000)
    public void colunaCheiaNaoRecebeMaisMelhorias() {
        Jogo j = jogo();
        Jogador tolui = j.jogador(0);
        for (int i = 0; i < 4; i++) tolui.adicionarMelhoria(0, new Melhoria(Melhoria.Tipo.PUNHO, Cor.VERMELHA));
        darMoedas(j, 0, 3);
        j.ativarColuna(0, 1);
        IllegalStateException e = assertThrows("a coluna vermelha está cheia",
                IllegalStateException.class, () -> j.comprarMelhoria(0, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("vermelha"));
        assertEquals("nada foi pago", 4, (int) j.get(MOEDAS).get(0));
        j.comprarMelhoria(0, 1); // moeda-verde cabe
        assertEquals("a moeda verde cabe na coluna verde",
                List.of("moeda-verde"), j.get(ObservadoIF.MELHORIAS).get(0).get(1));
    }

    // ---------------------------------------------------------------- efeitos (regras, p. 16)

    @Test(timeout = 1000)
    public void melhoriasSomamAcoesAColunaAtivada() {
        Jogo j = jogo();
        Jogador tolui = j.jogador(0);
        tolui.adicionarMelhoria(1, Melhoria.porId("cavalos-verde"));
        tolui.adicionarMelhoria(1, Melhoria.porId("punho-verde"));
        tolui.adicionarMelhoria(1, Melhoria.porId("espada-verde"));
        tolui.adicionarMelhoria(1, Melhoria.porId("seda-verde"));
        j.ativarColuna(0, 1);
        assertEquals("2 movimentos da cartela + 2", 4, (int) j.get(MOVIMENTOS));
        assertEquals("1 punho da cartela + 1", 2, (int) j.get(PUNHOS));
        assertEquals("a espada virtual", 1, (int) j.get(VIRTUAIS).get("espada"));
        assertEquals("sem moeda virtual", 0, (int) j.get(VIRTUAIS).get("moeda"));
        assertEquals("a seda virtual", List.of("seda"), j.get(TESOUROS_VIRTUAIS));
        assertNull("só yurt, espada e moeda são virtuais", j.get(VIRTUAIS).get("ouro"));
    }

    // Regras, p. 11: a melhoria comprada não vale no turno da compra.
    @Test(timeout = 1000)
    public void melhoriaCompradaSoValeNoProximoTurno() {
        Jogo j = jogo();
        darMoedas(j, 0, 1);
        j.ativarColuna(0, 0);
        j.comprarMelhoria(0, 0); // cavalos-vermelha, na coluna ativada
        assertEquals("os cavalos comprados não valem no turno da compra", 1, (int) j.get(MOVIMENTOS));
        j.encerrarTurno(0);
        j.ativarColuna(1, 1);
        j.encerrarTurno(1);
        j.ativarColuna(0, 3); // coluna branca: libera a vermelha
        j.moverKhan(0, 0);
        j.encerrarTurno(0);
        j.ativarColuna(1, 0);
        j.moverKhan(1, 1);
        j.encerrarTurno(1);
        j.ativarColuna(0, 0);
        assertEquals("1 cavalo da cartela + 2", 3, (int) j.get(MOVIMENTOS));
    }

    @Test(timeout = 1000)
    public void espadaVirtualAtacaCidade() {
        Jogo j = jogo();
        j.prepararCidades(List.of("Sarai", "Kiev", "Pequim", "Bagdá", "Cabul", "Moscou", "Samarcanda", "Kaesong",
                "Cantão"), List.of(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE));
        j.jogador(0).adicionarMelhoria(1, Melhoria.porId("espada-verde"));
        j.ativarColuna(0, 1);
        j.mover(0, "r9"); // ao lado de Sarai
        assertEquals("Tolui não tem espada real", 0, (int) j.get(ESPADAS).get(0));
        assertEquals("a espada virtual basta para atacar Sarai", List.of("Sarai"), j.get(CIDADES_ATACAVEIS));
        j.atacarCidade(0, "Sarai", "boi");
        assertEquals("a espada virtual foi gasta", 0, (int) j.get(VIRTUAIS).get("espada"));
        assertEquals("ganhou o boi", 1, (int) j.get(TESOUROS).get(0).get("boi"));
        assertEquals("o 2º tesouro custaria 2 espadas", List.of(), j.get(CIDADES_ATACAVEIS));
    }

    @Test(timeout = 1000)
    public void espadaVirtualEGastaAntesDaReal() {
        Jogo j = jogo();
        j.prepararCidades(List.of("Sarai", "Kiev", "Pequim", "Bagdá", "Cabul", "Moscou", "Samarcanda", "Kaesong",
                "Cantão"), List.of(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE));
        j.jogador(0).adicionarMelhoria(1, Melhoria.porId("espada-verde"));
        j.jogador(0).ganhar(Tributo.ESPADA);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        j.atacarCidade(0, "Sarai", "boi");
        assertEquals("a espada virtual é gasta antes: a real sobrou", 1, (int) j.get(ESPADAS).get(0));
    }

    @Test(timeout = 1000)
    public void yurtVirtualConstroiYurt() {
        Jogo j = jogo();
        j.jogador(0).adicionarMelhoria(1, Melhoria.porId("yurt-verde"));
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        assertEquals("Tolui não tem peça de yurt", 0, (int) j.get(PECAS_YURT).get(0));
        assertTrue("o yurt virtual basta para construir", j.get(PARADAS_PARA_YURT).contains("r9"));
        j.construirYurt(0, "r9");
        assertEquals("o yurt em r9", List.of(0), j.get(YURTS_NAS_PARADAS).get("r9"));
        assertEquals("o yurt virtual foi gasto", 0, (int) j.get(VIRTUAIS).get("yurt"));
        assertEquals("não há mais yurt para construir", List.of(), j.get(PARADAS_PARA_YURT));
    }

    // Regras, p. 16: o tesouro virtual vale numa entrega, no lugar de 1 dos tesouros reais.
    @Test(timeout = 1000)
    public void tesouroVirtualCompletaAEntrega() {
        Jogo j = jogo();
        Jogador tolui = j.jogador(0);
        tolui.adicionarMelhoria(1, Melhoria.porId("boi-vermelha")); // a cor não importa aqui
        tolui.ganharTesouro(Tesouro.FERRO);
        tolui.ganharTesouro(Tesouro.PELE);
        j.ativarColuna(0, 1);
        assertEquals("Karakorum pede ferro, pele e boi", List.of("karakorum"), j.get(CONSELHEIROS_PARA_ENTREGA));
        j.entregarTesouros(0, "karakorum");
        assertEquals("os 2 votos de Karakorum", 2, (int) j.get(VOTOS).get(0));
        assertEquals("o boi virtual foi gasto", List.of(), j.get(TESOUROS_VIRTUAIS));
        assertEquals("o ferro real foi gasto", 0, (int) j.get(TESOUROS).get(0).get("ferro"));
    }

    // C1 (regras, p. 16: "uma única entrega com um tesouro a menos"): no máximo 1 tesouro virtual por entrega.
    @Test(timeout = 1000)
    public void entregaUsaNoMaximoUmTesouroVirtual() {
        Conselheiro karakorum = new Mapa().getConselheiro(Mapa.KARAKORUM); // pede ferro, pele e boi
        Jogador j = new Jogador(Herdeiro.TOLUI, 1);
        j.setPosicao(new Mapa().getParada(Mapa.KARAKORUM));
        for (String id : List.of("ferro-amarela", "pele-vermelha", "boi-vermelha"))
            j.adicionarMelhoria(1, Melhoria.porId(id));
        Turno t = new Turno(j, 1);
        assertNull("só virtuais", Kurultai.entrega(karakorum, j, t));
        j.ganharTesouro(Tesouro.FERRO);
        assertNull("1 real e 2 virtuais", Kurultai.entrega(karakorum, j, t));
        j.ganharTesouro(Tesouro.PELE);
        Kurultai.Entrega e = Kurultai.entrega(karakorum, j, t);
        assertEquals("ferro e pele reais", List.of(Tesouro.FERRO, Tesouro.PELE), e.reais());
        assertEquals("o boi virtual", List.of(Tesouro.BOI), e.virtuais());
    }

    // No conselheiro de 3 tesouros quaisquer, o virtual entra mesmo quando o jogador tem reais de sobra.
    @Test(timeout = 1000)
    public void tesouroVirtualEntraNoConselheiroDeTresQuaisquer() {
        Conselheiro c8 = new Mapa().getConselheiro("c8");
        Jogador j = new Jogador(Herdeiro.TOLUI, 1);
        j.adicionarMelhoria(1, Melhoria.porId("boi-vermelha"));
        for (int i = 0; i < 3; i++) j.ganharTesouro(Tesouro.SEDA);
        Turno t = new Turno(j, 1);
        Kurultai.Entrega e = Kurultai.entrega(c8, j, t);
        assertEquals("2 sedas reais", List.of(Tesouro.SEDA, Tesouro.SEDA), e.reais());
        assertEquals("o boi virtual no lugar da 3ª seda", List.of(Tesouro.BOI), e.virtuais());
    }

    // Regras, p. 18: os tesouros virtuais contam para as cartas de conselheiro aberto.
    @Test(timeout = 1000)
    public void tesourosVirtuaisContamParaAsCartas() {
        Jogo j = jogo();
        j.prepararCartas(List.of("tesouros4", "cidades3", "regioes2", "yurts6"));
        Jogador tolui = j.jogador(0);
        tolui.ganharTesouro(Tesouro.SEDA);
        tolui.ganharTesouro(Tesouro.SEDA);
        tolui.adicionarMelhoria(1, Melhoria.porId("boi-vermelha"));
        tolui.adicionarMelhoria(1, Melhoria.porId("pele-vermelha"));
        j.ativarColuna(0, 1);
        j.pegarTributo(0, "t5"); // qualquer ação confere as cartas
        assertEquals("2 sedas reais e 2 virtuais: a carta de 4 tesouros",
                0, (int) j.get(DONOS_CARTAS).get("tesouros4"));
    }

    // ---------------------------------------------------------------- linha completa (regras, p. 12)

    private static Jogo toluiQuaseComLinha() {
        Jogo j = jogo();
        Jogador tolui = j.jogador(0);
        tolui.adicionarMelhoria(0, Melhoria.porId("punho-vermelha"));
        tolui.adicionarMelhoria(1, Melhoria.porId("punho-verde"));
        darMoedas(j, 0, 3);
        j.ativarColuna(0, 1);
        return j;
    }

    @Test(timeout = 1000)
    public void linhaCompletaDaMelhoriaGratisNaColunaBranca() {
        Jogo j = toluiQuaseComLinha();
        j.comprarMelhoria(0, 2); // punho-amarela completa a 1ª linha
        assertEquals("completar a 1ª linha dá 1 melhoria grátis", 1, (int) j.get(MELHORIAS_GRATIS));
        IllegalStateException e = assertThrows("não encerra o turno sem escolher a grátis",
                IllegalStateException.class, () -> j.encerrarTurno(0));
        assertTrue(e.getMessage(), e.getMessage().contains("grátis"));
        assertThrows("primeiro a escolha grátis", IllegalStateException.class, () -> j.comprarMelhoria(0, 0));
        // seda-verde vai para a coluna branca, de qualquer cor
        assertEquals("a escolha avisa MELHORIA_GRATIS",
                List.of(Evento.MELHORIA_GRATIS), Partidas.eventos(j, () -> j.escolherMelhoriaGratis(0, 4)));
        assertEquals("a melhoria escolhida", "seda-verde", j.get(ULTIMA_MELHORIA));
        assertEquals("o espaço dela", 4, (int) j.get(ESPACO_ULTIMA_MELHORIA));
        assertEquals("a coluna branca", 3, (int) j.get(COLUNA_ULTIMA_MELHORIA));
        assertEquals("seda-verde vai para a coluna branca, de qualquer cor",
                List.of("seda-verde"), j.get(ObservadoIF.MELHORIAS).get(0).get(3));
        assertEquals("não há mais grátis", 0, (int) j.get(MELHORIAS_GRATIS));
        assertEquals("a grátis não custa moedas", 2, (int) j.get(MOEDAS).get(0));
        assertThrows("só 1 grátis por linha", IllegalStateException.class, () -> j.escolherMelhoriaGratis(0, 0));
        j.encerrarTurno(0);
    }

    @Test(timeout = 1000)
    public void cadaLinhaSoDaUmaMelhoriaGratis() {
        Jogo j = toluiQuaseComLinha();
        j.comprarMelhoria(0, 2);
        j.escolherMelhoriaGratis(0, Melhorias.ESPACOS);
        j.comprarMelhoria(0, 1); // cavalos-vermelha: a 2ª linha ainda não está completa
        assertEquals("a 2ª linha ainda não está completa", 0, (int) j.get(MELHORIAS_GRATIS));
        assertEquals("1 linha premiada", 1, j.jogador(0).getLinhasPremiadas());
    }

    @Test(timeout = 1000)
    public void semMelhoriasParaEscolherOTurnoPodeAcabar() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        j.prepararMelhorias(List.of("punho-amarela"));
        Partidas.escolherSecretas(j);
        j.jogador(0).adicionarMelhoria(0, Melhoria.porId("punho-vermelha"));
        j.jogador(0).adicionarMelhoria(1, Melhoria.porId("punho-verde"));
        darMoedas(j, 0, 1);
        j.ativarColuna(0, 1);
        j.comprarMelhoria(0, 0);
        assertEquals("não sobrou nenhuma melhoria", 0, (int) j.get(MELHORIAS_GRATIS));
        j.encerrarTurno(0);
    }

    // ---------------------------------------------------------------- Khan na área de melhorias (regras, p. 9)

    @Test(timeout = 1000)
    public void khanNaAreaPreencheOsEspacosEDaMoedaEEspada() {
        Jogo j = jogo();
        darMoedas(j, 0, 3);
        j.ativarColuna(0, 2); // coluna amarela: Khan
        j.comprarMelhoria(0, 0);
        j.comprarMelhoria(0, 1); // os espaços 0 e 1 (o da moeda) ficam vazios
        assertEquals("os espaços 0 e 1 (o da moeda) ficam vazios",
                java.util.Arrays.asList(null, null, "punho-amarela", "espada-vermelha", "seda-verde"),
                j.get(AREA_MELHORIAS));
        assertEquals("gastou as 4 moedas", 0, (int) j.get(MOEDAS).get(0));
        j.moverKhan(0, Tributos.AREA);
        assertEquals("o Khan na área", Tributos.AREA, (int) j.get(POSICAO_KHAN));
        assertEquals("o espaço da moeda estava vazio: ganhou 1 moeda", 1, (int) j.get(MOEDAS).get(0));
        assertEquals("o Khan deu a moeda", List.of("moeda"), j.get(GANHOS_DO_KHAN));
        assertEquals("o espaço da espada não estava vazio", 0, (int) j.get(ESPADAS).get(0));
        assertEquals("os espaços vazios recebem as 2 primeiras da pilha",
                List.of("yurt-amarela", "boi-vermelha", "punho-amarela", "espada-vermelha", "seda-verde"),
                j.get(AREA_MELHORIAS));
        assertEquals("a pilha tinha 3", 1, (int) j.get(MELHORIAS_NA_PILHA));
        assertEquals("a área não tem províncias", List.of(), j.get(PROVINCIAS_KHAN).get(Tributos.AREA));
    }

    @Test(timeout = 1000)
    public void khanNaAreaSemPilhaSoDaOsTributos() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        j.prepararMelhorias(List.of("moeda-verde", "punho-amarela")); // espaços 2, 3 e 4 vazios
        Partidas.escolherSecretas(j);
        j.ativarColuna(0, 2);
        j.moverKhan(0, Tributos.AREA);
        assertEquals("o espaço da espada estava vazio", 1, (int) j.get(ESPADAS).get(0));
        assertEquals("o espaço da moeda está ocupado", 1, (int) j.get(MOEDAS).get(0));
        assertEquals("o Khan deu só a espada", List.of("espada"), j.get(GANHOS_DO_KHAN));
        assertEquals("sem pilha, a área não muda",
                java.util.Arrays.asList("moeda-verde", "punho-amarela", null, null, null), j.get(AREA_MELHORIAS));
    }

    @Test(timeout = 1000)
    public void khanNaoFicaNaAreaDeMelhorias() {
        Jogo j = jogo();
        j.ativarColuna(0, 2);
        j.moverKhan(0, Tributos.AREA);
        j.encerrarTurno(0);
        j.ativarColuna(1, 0); // Altani: coluna vermelha com Khan
        IllegalArgumentException e = assertThrows("o Khan não fica na área de melhorias 2 vezes seguidas",
                IllegalArgumentException.class, () -> j.moverKhan(1, Tributos.AREA));
        assertTrue(e.getMessage(), e.getMessage().contains("área de melhorias"));
        j.moverKhan(1, 0);
    }

    @Test(timeout = 1000)
    public void areaDeMelhoriasEmPontoDoTabuleiro() {
        Jogo j = jogo();
        for (int i = 0; i <= Melhorias.ESPACOS; i++) {
            int[] r = j.get(RETANGULOS_MELHORIAS).get(i);
            assertEquals("espaço " + i, i, j.espacoMelhoriaEm(r[0], r[1]));
            assertNull("espaço " + i + ": não é parada", j.paradaEm(r[0], r[1]));
            assertNull("espaço " + i + ": não é província", j.provinciaEm(r[0], r[1]));
            assertEquals("espaço " + i + ": não é lugar do Khan", -1, j.localKhanEm(r[0], r[1]));
        }
        int[] k = j.get(PONTOS_KHAN).get(Tributos.AREA);
        assertEquals("a cabeça do Khan na área", Tributos.AREA, j.localKhanEm(k[0], k[1]));
        assertEquals("a cabeça do Khan não é espaço de melhoria", -1, j.espacoMelhoriaEm(k[0], k[1]));
        assertEquals("o canto não é espaço", -1, j.espacoMelhoriaEm(10, 10));
        assertEquals("4 províncias do Khan e a área", 5, j.get(PONTOS_KHAN).size());
    }

    // Observer: a compra avisa os observadores com o evento MELHORIA.
    @Test(timeout = 1000)
    public void compraAvisaOsObservadores() {
        Jogo j = jogo();
        darMoedas(j, 0, 1);
        j.ativarColuna(0, 1);
        assertEquals("a compra avisa MELHORIA",
                List.of(Evento.MELHORIA), Partidas.eventos(j, () -> j.comprarMelhoria(0, 0)));
        assertEquals("a do espaço 0 vai para a coluna vermelha", "cavalos-vermelha", j.get(ULTIMA_MELHORIA));
        assertEquals("o espaço 0", 0, (int) j.get(ESPACO_ULTIMA_MELHORIA));
        assertEquals("a coluna vermelha", 0, (int) j.get(COLUNA_ULTIMA_MELHORIA));
    }
}
