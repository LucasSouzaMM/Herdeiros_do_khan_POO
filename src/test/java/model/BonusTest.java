package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

// Peças de bônus (regras, p. 4, 13 e 17) e as 5 cartas de conselheiro aberto que dependem de bônus e de melhorias
// (regras, p. 18). Tolui é o jogador 0, começa em Karakorum e tem: coluna 0 com 1 cavalo e 1 punho, coluna 1 com
// 2 cavalos e 1 punho, coluna 2 com 1 cavalo, 2 punhos e o Khan.
public class BonusTest {

    // Os conselheiros são, na ordem de getConselheiros(): r4, r11, p1, p8, c7, c8 e karakorum.
    private static final List<String> BONUS = List.of("espadas", "moedas", "yurts", "punhos", "cavalos-punho", "bau",
            "trono");

    private static Jogo jogo() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        j.prepararCartas(List.of("yurts10", "cidades3", "regioes2", "yurts6"));
        j.prepararBonus(BONUS);
        Partidas.escolherSecretas(j);
        return j;
    }

    private static void darTesourosDeKarakorum(Jogo j) {
        j.jogador(0).ganharTesouro(Tesouro.FERRO);
        j.jogador(0).ganharTesouro(Tesouro.PELE);
        j.jogador(0).ganharTesouro(Tesouro.BOI);
    }

    // ---------------------------------------------------------------- preparação

    @Test(timeout = 1000)
    public void cadaConselheiroComecaComUmaPecaDiferente() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        Set<String> pecas = new HashSet<>();
        for (String c : List.copyOf(j.get(PEDIDOS_CONSELHEIROS).keySet())) {
            assertNotNull(c, j.get(BONUS_NOS_CONSELHEIROS).get(c));
            pecas.add(j.get(BONUS_NOS_CONSELHEIROS).get(c));
            assertEquals(c + ": o ponto da peça tem x e y", 2, j.get(PONTOS_BONUS).get(c).length);
        }
        assertEquals("as 7 peças, uma em cada conselheiro",
                new HashSet<>(List.copyOf(Jogo.getInstance().get(DESCRICOES_BONUS).keySet())), pecas);
        assertEquals("Tolui começa sem peças", List.of(), j.get(ObservadoIF.BONUS).get(0));
    }

    @Test(timeout = 1000)
    public void idDaPecaDeBonus() {
        for (String id : List.copyOf(Jogo.getInstance().get(DESCRICOES_BONUS).keySet())) {
            assertEquals("porId devolve a peça do id", id, Bonus.porId(id).getId());
            assertFalse(id + ": tem descrição", Jogo.getInstance().get(DESCRICOES_BONUS).get(id).isEmpty());
        }
        IllegalArgumentException e = assertThrows("peça que não existe é recusada",
                IllegalArgumentException.class, () -> Bonus.porId("coroa"));
        assertTrue(e.getMessage(), e.getMessage().contains("Bonus.porId"));
    }

    // O espaço do bônus fica no quadrado roxo de cada conselheiro, fora das paradas e das províncias. O de Karakorum fica
    // dentro do quadro de Karakorum, que é a área de clique da parada.
    @Test(timeout = 1000)
    public void espacoDoBonusNoTabuleiro() {
        Jogo j = jogo();
        for (String c : List.copyOf(j.get(PEDIDOS_CONSELHEIROS).keySet())) {
            int[] p = j.get(PONTOS_BONUS).get(c);
            if (!c.equals("karakorum")) assertNull(c, j.paradaEm(p[0], p[1])); // o de Karakorum fica dentro dela
            assertNull(c, j.provinciaEm(p[0], p[1]));
        }
    }

    // ---------------------------------------------------------------- quem ganha a peça (regras, p. 13)

    @Test(timeout = 1000)
    public void primeiroAEntregarLevaAPecaEOTronoDaUmVoto() {
        Jogo j = jogo();
        darTesourosDeKarakorum(j);
        j.ativarColuna(0, 1);
        j.entregarTesouros(0, "karakorum"); // 2 votos da entrega + 1 do trono
        assertEquals("2 votos da entrega e 1 do trono", 3, (int) j.get(VOTOS).get(0));
        assertEquals("Tolui leva o trono de Karakorum", List.of("trono"), j.get(ObservadoIF.BONUS).get(0));
        assertTrue("o trono vale na hora", j.get(BONUS_USADOS).get(0).contains("trono"));
        assertNull("a peça saiu de Karakorum", j.get(BONUS_NOS_CONSELHEIROS).get("karakorum"));
        darTesourosDeKarakorum(j);
        j.entregarTesouros(0, "karakorum"); // a peça já foi levada: só os 2 votos
        assertEquals("a 2ª entrega só dá os 2 votos", 5, (int) j.get(VOTOS).get(0));
        IllegalStateException e = assertThrows("o trono não é usado depois",
                IllegalStateException.class, () -> j.usarBonus(0, "trono"));
        assertTrue(e.getMessage(), e.getMessage().contains("já usou"));
    }

    @Test(timeout = 1000)
    public void quemEntregaDepoisNaoGanhaAPeca() {
        Jogo j = jogo();
        darTesourosDeKarakorum(j);
        j.ativarColuna(0, 1);
        j.entregarTesouros(0, "karakorum");
        j.encerrarTurno(0);
        j.jogador(1).ganharTesouro(Tesouro.FERRO);
        j.jogador(1).ganharTesouro(Tesouro.PELE);
        j.jogador(1).ganharTesouro(Tesouro.BOI);
        j.ativarColuna(1, 1);
        j.entregarTesouros(1, "karakorum");
        assertEquals("Altani ganha os 2 votos da entrega", 2, (int) j.get(VOTOS).get(1));
        assertEquals("Altani não ganha a peça", List.of(), j.get(ObservadoIF.BONUS).get(1));
    }

    // ---------------------------------------------------------------- uso (regras, p. 17)

    @Test(timeout = 1000)
    public void espadasDao2EspadasUmaVezSo() {
        Jogo j = jogo();
        j.jogador(0).ganharBonus(Bonus.ESPADAS);
        assertThrows("não usa a peça antes de ativar a coluna",
                IllegalStateException.class, () -> j.usarBonus(0, "espadas"));
        assertEquals("antes da coluna, nenhuma peça usável", List.of(), j.get(BONUS_USAVEIS));
        j.ativarColuna(0, 1);
        assertEquals("depois da coluna, as espadas são usáveis", List.of("espadas"), j.get(BONUS_USAVEIS));
        j.usarBonus(0, "espadas");
        assertEquals("a peça usada não é mais usável", List.of(), j.get(BONUS_USAVEIS));
        assertEquals("ganhou 2 espadas", 2, (int) j.get(ESPADAS).get(0));
        assertTrue("as espadas estão nas usadas", j.get(BONUS_USADOS).get(0).contains("espadas"));
        IllegalStateException e = assertThrows("não usa a peça 2 vezes",
                IllegalStateException.class, () -> j.usarBonus(0, "espadas"));
        assertTrue(e.getMessage(), e.getMessage().contains("PecasBonus.usar"));
        assertEquals("a peça usada continua com o jogador", List.of("espadas"), j.get(ObservadoIF.BONUS).get(0));
    }

    @Test(timeout = 1000)
    public void naoUsaPecaQueNaoTem() {
        Jogo j = jogo();
        j.ativarColuna(0, 1);
        IllegalArgumentException e = assertThrows("não usa a peça que não tem",
                IllegalArgumentException.class, () -> j.usarBonus(0, "moedas"));
        assertTrue(e.getMessage(), e.getMessage().contains("não tem"));
        assertThrows("peça que não existe é recusada", IllegalArgumentException.class, () -> j.usarBonus(0, "coroa"));
    }

    @Test(timeout = 1000)
    public void tributosDoBonusSaemDoEstoqueLimitado() {
        Jogo j = jogo();
        for (int i = 0; i < 5; i++) j.jogador(0).ganhar(Tributo.ESPADA); // 4 nas províncias + 5 = só 1 no estoque
        assertEquals("4 espadas nas províncias e 5 com Tolui: só 1 no estoque", 1, (int) j.get(ESTOQUE).get("espada"));
        j.jogador(0).ganharBonus(Bonus.ESPADAS);
        j.ativarColuna(0, 1);
        j.usarBonus(0, "espadas");
        assertEquals("5 espadas + 1 do estoque", 6, (int) j.get(ESPADAS).get(0));
        assertEquals("o estoque de espadas acabou", 0, (int) j.get(ESTOQUE).get("espada"));
    }

    @Test(timeout = 1000)
    public void moedasEYurtsDao2Pecas() {
        Jogo j = jogo();
        j.jogador(0).ganharBonus(Bonus.MOEDAS);
        j.jogador(0).ganharBonus(Bonus.YURTS);
        j.ativarColuna(0, 1);
        j.usarBonus(0, "moedas");
        j.usarBonus(0, "yurts");
        assertEquals("1 moeda inicial + 2", 3, (int) j.get(MOEDAS).get(0));
        assertEquals("2 peças de yurt", 2, (int) j.get(PECAS_YURT).get(0));
    }

    @Test(timeout = 1000)
    public void cavalosEPunhosSomamAoTurno() {
        Jogo j = jogo();
        j.jogador(0).ganharBonus(Bonus.CAVALOS_PUNHO);
        j.jogador(0).ganharBonus(Bonus.PUNHOS);
        j.ativarColuna(0, 0); // 1 cavalo e 1 punho
        j.usarBonus(0, "cavalos-punho");
        assertEquals("1 cavalo + 3", 4, (int) j.get(MOVIMENTOS));
        assertEquals("1 punho + 1", 2, (int) j.get(PUNHOS));
        j.usarBonus(0, "punhos");
        assertEquals("mais 2 punhos", 4, (int) j.get(PUNHOS));
    }

    @Test(timeout = 1000)
    public void bauSorteia3TesourosEOJogadorFicaCom1() throws Exception {
        Jogo j = jogo();
        List<Tesouro> saco = new ArrayList<>();
        for (int i = 0; i < 12; i++) saco.add(Tesouro.BOI); // os 12 das 3 cidades reveladas
        saco.addAll(List.of(Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE));
        j.prepararCidades(List.of("Sarai", "Kiev", "Pequim", "Bagdá", "Cabul", "Moscou", "Samarcanda", "Kaesong",
                "Cantão"), saco);
        j.jogador(0).ganharBonus(Bonus.BAU);
        j.jogador(0).ganharBonus(Bonus.PUNHOS);
        j.ativarColuna(0, 1);
        j.usarBonus(0, "bau");
        assertEquals("os 3 primeiros do saco", List.of("seda", "ferro", "pele"), j.get(TESOUROS_BAU));
        assertEquals("com o baú aberto, nenhuma outra peça é usável", List.of(), j.get(BONUS_USAVEIS));
        IllegalStateException e = assertThrows("não encerra o turno com o baú aberto",
                IllegalStateException.class, () -> j.encerrarTurno(0));
        assertTrue(e.getMessage(), e.getMessage().contains("baú"));
        assertThrows("primeiro a escolha do baú", IllegalStateException.class, () -> j.usarBonus(0, "punhos"));
        assertThrows("o baú não tem boi", IllegalArgumentException.class, () -> j.escolherTesouroBau(0, "boi"));
        assertEquals("a escolha avisa BAU",
                List.of(Evento.BAU), Partidas.eventos(j, () -> j.escolherTesouroBau(0, "ferro")));
        assertEquals("Tolui ficou com o ferro", "ferro", j.get(TESOURO_DO_BAU));
        assertEquals("com o baú fechado, a peça de punhos é usável", List.of("punhos"), j.get(BONUS_USAVEIS));
        assertEquals("Tolui ganhou o ferro", 1, (int) j.get(TESOUROS).get(0).get("ferro"));
        assertEquals("a seda não ficou com Tolui", 0, (int) j.get(TESOUROS).get(0).get("seda"));
        assertEquals("o baú fechou", List.of(), j.get(TESOUROS_BAU));
        assertThrows("não escolhe de novo", IllegalStateException.class, () -> j.escolherTesouroBau(0, "seda"));
        // C3 (regras, p. 17: "Descarte os outros"): seda e pele saem do jogo, e o saco fica vazio.
        Path arquivo = Files.createTempFile("bau", ".txt");
        j.salvar(arquivo);
        assertTrue("seda e pele saem do jogo: o saco fica vazio",
                Files.readAllLines(arquivo).contains("cidades.saco="));
        Files.delete(arquivo);
        j.encerrarTurno(0);
    }

    // ---------------------------------------------------------------- cartas novas (regras, p. 18)

    @Test(timeout = 1000)
    public void jogoTem14CartasDeConselheiroAberto() {
        assertEquals("14 cartas abertas", 14, CartaAberta.values().length);
        for (String id : List.of("bonus2", "bonus3", "linha", "colunas2", "coluna4"))
            assertEquals("porId devolve a carta do id", id, CartaAberta.porId(id).getId());
    }

    // Qualquer ação confere as cartas; aqui é pegar 1 tributo de t5, ao lado de Karakorum.
    private static Jogo comCarta(String carta) {
        Jogo j = jogo();
        j.prepararCartas(List.of(carta, "yurts10", "cidades3", "regioes2"));
        return j;
    }

    @Test(timeout = 1000)
    public void cartaDe2PecasDeBonus() {
        Jogo j = comCarta("bonus2");
        j.jogador(0).ganharBonus(Bonus.ESPADAS);
        j.ativarColuna(0, 1);
        j.pegarTributo(0, "t5");
        assertEquals("1 peça não basta", -1, (int) j.get(DONOS_CARTAS).get("bonus2"));
        j.jogador(0).ganharBonus(Bonus.MOEDAS);
        j.usarBonus(0, "moedas"); // a peça usada também conta
        assertEquals("a peça usada também conta", 0, (int) j.get(DONOS_CARTAS).get("bonus2"));
        assertEquals("o voto da carta", 1, (int) j.get(VOTOS).get(0));
    }

    @Test(timeout = 1000)
    public void cartaDe3PecasDeBonus() {
        Jogo j = comCarta("bonus3");
        for (Bonus b : List.of(Bonus.ESPADAS, Bonus.MOEDAS, Bonus.YURTS)) j.jogador(0).ganharBonus(b);
        j.ativarColuna(0, 1);
        j.pegarTributo(0, "t5");
        assertEquals("3 peças de bônus", 0, (int) j.get(DONOS_CARTAS).get("bonus3"));
    }

    @Test(timeout = 1000)
    public void cartaDaLinhaDeMelhoriasCompleta() {
        Jogo j = comCarta("linha");
        j.jogador(0).adicionarMelhoria(0, Melhoria.porId("punho-vermelha"));
        j.jogador(0).adicionarMelhoria(1, Melhoria.porId("punho-verde"));
        j.ativarColuna(0, 1);
        j.pegarTributo(0, "t5");
        assertEquals("2 melhorias não fazem a linha", -1, (int) j.get(DONOS_CARTAS).get("linha"));
        j.jogador(0).adicionarMelhoria(2, Melhoria.porId("punho-amarela"));
        j.jogador(0).ganharBonus(Bonus.PUNHOS);
        j.usarBonus(0, "punhos"); // outra ação, para conferir as cartas de novo
        assertEquals("a linha de 3 melhorias", 0, (int) j.get(DONOS_CARTAS).get("linha"));
    }

    @Test(timeout = 1000)
    public void cartaDe2ColunasCom2Melhorias() {
        Jogo j = comCarta("colunas2");
        Jogador tolui = j.jogador(0);
        tolui.adicionarMelhoria(0, Melhoria.porId("punho-vermelha"));
        tolui.adicionarMelhoria(0, Melhoria.porId("moeda-vermelha"));
        tolui.adicionarMelhoria(3, Melhoria.porId("punho-verde")); // a coluna branca não conta
        tolui.adicionarMelhoria(3, Melhoria.porId("moeda-verde"));
        j.ativarColuna(0, 1);
        j.pegarTributo(0, "t5");
        assertEquals("a coluna branca não conta", -1, (int) j.get(DONOS_CARTAS).get("colunas2"));
        tolui.adicionarMelhoria(2, Melhoria.porId("punho-amarela"));
        tolui.adicionarMelhoria(2, Melhoria.porId("moeda-amarela"));
        tolui.ganharBonus(Bonus.PUNHOS);
        j.usarBonus(0, "punhos"); // outra ação, para conferir as cartas de novo
        assertEquals("as colunas vermelha e amarela com 2 melhorias", 0, (int) j.get(DONOS_CARTAS).get("colunas2"));
    }

    @Test(timeout = 1000)
    public void cartaDe4MelhoriasNumaColuna() {
        Jogo j = comCarta("coluna4");
        for (int i = 0; i < 3; i++) j.jogador(0).adicionarMelhoria(1, Melhoria.porId("cavalos-verde"));
        j.ativarColuna(0, 0);
        j.pegarTributo(0, "t5");
        assertEquals("3 melhorias não bastam", -1, (int) j.get(DONOS_CARTAS).get("coluna4"));
        j.jogador(0).adicionarMelhoria(1, Melhoria.porId("cavalos-verde"));
        j.jogador(0).ganhar(Tributo.MOEDA);
        j.jogador(0).ganharBonus(Bonus.PUNHOS);
        j.usarBonus(0, "punhos");
        assertEquals("4 melhorias na coluna verde", 0, (int) j.get(DONOS_CARTAS).get("coluna4"));
    }

    // ---------------------------------------------------------------- Observer

    @Test(timeout = 1000)
    public void ganharEUsarBonusAvisaOsObservadores() {
        Jogo j = jogo();
        darTesourosDeKarakorum(j);
        j.jogador(0).ganharBonus(Bonus.PUNHOS);
        j.ativarColuna(0, 1);
        assertEquals("entregar e usar avisam CONSELHEIRO, BONUS_GANHO e BONUS_USADO",
                List.of(Evento.CONSELHEIRO, Evento.BONUS_GANHO, Evento.BONUS_USADO), Partidas.eventos(j, () -> {
            j.entregarTesouros(0, "karakorum");
            j.usarBonus(0, "punhos");
        }));
        assertEquals("a última peça usada", "punhos", j.get(ULTIMO_BONUS));
    }
}
