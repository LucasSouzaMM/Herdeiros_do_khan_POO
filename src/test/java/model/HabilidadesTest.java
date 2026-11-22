package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

// Habilidades dos herdeiros (texto das cartelas e regras, p. 20).
public class HabilidadesTest {
    // Sarai fica no anel r8, r9 e r10; Karakorum é vizinha de r9, r11, p4 e c4.
    private static final List<String> CIDADES = List.of("Sarai", "Kiev", "Pequim", "Bagdá", "Cabul", "Moscou",
            "Samarcanda", "Kaesong", "Cantão");

    private static Jogo jogo(String... herdeiros) {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of(herdeiros));
        j.prepararCartas(List.of("yurts10", "cidades3", "regioes2", "yurts6"));
        j.prepararBonus(List.of());
        Partidas.escolherSecretas(j);
        return j;
    }

    // ---------------------------------------------------------------- Altani: 2 piões

    @Test(timeout = 1000)
    public void altaniTem2PioesEmKarakorum() {
        Jogo j = jogo("Altani", "Tolui");
        assertEquals("Altani tem 2 piões", 2, j.get(PEOES).get(0).size());
        assertEquals("Tolui tem 1 pião", 1, j.get(PEOES).get(1).size());
        assertEquals("os 2 piões começam em Karakorum", List.of("karakorum", "karakorum"), j.get(PEOES).get(0));
        assertEquals("começa com o pião 1", 0, (int) j.get(PEOES_ATIVOS).get(0));
    }

    @Test(timeout = 1000)
    public void altaniEscolheOPiaoAntesDeAtivarAColuna() {
        Jogo j = jogo("Altani", "Tolui");
        assertTrue("Altani pode trocar de pião antes da coluna", j.get(PODE_TROCAR_PEAO));
        j.selecionarPeao(0, 1);
        assertEquals("o pião 2 ficou ativo", 1, (int) j.get(PEOES_ATIVOS).get(0));
        j.ativarColuna(0, 1); // 2 cavalos
        assertFalse("depois da coluna, não troca mais", j.get(PODE_TROCAR_PEAO));
        IllegalStateException e = assertThrows("não troca de pião depois de ativar a coluna",
                IllegalStateException.class, () -> j.selecionarPeao(0, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("antes de ativar"));
        j.mover(0, "r9");
        assertEquals("só o pião 2 andou", List.of("karakorum", "r9"), j.get(PEOES).get(0));
        assertEquals("a posição é a do pião ativo", "r9", j.get(POSICOES).get(0));
        j.encerrarTurno(0);
        j.ativarColuna(1, 0);
        j.encerrarTurno(1);
        j.selecionarPeao(0, 0);
        j.ativarColuna(0, 2); // 1 cavalo
        assertEquals("o caminho começa no pião 1", List.of("karakorum"), j.get(VISITADAS));
        j.mover(0, "r11");
        assertEquals("o pião 1 andou até r11", List.of("r11", "r9"), j.get(PEOES).get(0));
    }

    @Test(timeout = 1000)
    public void soAAltaniTrocaDePiao() {
        Jogo j = jogo("Tolui", "Altani");
        assertFalse("Tolui não troca de pião", j.get(PODE_TROCAR_PEAO));
        assertThrows("Tolui não tem 2º pião", IllegalStateException.class, () -> j.selecionarPeao(0, 1));
        assertThrows("não é a vez de Altani", IllegalStateException.class, () -> j.selecionarPeao(1, 1));
        j.ativarColuna(0, 1);
        j.encerrarTurno(0);
        assertThrows("o pião 3 não existe", IllegalArgumentException.class, () -> j.selecionarPeao(1, 2));
    }

    // §7.1: o outro pião da Altani também ocupa a parada.
    @Test(timeout = 1000)
    public void outroPiaoDaAltaniOcupaAParada() {
        Jogo j = jogo("Altani", "Tolui");
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        j.encerrarTurno(0);
        j.ativarColuna(1, 0);
        j.encerrarTurno(1);
        j.selecionarPeao(0, 1);
        j.ativarColuna(0, 2); // 1 cavalo
        IllegalArgumentException e = assertThrows("o outro pião da Altani ocupa r9",
                IllegalArgumentException.class, () -> j.mover(0, "r9"));
        assertTrue(e.getMessage(), e.getMessage().contains("ocupada"));
    }

    @Test(timeout = 1000)
    public void desempateUsaOPiaoDaAltaniMaisPertoDeKarakorum() {
        Jogo j = jogo("Altani", "Tolui");
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        j.mover(0, "r8");
        assertEquals("o pião 2 continua em Karakorum", 0, (int) j.get(DISTANCIAS_KARAKORUM).get(0));
    }

    // ---------------------------------------------------------------- Tolui: yurt em Karakorum e yurts vizinhos

    @Test(timeout = 1000)
    public void toluiComecaComUmYurtEmKarakorumSemRegiao() {
        Jogo j = jogo("Tolui", "Altani");
        assertEquals("o yurt do Tolui em Karakorum", List.of(0), j.get(YURTS_NAS_PARADAS).get("karakorum"));
        assertEquals("1 dos 12 yurts já está em Karakorum", 11, (int) j.get(YURTS_RESTANTES).get(0));
        for (int c = 0; c < 3; c++) assertEquals("região " + c + ": Karakorum não tem região",
                0, (int) j.get(YURTS_NAS_REGIOES).get(0).get(c));
        assertEquals("r9 sem yurts", List.of(), j.get(YURTS_NAS_PARADAS).get("r9"));
    }

    // Regras, p. 7: os próprios yurts podem ser pulados; o de Karakorum serve para isso.
    @Test(timeout = 1000)
    public void yurtDeKarakorumAjudaOMovimentoDoTolui() {
        Jogo j = jogo("Tolui", "Altani");
        j.ativarColuna(0, 1); // 2 cavalos
        j.mover(0, "r9");
        assertTrue("Tolui pula o próprio yurt de Karakorum até p4", j.get(DESTINOS).contains("p4"));
        j.mover(0, "p4"); // 1 movimento só, por cima de Karakorum
        assertEquals("Tolui chegou em p4", "p4", j.get(POSICOES).get(0));
        assertEquals("1 movimento por cima de Karakorum e 1 até r9", 0, (int) j.get(MOVIMENTOS));
    }

    @Test(timeout = 1000)
    public void toluiConstroiNasParadasVizinhasAoPiao() {
        Jogo j = jogo("Tolui", "Altani");
        j.jogador(0).ganhar(Tributo.YURT);
        j.jogador(0).ganhar(Tributo.YURT);
        j.ativarColuna(0, 1);
        assertEquals("Tolui pode construir nas vizinhas do pião",
                Set.of("r9", "r11", "p4", "c4"), new HashSet<>(j.get(PARADAS_PARA_YURT)));
        j.construirYurt(0, "r11"); // sem ter passado por lá
        assertEquals("o yurt em r11, sem ter passado por lá", List.of(0), j.get(YURTS_NAS_PARADAS).get("r11"));
        assertThrows("r8 fica longe do pião", IllegalArgumentException.class, () -> j.construirYurt(0, "r8"));
    }

    @Test(timeout = 1000)
    public void outrosHerdeirosSoConstroemOndeEstiveram() {
        Jogo j = jogo("Altani", "Tolui");
        j.jogador(0).ganhar(Tributo.YURT);
        j.ativarColuna(0, 1);
        assertEquals("Altani não esteve em nenhuma parada", List.of(), j.get(PARADAS_PARA_YURT));
        IllegalArgumentException e = assertThrows("Altani não constrói onde não esteve",
                IllegalArgumentException.class, () -> j.construirYurt(0, "r9"));
        assertTrue(e.getMessage(), e.getMessage().contains("não esteve"));
    }

    // ---------------------------------------------------------------- Jochi: 2 melhorias no começo

    @Test(timeout = 1000)
    public void jochiComecaCom2MelhoriasUmaNaColunaBranca() {
        Jogo j = jogo("Jochi", "Tolui");
        int coloridas = 0;
        for (int c = 0; c < 3; c++) {
            for (String id : j.get(MELHORIAS).get(0).get(c)) assertTrue(id + ": na coluna da sua cor",
                    id.endsWith(corDaColuna(c)));
            coloridas += j.get(MELHORIAS).get(0).get(c).size();
        }
        assertEquals("1 melhoria numa coluna colorida", 1, coloridas);
        assertEquals("1 melhoria na coluna branca", 1, j.get(MELHORIAS).get(0).get(3).size());
        assertEquals("44 melhorias, 5 na área e 2 do Jochi", 44 - 5 - 2, (int) j.get(MELHORIAS_NA_PILHA));
        for (int c = 0; c < 4; c++) assertEquals("coluna " + c + ": Tolui não ganha melhorias",
                List.of(), j.get(MELHORIAS).get(1).get(c));
    }

    private static String corDaColuna(int c) {
        return List.of("vermelha", "verde", "amarela").get(c);
    }

    // Regras, p. 20: ao completar a 1ª linha, o Jochi não ganha a melhoria grátis.
    @Test(timeout = 1000)
    public void jochiNaoGanhaAMelhoriaGratisDaPrimeiraLinha() {
        Jogo j = jogo("Jochi", "Tolui");
        Jogador jochi = j.jogador(0);
        int falta = -1;
        for (int c = 0; c < 3; c++)
            if (jochi.getMelhorias(c).isEmpty()) {
                if (falta == -1) falta = c;
                else jochi.adicionarMelhoria(c, new Melhoria(Melhoria.Tipo.PUNHO, Cor.daColuna(c)));
            }
        j.prepararMelhorias(List.of("punho-" + corDaColuna(falta), "moeda-verde"));
        for (int i = 0; i < 2; i++) jochi.ganhar(Tributo.MOEDA);
        j.ativarColuna(0, 1);
        j.comprarMelhoria(0, 0); // completa a 1ª linha
        assertEquals("Jochi completou a 1ª linha", 1, jochi.linhasCompletas());
        assertEquals("a 1ª linha não dá melhoria grátis ao Jochi", 0, (int) j.get(MELHORIAS_GRATIS));
    }

    // ---------------------------------------------------------------- Chagatai: 1 cavalo por 1 espada

    @Test(timeout = 1000)
    public void chagataiTroca1MovimentoPor1Espada() {
        Jogo j = jogo("Chagatai", "Tolui");
        assertFalse("antes de ativar a coluna, não troca", j.get(PODE_TROCAR_CAVALO_POR_ESPADA));
        j.ativarColuna(0, 2); // 2 cavalos
        assertTrue("com a coluna, a Chagatai troca", j.get(PODE_TROCAR_CAVALO_POR_ESPADA));
        assertEquals("a troca avisa CAVALO_POR_ESPADA",
                List.of(Evento.CAVALO_POR_ESPADA), Partidas.eventos(j, () -> j.trocarCavaloPorEspada(0)));
        assertEquals("2 movimentos - 1", 1, (int) j.get(MOVIMENTOS));
        assertEquals("ganhou 1 espada", 1, (int) j.get(ESPADAS).get(0));
        assertFalse("1 troca por turno", j.get(PODE_TROCAR_CAVALO_POR_ESPADA));
        IllegalStateException e = assertThrows("não troca 2 vezes no turno",
                IllegalStateException.class, () -> j.trocarCavaloPorEspada(0));
        assertTrue(e.getMessage(), e.getMessage().contains("já trocou"));
        j.encerrarTurno(0); // a coluna amarela do Chagatai não tem Khan
        j.ativarColuna(1, 1);
        assertThrows("só a Chagatai troca", IllegalStateException.class, () -> j.trocarCavaloPorEspada(1));
        assertFalse("Tolui não troca", j.get(PODE_TROCAR_CAVALO_POR_ESPADA));
    }

    @Test(timeout = 1000)
    public void chagataiPrecisaDeUmMovimentoSobrando() {
        Jogo j = jogo("Chagatai", "Tolui");
        j.ativarColuna(0, 0); // 1 cavalo
        j.mover(0, "r9");
        IllegalStateException e = assertThrows("sem movimento sobrando, não troca",
                IllegalStateException.class, () -> j.trocarCavaloPorEspada(0));
        assertTrue(e.getMessage(), e.getMessage().contains("movimento"));
    }

    // Encontrado nas partidas simuladas: parado numa parada ocupada, o Chagatai não pode trocar o último movimento,
    // senão ficaria preso ali sem poder encerrar o turno.
    @Test(timeout = 1000)
    public void chagataiNaoTrocaOUltimoMovimentoNumaParadaOcupada() {
        Jogo j = jogo("Tolui", "Chagatai");
        j.ativarColuna(0, 0); // 1 cavalo
        j.mover(0, "r9");
        j.encerrarTurno(0);
        j.ativarColuna(1, 2); // 2 cavalos
        j.mover(1, "r9"); // de passagem: Tolui está lá
        assertFalse("parada ocupada: não troca o último movimento", j.get(PODE_TROCAR_CAVALO_POR_ESPADA));
        IllegalStateException e = assertThrows("não troca o último movimento em parada ocupada",
                IllegalStateException.class, () -> j.trocarCavaloPorEspada(1));
        assertTrue(e.getMessage(), e.getMessage().contains("ocupada"));
        j.mover(1, "r8");
        j.encerrarTurno(1);
    }

    // ---------------------------------------------------------------- Ogedei: tesouro com moeda

    private static Jogo ogedeiComSarai(List<Tesouro> saco) {
        Jogo j = jogo("Ogedei", "Tolui");
        j.prepararCidades(CIDADES, saco);
        return j;
    }

    @Test(timeout = 1000)
    public void ogedeiPaga1MoedaETomaUmTesouroDeLonge() {
        Jogo j = ogedeiComSarai(List.of(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE));
        j.ativarColuna(0, 0); // sem moeda virtual; Ogedei tem 1 moeda
        assertEquals("Karakorum não fica no anel de Sarai", List.of(), j.get(CIDADES_ATACAVEIS));
        assertTrue("Ogedei pode tomar com moeda", j.get(PODE_TOMAR_TESOURO_COM_MOEDA));
        assertTrue("Sarai está revelada e tem 2 ou mais tesouros", j.get(CIDADES_PARA_MOEDA).contains("Sarai"));
        for (String c : j.get(CIDADES_PARA_MOEDA)) assertTrue(c + ": 2 ou mais tesouros",
                j.get(TESOUROS_CIDADES).get(c).size() >= 2);
        assertEquals("a tomada avisa TESOURO_COM_MOEDA",
                List.of(Evento.TESOURO_COM_MOEDA), Partidas.eventos(j, () -> j.tomarTesouroComMoeda(0, "Sarai", "seda")));
        assertEquals("uma vez por turno", List.of(), j.get(CIDADES_PARA_MOEDA));
        assertEquals("a última cidade", "Sarai", j.get(ULTIMA_CIDADE));
        assertEquals("o último tesouro", "seda", j.get(ULTIMO_TESOURO));
        assertEquals("pagou a moeda", 0, (int) j.get(MOEDAS).get(0));
        assertEquals("ganhou a seda", 1, (int) j.get(TESOUROS).get(0).get("seda"));
        assertEquals("Sarai ficou com 3", List.of("boi", "ferro", "pele"), j.get(TESOUROS_CIDADES).get("Sarai"));
        assertFalse("não toma de novo no turno", j.get(PODE_TOMAR_TESOURO_COM_MOEDA));
        j.jogador(0).ganhar(Tributo.MOEDA);
        IllegalStateException e = assertThrows("não toma 2 vezes no turno",
                IllegalStateException.class, () -> j.tomarTesouroComMoeda(0, "Sarai", "boi"));
        assertTrue(e.getMessage(), e.getMessage().contains("já tomou"));
    }

    // Regras, p. 20: depois da moeda, se estiver ao lado da cidade, o próximo tesouro sai com 1 espada.
    @Test(timeout = 1000)
    public void depoisDaMoedaOProximoTesouroCusta1Espada() {
        Jogo j = ogedeiComSarai(List.of(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE));
        j.jogador(0).ganhar(Tributo.ESPADA);
        j.ativarColuna(0, 0); // 2 cavalos
        j.tomarTesouroComMoeda(0, "Sarai", "boi");
        j.mover(0, "r9");
        j.atacarCidade(0, "Sarai", "seda");
        assertEquals("o 1º ataque do turno em Sarai custa 1 espada", 0, (int) j.get(ESPADAS).get(0));
        assertEquals("Sarai ficou com 2", 2, j.get(TESOUROS_CIDADES).get("Sarai").size());
    }

    @Test(timeout = 1000)
    public void ogedeiNaoTomaOUltimoTesouroComMoeda() {
        Jogo j = ogedeiComSarai(List.of(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE, Tesouro.PELE));
        j.ativarColuna(0, 0);
        IllegalStateException e = assertThrows("Kiev tem 1 tesouro só",
                IllegalStateException.class, () -> j.tomarTesouroComMoeda(0, "Kiev", "pele"));
        assertTrue(e.getMessage(), e.getMessage().contains("2 ou mais"));
        assertThrows("Moscou não está revelada",
                IllegalStateException.class, () -> j.tomarTesouroComMoeda(0, "Moscou", "pele"));
        assertThrows("Sarai não tem especiaria",
                IllegalArgumentException.class, () -> j.tomarTesouroComMoeda(0, "Sarai", "especiaria"));
    }

    @Test(timeout = 1000)
    public void ogedeiUsaAMoedaVirtual() {
        Jogo j = ogedeiComSarai(List.of(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE));
        j.jogador(0).gastar(Tributo.MOEDA, 1);
        j.ativarColuna(0, 1); // a coluna verde do Ogedei tem a moeda virtual
        j.tomarTesouroComMoeda(0, "Sarai", "ferro");
        assertEquals("a moeda virtual foi usada", 0, (int) j.get(VIRTUAIS).get("moeda"));
        assertEquals("ganhou o ferro", 1, (int) j.get(TESOUROS).get(0).get("ferro"));
    }

    @Test(timeout = 1000)
    public void soOOgedeiTomaTesouroComMoeda() {
        Jogo j = jogo("Tolui", "Ogedei");
        j.ativarColuna(0, 0);
        assertFalse("Tolui não toma com moeda", j.get(PODE_TOMAR_TESOURO_COM_MOEDA));
        String cidade = null;
        for (String c : List.copyOf(j.get(PONTOS_CIDADES).keySet()))
            if (j.get(CIDADES_REVELADAS).contains(c)) cidade = c;
        String alvo = cidade, tesouro = j.get(TESOUROS_CIDADES).get(cidade).get(0);
        assertThrows("só o Ogedei toma com moeda",
                IllegalStateException.class, () -> j.tomarTesouroComMoeda(0, alvo, tesouro));
    }

    // Observer: a troca de pião avisa os observadores com o evento PEAO.
    @Test(timeout = 1000)
    public void trocaDePiaoAvisaOsObservadores() {
        Jogo j = jogo("Altani", "Tolui");
        assertEquals("a troca de pião avisa PEAO",
                List.of(Evento.PEAO), Partidas.eventos(j, () -> j.selecionarPeao(0, 1)));
    }
}
