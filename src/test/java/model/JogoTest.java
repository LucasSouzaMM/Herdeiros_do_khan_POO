package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import model.ObservadoIF.Chave;
import model.ObservadoIF.Evento;

// Os índices começam em 0: o "jogador 1" de teste_herdeiros.md é o índice 0.
// As colunas vão de 0 a 3, da esquerda para a direita na cartela: vermelha, verde, amarela e branca.
// Jogo é um Singleton: cada teste começa uma partida nova na mesma instância.
public class JogoTest {

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    private static Jogo jogo(int n) {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani", "Jochi", "Chagatai", "Ogedei").subList(0, n));
        j.prepararCartas(List.of("yurts10", "cidades3", "regioes2", "yurts6")); // cartas que os testes não cumprem por acaso
        return j;
    }

    // Encerra o turno, movendo antes o Khan quando a coluna manda (§7.3).
    private static void fim(Jogo j, int jogador) {
        if (j.get(KHAN_PENDENTE)) j.moverKhan(jogador, (int) j.get(POSICAO_KHAN) == 0 ? 1 : 0);
        j.encerrarTurno(jogador);
    }

    // T1
    @Test
    public void criaPartidaCom2Jogadores() {
        Jogo j = jogo(2);
        assertEquals(2, j.get(NOMES).size());
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
        assertEquals("Tolui", j.get(NOMES).get(0));
        assertEquals("Altani", j.get(NOMES).get(1));
    }

    // Singleton (cap. 17)
    @Test
    public void haUmaSoInstancia() {
        assertSame(Jogo.getInstance(), Jogo.getInstance());
        assertSame(Jogo.getInstance(), jogo(2));
    }

    @Test
    public void novaPartidaZeraOEstadoAnterior() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        j.novaPartida(List.of("Ogedei", "Jochi", "Altani"));
        assertEquals(3, j.get(NOMES).size());
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
        assertEquals(-1, (int) j.get(COLUNA_ATIVADA));
        assertEquals("karakorum", j.get(POSICOES).get(0));
        assertFalse(j.get(COLUNAS_BLOQUEADAS).get(0).contains(1));
    }

    @Test
    public void partidaInvalidaNaoApagaAAnterior() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        assertThrows(IllegalArgumentException.class, () -> j.novaPartida(List.of("Tolui")));
        assertEquals(2, j.get(NOMES).size());
        assertEquals(1, (int) j.get(COLUNA_ATIVADA));
    }

    // T2, CA01
    @Test
    public void rejeitaMenosDe2Jogadores() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Jogo.validarNumeroJogadores(-1));
        assertTrue(e.getMessage().contains("Jogo.validarNumeroJogadores"));
        assertTrue(e.getMessage().contains("mínimo: 2"));
        assertTrue(e.getMessage().contains("máximo: 5"));
        assertThrows(IllegalArgumentException.class, () -> jogo(1));
    }

    // T6, CA02
    @Test
    public void rejeitaMaisDe5Jogadores() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Jogo.getInstance().novaPartida(List.of("Tolui", "Altani", "Jochi", "Chagatai", "Ogedei", "Tolui")));
        assertTrue(e.getMessage().contains("mínimo: 2"));
        assertTrue(e.getMessage().contains("máximo: 5"));
    }

    // T3
    @Test
    public void passarTurnoPassaParaOProximo() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        assertEquals(1, (int) j.get(JOGADOR_ATUAL));
        j.ativarColuna(1, 0);
        fim(j, 1);
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
    }

    // T4, CA03
    @Test
    public void acaoForaDoTurnoFalha() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.ativarColuna(0, 1));
        assertTrue(e.getMessage().contains("Jogo.ativarColuna"));
        assertEquals(-1, (int) j.get(COLUNA_ATIVADA));
    }

    // T5
    @Test
    public void acaoNoTurnoFunciona() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertEquals(2, (int) j.get(COLUNA_ATIVADA));
    }

    @Test
    public void soUmaColunaPorTurno() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertThrows(IllegalStateException.class, () -> j.ativarColuna(0, 1));
        fim(j, 0);
        assertEquals(-1, (int) j.get(COLUNA_ATIVADA));
        j.ativarColuna(1, 1);
        assertEquals(1, (int) j.get(COLUNA_ATIVADA));
    }

    // T7
    @Test
    public void naoPassaTurnoDeOutroJogador() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.encerrarTurno(1));
        assertTrue(e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
    }

    @Test
    public void rejeitaJogadorInexistente() {
        Jogo j = jogo(2);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.ativarColuna(2, 0));
        assertTrue(e.getMessage().contains("Jogo"));
    }

    // T8
    @Test
    public void moedasIniciaisCom3Jogadores() {
        Jogo j = jogo(3);
        assertEquals(1, (int) j.get(MOEDAS).get(0));
        assertEquals(1, (int) j.get(MOEDAS).get(1));
        assertEquals(2, (int) j.get(MOEDAS).get(2));
    }

    // T9
    @Test
    public void moedasIniciaisCom5Jogadores() {
        Jogo j = jogo(5);
        assertEquals(1, (int) j.get(MOEDAS).get(0));
        assertEquals(1, (int) j.get(MOEDAS).get(1));
        for (int i = 2; i < 5; i++) assertEquals(2, (int) j.get(MOEDAS).get(i));
    }

    // G5, G9: todos os piões começam em Karakorum (regras, §5).
    @Test
    public void pioesComecamEmKarakorum() {
        Jogo j = jogo(5);
        for (int i = 0; i < 5; i++) assertEquals("karakorum", j.get(POSICOES).get(i));
    }

    // J1
    @Test
    public void colunaDaOsCavalosDaCartelaDoHerdeiro() {
        Jogo j = jogo(2);
        assertEquals(0, (int) j.get(MOVIMENTOS));
        j.ativarColuna(0, 1); // coluna verde de Tolui: 2 cavalos
        assertEquals(2, (int) j.get(MOVIMENTOS));
        fim(j, 0);
        assertEquals(0, (int) j.get(MOVIMENTOS));
        j.ativarColuna(1, 0); // coluna vermelha de Altani: 1 cavalo
        assertEquals(1, (int) j.get(MOVIMENTOS));
    }

    // §7: punhos e Khan da coluna ativada.
    @Test
    public void colunaDaOsPunhosEOKhanDaCartela() {
        Jogo j = jogo(2);
        assertEquals(0, (int) j.get(PUNHOS));
        assertFalse(j.get(KHAN_PENDENTE));
        j.ativarColuna(0, 2); // coluna amarela de Tolui: 1 cavalo, 2 punhos e Khan
        assertEquals(1, (int) j.get(MOVIMENTOS));
        assertEquals(2, (int) j.get(PUNHOS));
        assertTrue(j.get(KHAN_PENDENTE));
        fim(j, 0);
        j.ativarColuna(1, 1); // coluna verde de Altani: 2 cavalos e 1 punho, sem Khan
        assertEquals(1, (int) j.get(PUNHOS));
        assertFalse(j.get(KHAN_PENDENTE));
        fim(j, 1);
        j.ativarColuna(0, (int) j.get(COLUNA_REATIVACAO)); // coluna branca: 2 cavalos e Khan, sem punhos
        assertEquals(2, (int) j.get(MOVIMENTOS));
        assertEquals(0, (int) j.get(PUNHOS));
        assertTrue(j.get(KHAN_PENDENTE));
    }

    // RF11, RF12: marcar a coluna na cartela e depois confirmar.
    @Test
    public void marcaEConfirmaColuna() {
        Jogo j = jogo(2);
        j.selecionarColuna(0, 2);
        assertEquals(2, (int) j.get(COLUNA_SELECIONADA));
        assertEquals(-1, (int) j.get(COLUNA_ATIVADA));
        j.selecionarColuna(0, 1); // troca a marcação
        assertEquals(1, (int) j.get(COLUNA_SELECIONADA));
        j.selecionarColuna(0, 1); // desmarca
        assertEquals(-1, (int) j.get(COLUNA_SELECIONADA));
        j.selecionarColuna(0, 0);
        j.confirmarColuna(0);
        assertEquals(0, (int) j.get(COLUNA_ATIVADA));
        assertEquals(-1, (int) j.get(COLUNA_SELECIONADA));
        assertThrows(IllegalStateException.class, () -> j.selecionarColuna(0, 1));
    }

    @Test
    public void naoMarcaColunaBloqueadaNemConfirmaSemMarcar() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.confirmarColuna(0));
        assertTrue(e.getMessage().contains("Jogo.confirmarColuna"));
        j.ativarColuna(0, 0);
        fim(j, 0);
        j.ativarColuna(1, 0);
        fim(j, 1);
        e = assertThrows(IllegalStateException.class, () -> j.selecionarColuna(0, 0));
        assertTrue(e.getMessage().contains("bloqueada"));
        assertEquals(-1, (int) j.get(COLUNA_SELECIONADA));
        assertThrows(IllegalStateException.class, () -> j.selecionarColuna(1, 2)); // não é a vez dele
        assertThrows(IllegalArgumentException.class, () -> j.selecionarColuna(0, 4));
    }

    // J2
    @Test
    public void naoEncerraTurnoSemAtivarColuna() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.encerrarTurno(0));
        assertTrue(e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
    }

    // J4, RF04
    @Test
    public void salvaECarregaPartida() throws Exception {
        Jogo j = jogo(3);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        fim(j, 0);
        j.ativarColuna(1, 1); // Altani: 2 cavalos
        j.mover(1, "r11");
        Path arquivo = pasta.newFile("partida.txt").toPath();
        j.salvar(arquivo);

        j.novaPartida(List.of("Ogedei", "Jochi"));
        assertSame(j, Jogo.carregar(arquivo)); // o Singleton recebe a partida salva
        assertEquals(3, j.get(NOMES).size());
        assertEquals(List.of("Tolui", "Altani", "Jochi"), List.of(j.get(NOMES).get(0), j.get(NOMES).get(1), j.get(NOMES).get(2)));
        assertEquals(List.of(1, 1, 2), List.of((int) j.get(MOEDAS).get(0), (int) j.get(MOEDAS).get(1), (int) j.get(MOEDAS).get(2)));
        assertEquals("r9", j.get(POSICOES).get(0));
        assertEquals("r11", j.get(POSICOES).get(1));
        assertEquals("karakorum", j.get(POSICOES).get(2));
        assertEquals(1, (int) j.get(JOGADOR_ATUAL));
        assertEquals(1, (int) j.get(COLUNA_ATIVADA));
        assertEquals(1, (int) j.get(MOVIMENTOS));
        assertTrue(j.get(COLUNAS_BLOQUEADAS).get(0).contains(0));
    }

    @Test
    public void carregarParadaInexistenteFalha() throws Exception {
        Path arquivo = pasta.newFile("parada.txt").toPath();
        jogo(2).salvar(arquivo);
        Files.writeString(arquivo, Files.readString(arquivo).replace("jogador0.posicao=karakorum", "jogador0.posicao=x1"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage().contains("x1"));
    }

    // Os movimentos abaixo começam em Karakorum, vizinha de r9, r11, c4 e p4. r9 é vizinha de r8 e de r10.
    // A coluna 0 (vermelha) dá 1 cavalo a Tolui, Altani e Jochi; a coluna 1 (verde) dá 2 a Tolui e Altani.

    // J3: o turno só passa quando o jogador clica em Próximo.
    @Test
    public void turnoNaoPassaQuandoOsMovimentosAcabam() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        j.mover(0, "r10");
        assertEquals("r10", j.get(POSICOES).get(0));
        assertEquals(0, (int) j.get(MOVIMENTOS));
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
        assertEquals(1, (int) j.get(COLUNA_ATIVADA));
        fim(j, 0);
        assertEquals(1, (int) j.get(JOGADOR_ATUAL));
    }

    // J8
    @Test
    public void moveParaParadaVizinha() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        assertEquals("r9", j.get(POSICOES).get(0));
        assertEquals(0, (int) j.get(MOVIMENTOS));
    }

    // §7.1: cada movimento leva a uma parada vizinha.
    @Test
    public void naoPulaParadas() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.mover(0, "r10"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
        assertTrue(e.getMessage().contains("vizinha"));
        assertEquals("karakorum", j.get(POSICOES).get(0));
        assertEquals(2, (int) j.get(MOVIMENTOS));
    }

    // J9, CA04
    @Test
    public void naoMoveAlemDosMovimentos() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.mover(0, "r8"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
        assertEquals("r9", j.get(POSICOES).get(0));
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
    }

    // J10
    @Test
    public void moveEmVariosCliques() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        assertEquals(1, (int) j.get(MOVIMENTOS));
        j.mover(0, "r8");
        assertEquals("r8", j.get(POSICOES).get(0));
        assertEquals(0, (int) j.get(MOVIMENTOS));
    }

    @Test
    public void guardaAsParadasVisitadasEMostraOsDestinos() {
        Jogo j = jogo(2);
        assertEquals(List.of(), j.get(VISITADAS));
        assertEquals(List.of(), j.get(DESTINOS));
        j.ativarColuna(0, 1);
        assertEquals(List.of("karakorum"), j.get(VISITADAS));
        assertEquals(Set.of("r9", "r11", "c4", "p4"), new HashSet<>(j.get(DESTINOS)));
        j.mover(0, "r9");
        j.mover(0, "karakorum"); // pode voltar
        assertEquals(List.of("karakorum", "r9"), j.get(VISITADAS));
        assertEquals(List.of(), j.get(DESTINOS));
        fim(j, 0);
        assertEquals(List.of(), j.get(VISITADAS));
    }

    // J11
    @Test
    public void passaPorParadaOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        fim(j, 0);
        j.ativarColuna(1, 1);
        assertTrue(j.get(DESTINOS).contains("r9")); // ainda sobra 1 movimento para sair de lá
        j.mover(1, "r9");
        assertEquals(Set.of("r6", "r8", "r10", "karakorum"), new HashSet<>(j.get(DESTINOS)));
        j.mover(1, "r8");
        assertEquals("r8", j.get(POSICOES).get(1));
    }

    // J12
    @Test
    public void naoTerminaEmParadaSimplesOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        fim(j, 0);
        j.ativarColuna(1, 0);
        assertFalse(j.get(DESTINOS).contains("r9"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.mover(1, "r9"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
        assertEquals("karakorum", j.get(POSICOES).get(1));
    }

    @Test
    public void naoEncerraTurnoEmParadaOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        fim(j, 0);
        j.ativarColuna(1, 1);
        j.mover(1, "r9");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.encerrarTurno(1));
        assertTrue(e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals(1, (int) j.get(JOGADOR_ATUAL));
        j.mover(1, "r10");
        fim(j, 1);
        assertEquals(0, (int) j.get(JOGADOR_ATUAL));
    }

    // J13
    @Test
    public void paradaDuplaAceitaDoisPioes() {
        Jogo j = jogo(3);
        j.ativarColuna(0, 0);
        j.mover(0, "r11");
        fim(j, 0);
        j.ativarColuna(1, 0);
        j.mover(1, "r11");
        assertEquals("r11", j.get(POSICOES).get(1));
        fim(j, 1);
        j.ativarColuna(2, 0);
        assertThrows(IllegalArgumentException.class, () -> j.mover(2, "r11"));
        assertEquals("karakorum", j.get(POSICOES).get(2));
    }

    // J15
    @Test
    public void naoMoveSemAtivarColuna() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.mover(0, "r9"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
        assertEquals("karakorum", j.get(POSICOES).get(0));
    }

    // CA03
    @Test
    public void naoMoveForaDoTurno() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertThrows(IllegalStateException.class, () -> j.mover(1, "r9"));
        assertEquals("karakorum", j.get(POSICOES).get(1));
    }

    @Test
    public void naoMoveParaAMesmaParadaNemParaParadaInexistente() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertThrows(IllegalArgumentException.class, () -> j.mover(0, "karakorum"));
        assertThrows(IllegalArgumentException.class, () -> j.mover(0, "x1"));
        assertEquals(1, (int) j.get(MOVIMENTOS));
    }

    // J17
    @Test
    public void paradaEmPontoDoTabuleiro() {
        Jogo j = jogo(2);
        assertEquals("r1", j.paradaEm(490, 255));
        assertNull(j.paradaEm(10, 10));
    }

    // Dados de que a interface precisa para desenhar os piões e o modo de depuração (RNF10: a view só fala com Jogo).
    @Test
    public void dadosDoMapaParaAInterface() {
        Jogo j = jogo(2);
        assertEquals(34, List.copyOf(j.get(PONTOS_PARADAS).keySet()).size());
        assertArrayEquals(new int[] {855, 170, 930, 250}, j.get(PONTOS_PARADAS).get("r4"));
        assertEquals(List.of("r1", "r6"), j.get(VIZINHAS_PARADAS).get("r4"));
        assertTrue((int) j.get(RAIOS_PARADAS).get("karakorum") > (int) j.get(RAIOS_PARADAS).get("r1"));
        assertEquals(List.of("Altani", "Jochi", "Chagatai", "Tolui", "Ogedei"), Jogo.getInstance().get(HERDEIROS));
    }

    @Test
    public void carregarArquivoInvalidoFalha() throws Exception {
        Path arquivo = pasta.newFile("ruim.txt").toPath();
        Files.writeString(arquivo, "jogadores=dois\n");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage().contains("Jogo.carregar"));
    }

    // J5
    @Test
    public void carregarHerdeiroInexistenteFalha() throws Exception {
        Path arquivo = pasta.newFile("herdeiro.txt").toPath();
        jogo(2).salvar(arquivo);
        Files.writeString(arquivo, Files.readString(arquivo).replace("Tolui", "Fulano"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage().contains("Fulano"));
        assertEquals("Tolui", Jogo.getInstance().get(NOMES).get(0)); // a partida anterior continua
    }

    // J6
    @Test
    public void rejeitaHerdeiroRepetido() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Jogo.getInstance().novaPartida(List.of("Tolui", "Altani", "Tolui")));
        assertTrue(e.getMessage().contains("Jogo.novaPartida"));
        assertTrue(e.getMessage().contains("Tolui"));
    }

    // J7, RF07
    @Test
    public void sorteiaHerdeirosDiferentes() {
        Jogo j = Jogo.getInstance();
        Set<List<String>> combinacoes = new HashSet<>();
        for (int k = 0; k < 20; k++) {
            j.sortear(3);
            assertEquals(3, j.get(NOMES).size());
            List<String> nomes = List.of(j.get(NOMES).get(0), j.get(NOMES).get(1), j.get(NOMES).get(2));
            assertEquals(3, new HashSet<>(nomes).size());
            combinacoes.add(nomes);
        }
        assertTrue(combinacoes.size() >= 2);
    }

    @Test
    public void sortearValidaNumeroDeJogadores() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Jogo.getInstance().sortear(6));
        assertTrue(e.getMessage().contains("máximo: 5"));
    }

    @Test
    public void colunaUsadaFicaBloqueada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        j.ativarColuna(1, 0);
        fim(j, 1);
        assertTrue(j.get(COLUNAS_BLOQUEADAS).get(0).contains(0));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.ativarColuna(0, 0));
        assertTrue(e.getMessage().contains("Jogo.ativarColuna"));
        assertEquals(-1, (int) j.get(COLUNA_ATIVADA));
    }

    @Test
    public void colunaDeReativacaoLiberaAsOutras() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        j.ativarColuna(1, 0);
        fim(j, 1);
        j.ativarColuna(0, (int) j.get(COLUNA_REATIVACAO));
        assertFalse(j.get(COLUNAS_BLOQUEADAS).get(0).contains(0));
        assertFalse(j.get(COLUNAS_BLOQUEADAS).get(0).contains((int) j.get(COLUNA_REATIVACAO)));
        fim(j, 0);
        j.ativarColuna(1, (int) j.get(COLUNA_REATIVACAO));
        fim(j, 1);
        j.ativarColuna(0, 0); // liberada de novo
        assertEquals(0, (int) j.get(COLUNA_ATIVADA));
    }

    // ---------------------------------------------------------------- tributos e Khan
    // Karakorum fica na borda das províncias t4 (yurt), t5 (moeda), t6 (espada) e t9 (yurt).
    // Tolui: coluna 0 = 1 cavalo e 1 punho; coluna 1 = 2 cavalos e 1 punho; coluna 2 = 1 cavalo, 2 punhos e Khan.

    // §5: 1 peça de tributo em cada província, e o Khan fora do tabuleiro.
    @Test
    public void provinciasComecamCom1Peca() {
        Jogo j = jogo(2);
        assertEquals(12, List.copyOf(j.get(PONTOS_PROVINCIAS).keySet()).size());
        for (String p : List.copyOf(j.get(PONTOS_PROVINCIAS).keySet())) assertEquals(p, 1, (int) j.get(PECAS_PROVINCIAS).get(p));
        assertEquals(-1, (int) j.get(POSICAO_KHAN));
        assertEquals(0, (int) j.get(ESPADAS).get(0));
        assertEquals(0, (int) j.get(PECAS_YURT).get(0));
    }

    // §7.2
    @Test
    public void pegaTributoDeProvinciaAdjacente() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertEquals(Set.of("t4", "t5", "t6", "t9"), new HashSet<>(j.get(PROVINCIAS_ALCANCAVEIS)));
        j.pegarTributo(0, "t5"); // moeda, antes de andar
        assertEquals(2, (int) j.get(MOEDAS).get(0));
        assertEquals(0, (int) j.get(PECAS_PROVINCIAS).get("t5"));
        assertEquals(0, (int) j.get(PUNHOS));
        assertEquals(List.of(), j.get(PROVINCIAS_ALCANCAVEIS));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.pegarTributo(0, "t6"));
        assertTrue(e.getMessage().contains("punhos"));
        assertEquals(1, (int) j.get(PECAS_PROVINCIAS).get("t6"));
    }

    @Test
    public void naoPegaTributoDeProvinciaNaoAdjacente() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.pegarTributo(0, "t1"));
        assertTrue(e.getMessage().contains("Tributos.pegar"));
        assertEquals(1, (int) j.get(PUNHOS));
        assertEquals(1, (int) j.get(PECAS_PROVINCIAS).get("t1"));
        assertThrows(IllegalArgumentException.class, () -> j.pegarTributo(0, "t99"));
    }

    // §7.2: vale qualquer parada onde o pião esteve no turno, antes, durante ou depois do movimento.
    @Test
    public void pegaTributoDasParadasDoCaminho() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "p4");
        j.mover(0, "p3");
        assertTrue(j.get(PROVINCIAS_ALCANCAVEIS).containsAll(List.of("t4", "t7", "t8", "t9")));
        j.pegarTributo(0, "t7"); // espada, adjacente a p3
        assertEquals(1, (int) j.get(ESPADAS).get(0));
        assertEquals(0, (int) j.get(PECAS_PROVINCIAS).get("t7"));
    }

    @Test
    public void naoPegaTributoDeProvinciaVaziaNemSemColuna() {
        Jogo j = jogo(2);
        assertThrows(IllegalStateException.class, () -> j.pegarTributo(0, "t5"));
        j.ativarColuna(0, 2); // 2 punhos
        j.pegarTributo(0, "t9");
        assertEquals(1, (int) j.get(PECAS_YURT).get(0));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.pegarTributo(0, "t9"));
        assertTrue(e.getMessage().contains("vazia"));
        assertEquals(1, (int) j.get(PUNHOS));
        assertThrows(IllegalStateException.class, () -> j.pegarTributo(1, "t4")); // não é a vez dele
    }

    // §7.3: o Khan é obrigatório quando a coluna tem o ícone dele.
    @Test
    public void khanObrigatorioDaTributosAs3Provincias() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertTrue(j.get(KHAN_PENDENTE));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.encerrarTurno(0));
        assertTrue(e.getMessage().contains("Khan"));
        assertEquals(List.of("t4", "t5", "t6"), j.get(PROVINCIAS_KHAN).get(1));
        j.moverKhan(0, 1);
        assertEquals(1, (int) j.get(POSICAO_KHAN));
        assertFalse(j.get(KHAN_PENDENTE));
        for (String p : List.of("t4", "t5", "t6")) assertEquals(2, (int) j.get(PECAS_PROVINCIAS).get(p));
        assertEquals(1, (int) j.get(PECAS_PROVINCIAS).get("t1"));
        j.encerrarTurno(0);
        assertEquals(1, (int) j.get(JOGADOR_ATUAL));
    }

    @Test
    public void khanNaoFicaOndeEstaNemSeMoveSemOIcone() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0); // sem Khan
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.moverKhan(0, 1));
        assertTrue(e.getMessage().contains("Tributos.moverKhan"));
        j.encerrarTurno(0);
        j.ativarColuna(1, 0); // Altani: coluna vermelha com Khan
        j.moverKhan(1, 2);
        assertThrows(IllegalStateException.class, () -> j.moverKhan(1, 3)); // só uma vez por turno
        j.encerrarTurno(1);
        j.ativarColuna(0, 2);
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> j.moverKhan(0, 2));
        assertTrue(e2.getMessage().contains("outra"));
        assertThrows(IllegalArgumentException.class, () -> j.moverKhan(0, 4));
        j.moverKhan(0, 3);
        assertEquals(3, (int) j.get(POSICAO_KHAN));
    }

    // §7.3: no máximo 3 peças por província.
    @Test
    public void provinciaGuardaNoMaximo3Pecas() {
        Jogo j = jogo(2);
        int[][] jogadas = {{0, 2, 0}, {1, 0, 1}, {0, 3, 0}, {1, 3, 1}, {0, 2, 0}}; // jogador, coluna, local do Khan
        for (int[] jogada : jogadas) {
            j.ativarColuna(jogada[0], jogada[1]);
            j.moverKhan(jogada[0], jogada[2]);
            j.encerrarTurno(jogada[0]);
        }
        for (String p : List.of("t1", "t2", "t3")) assertEquals(3, (int) j.get(PECAS_PROVINCIAS).get(p));
        for (String p : List.of("t4", "t5", "t6")) assertEquals(3, (int) j.get(PECAS_PROVINCIAS).get(p));
        assertEquals(1, (int) j.get(PECAS_PROVINCIAS).get("t7"));
    }

    @Test
    public void novaPartidaReiniciaTributosEKhan() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        j.pegarTributo(0, "t5");
        j.moverKhan(0, 0);
        jogo(2);
        assertEquals(-1, (int) j.get(POSICAO_KHAN));
        assertEquals(1, (int) j.get(PECAS_PROVINCIAS).get("t5"));
        assertEquals(1, (int) j.get(PECAS_PROVINCIAS).get("t1"));
        assertEquals(1, (int) j.get(MOEDAS).get(0));
    }

    @Test
    public void provinciaEKhanEmPontoDoTabuleiro() {
        Jogo j = jogo(2);
        for (String p : List.copyOf(j.get(PONTOS_PROVINCIAS).keySet())) {
            int[] c = j.get(PONTOS_PROVINCIAS).get(p);
            assertEquals(p, j.provinciaEm(c[0], c[1]));
            assertNull(j.paradaEm(c[0], c[1])); // o ícone não fica em cima de uma parada
        }
        for (int i = 0; i < j.get(PONTOS_KHAN).size(); i++) {
            int[] c = j.get(PONTOS_KHAN).get(i);
            assertEquals(i, j.localKhanEm(c[0], c[1]));
            assertNull(j.paradaEm(c[0], c[1]));
            assertNull(j.provinciaEm(c[0], c[1]));
        }
        assertEquals(-1, j.localKhanEm(10, 10));
        assertNull(j.provinciaEm(10, 10));
    }

    // ---------------------------------------------------------------- estoque de tributos
    // Regras, p. 3: 10 espadas, 10 yurts e 15 moedas no jogo.

    @Test
    public void estoqueDeTributosELimitado() {
        Jogo j = jogo(2);
        assertEquals(6, (int) j.get(ESTOQUE).get("espada")); // 4 nas províncias
        assertEquals(6, (int) j.get(ESTOQUE).get("yurt"));
        assertEquals(9, (int) j.get(ESTOQUE).get("moeda")); // 4 nas províncias e 2 com os jogadores
        j.ativarColuna(0, 2);
        j.pegarTributo(0, "t6"); // da província para o jogador: o estoque não muda
        assertEquals(6, (int) j.get(ESTOQUE).get("espada"));
        j.moverKhan(0, 1); // t4, t5 e t6 recebem 1 peça do estoque
        assertEquals(5, (int) j.get(ESTOQUE).get("espada"));
        assertEquals(5, (int) j.get(ESTOQUE).get("yurt"));
        assertEquals(8, (int) j.get(ESTOQUE).get("moeda"));
    }

    // ---------------------------------------------------------------- cidades
    // Sarai fica no anel r8, r9 e r10; r9 é vizinha de Karakorum. A província t6 (espada) é vizinha de Karakorum.

    private static final List<String> ORDEM = List.of("Sarai", "Kiev", "Pequim", "Bagdá", "Cabul", "Moscou",
            "Samarcanda", "Kaesong", "Cantão");

    // Tolui pega 2 espadas (coluna amarela: 2 punhos e Khan) e anda até r9, ao lado de Sarai.
    private static Jogo toluiComDuasEspadasEmR9(List<Tesouro> saco) {
        Jogo j = jogo(2);
        j.prepararCidades(ORDEM, saco);
        j.ativarColuna(0, 2);
        j.moverKhan(0, 1); // t6 fica com 2 espadas
        j.pegarTributo(0, "t6");
        j.pegarTributo(0, "t6");
        j.mover(0, "r9");
        return j;
    }

    private static List<Tesouro> saco(Tesouro... t) {
        return List.of(t);
    }

    // §5: 3 cidades reveladas com 4 tesouros cada; as outras 6 ficam na pilha.
    @Test
    public void partidaComecaCom3CidadesReveladas() {
        Jogo j = jogo(2);
        assertEquals(9, List.copyOf(j.get(PONTOS_CIDADES).keySet()).size());
        int reveladas = 0;
        for (String c : List.copyOf(j.get(PONTOS_CIDADES).keySet())) {
            if (j.get(CIDADES_REVELADAS).contains(c)) {
                reveladas++;
                assertEquals(4, j.get(TESOUROS_CIDADES).get(c).size());
            } else {
                assertEquals(List.of(), j.get(TESOUROS_CIDADES).get(c));
            }
            assertEquals(-1, (int) j.get(DONOS_CIDADES).get(c));
        }
        assertEquals(3, reveladas);
        assertEquals(6, (int) j.get(CIDADES_NA_PILHA));
    }

    @Test
    public void prepararCidadesUsaAOrdemEOSacoDados() {
        Jogo j = jogo(2);
        j.prepararCidades(ORDEM, saco(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE, Tesouro.ESPECIARIA));
        assertEquals(List.of("boi", "seda", "ferro", "pele"), j.get(TESOUROS_CIDADES).get("Sarai"));
        assertEquals(List.of("especiaria"), j.get(TESOUROS_CIDADES).get("Kiev"));
        assertTrue(j.get(CIDADES_REVELADAS).contains("Pequim"));
        assertEquals(List.of(), j.get(TESOUROS_CIDADES).get("Pequim")); // o saco acabou
        assertFalse(j.get(CIDADES_REVELADAS).contains("Bagdá"));
    }

    // Regras, p. 10: o 1º tesouro custa 1 espada; os seguintes da mesma cidade, no mesmo turno, custam 2.
    @Test
    public void atacaCidadeAdjacenteComEspadas() {
        Jogo j = toluiComDuasEspadasEmR9(saco(Tesouro.BOI, Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO,
                Tesouro.PELE, Tesouro.PELE, Tesouro.PELE, Tesouro.PELE));
        assertEquals(2, (int) j.get(ESPADAS).get(0));
        assertEquals(List.of("Sarai"), j.get(CIDADES_ATACAVEIS));
        j.atacarCidade(0, "Sarai", "boi");
        assertEquals(1, (int) j.get(ESPADAS).get(0));
        assertEquals(1, (int) j.get(TESOUROS).get(0).get("boi"));
        assertEquals(List.of("boi", "seda", "ferro"), j.get(TESOUROS_CIDADES).get("Sarai"));
        assertEquals(List.of(), j.get(CIDADES_ATACAVEIS)); // o próximo custaria 2
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.atacarCidade(0, "Sarai", "seda"));
        assertTrue(e.getMessage().contains("2 espada"));
        assertEquals(1, (int) j.get(ESPADAS).get(0));
        assertEquals(3, j.get(TESOUROS_CIDADES).get("Sarai").size());
    }

    @Test
    public void naoAtacaCidadeLongeNemEscondidaNemTesouroQueNaoTem() {
        Jogo j = jogo(2);
        j.prepararCidades(ORDEM, saco(Tesouro.BOI, Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE,
                Tesouro.PELE, Tesouro.PELE, Tesouro.PELE));
        assertThrows(IllegalStateException.class, () -> j.atacarCidade(0, "Sarai", "boi")); // sem coluna
        j.ativarColuna(0, 2);
        j.moverKhan(0, 1);
        j.pegarTributo(0, "t6");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.atacarCidade(0, "Sarai", "boi"));
        assertTrue(e.getMessage().contains("adjacente")); // Tolui ainda está em Karakorum
        j.mover(0, "r9");
        assertThrows(IllegalArgumentException.class, () -> j.atacarCidade(0, "Sarai", "especiaria"));
        assertThrows(IllegalArgumentException.class, () -> j.atacarCidade(0, "Sarai", "ouro"));
        IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> j.atacarCidade(0, "Bagdá", "boi"));
        assertTrue(e2.getMessage().contains("revelada"));
        assertEquals(1, (int) j.get(ESPADAS).get(0));
    }

    // Regras, p. 10: quem toma o último tesouro conquista a cidade, e a próxima cidade da pilha é revelada.
    @Test
    public void conquistaRevelaAProximaCidade() {
        // Sarai recebe só 1 tesouro: o saco tem 1 peça para ela e as outras cidades ficam vazias.
        Jogo j = jogo(2);
        j.prepararCidades(List.of("Kiev", "Pequim", "Sarai", "Bagdá", "Cabul", "Moscou", "Samarcanda", "Kaesong",
                "Cantão"), saco(Tesouro.BOI, Tesouro.BOI, Tesouro.BOI, Tesouro.BOI, Tesouro.SEDA, Tesouro.SEDA,
                Tesouro.SEDA, Tesouro.SEDA, Tesouro.PELE));
        assertEquals(List.of("pele"), j.get(TESOUROS_CIDADES).get("Sarai"));
        j.ativarColuna(0, 2);
        j.moverKhan(0, 1);
        j.pegarTributo(0, "t6");
        j.mover(0, "r9");
        assertEquals(List.of(Evento.CIDADE, Evento.CONQUISTA), eventos(j, () -> j.atacarCidade(0, "Sarai", "pele")));
        assertEquals("Sarai", j.get(ULTIMA_CIDADE));
        assertEquals("pele", j.get(ULTIMO_TESOURO));
        assertEquals(0, (int) j.get(DONOS_CIDADES).get("Sarai"));
        assertEquals(11, (int) j.get(YURTS_RESTANTES).get(0)); // o yurt no meio da cidade conquistada
        assertEquals(1, (int) j.get(TESOUROS).get(0).get("pele"));
        assertEquals(0, (int) j.get(ESPADAS).get(0));
        assertTrue(j.get(CIDADES_REVELADAS).contains("Bagdá"));
        assertEquals(5, (int) j.get(CIDADES_NA_PILHA));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.atacarCidade(0, "Sarai", "pele"));
        assertTrue(e.getMessage().contains("conquistada"));
        assertFalse(j.get(CIDADES_ATACAVEIS).contains("Sarai"));
    }

    // ---------------------------------------------------------------- yurts
    // t9 e t4 são as províncias de yurt vizinhas de Karakorum. r11 é uma parada dupla vizinha de Karakorum.

    // Regras, p. 11: o yurt é construído numa parada onde o pião esteve no turno, gastando 1 peça de yurt.
    @Test
    public void constroiYurtNumaParadaDoCaminho() {
        Jogo j = jogo(2);
        assertEquals(12, (int) j.get(YURTS_RESTANTES).get(0));
        j.ativarColuna(0, 1); // Tolui: 2 cavalos e 1 punho
        assertThrows(IllegalStateException.class, () -> j.construirYurt(0, "karakorum")); // sem peça de yurt
        j.pegarTributo(0, "t9");
        assertEquals(List.of(), j.get(PARADAS_PARA_YURT)); // Karakorum não pode ter yurts
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.construirYurt(0, "karakorum"));
        assertTrue(e.getMessage().contains("Karakorum"));
        j.mover(0, "r9");
        assertEquals(List.of("r9"), j.get(PARADAS_PARA_YURT));
        assertThrows(IllegalArgumentException.class, () -> j.construirYurt(0, "r8")); // não esteve lá
        int estoque = (int) j.get(ESTOQUE).get("yurt");
        j.construirYurt(0, "r9");
        assertEquals(List.of(0), j.get(YURTS_NAS_PARADAS).get("r9"));
        assertEquals(0, (int) j.get(PECAS_YURT).get(0));
        assertEquals(11, (int) j.get(YURTS_RESTANTES).get(0));
        assertEquals(estoque + 1, (int) j.get(ESTOQUE).get("yurt")); // a peça usada volta para o estoque
        assertEquals(List.of(), j.get(PARADAS_PARA_YURT));
    }

    @Test
    public void paradaDuplaTemLugarPara2Yurts() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2); // Tolui: 1 cavalo, 2 punhos e Khan
        j.pegarTributo(0, "t9");
        j.pegarTributo(0, "t4");
        j.mover(0, "r11");
        j.construirYurt(0, "r11");
        j.construirYurt(0, "r11");
        assertEquals(List.of(0, 0), j.get(YURTS_NAS_PARADAS).get("r11"));
        assertEquals(10, (int) j.get(YURTS_RESTANTES).get(0));
    }

    @Test
    public void naoConstroiOndeJaTemYurt() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        j.pegarTributo(0, "t9");
        j.pegarTributo(0, "t4");
        j.mover(0, "r9");
        j.construirYurt(0, "r9");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.construirYurt(0, "r9"));
        assertTrue(e.getMessage().contains("não cabe"));
        assertEquals(1, (int) j.get(PECAS_YURT).get(0));
    }

    // Regras, p. 7: o jogador pode pular os próprios yurts ao se mover; a parada pulada não conta como visitada.
    @Test
    public void pulaOsPropriosYurts() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1); // Tolui: 2 cavalos e 1 punho
        j.pegarTributo(0, "t9");
        j.mover(0, "r9");
        j.construirYurt(0, "r9");
        j.mover(0, "karakorum");
        fim(j, 0);
        j.ativarColuna(1, 1); // Altani fica em Karakorum
        fim(j, 1);
        j.ativarColuna(0, 0); // Tolui: 1 cavalo
        assertTrue(j.get(DESTINOS).containsAll(List.of("r9", "r8", "r10", "r6")));
        j.mover(0, "r8"); // pula r9
        assertEquals("r8", j.get(POSICOES).get(0));
        assertEquals(0, (int) j.get(MOVIMENTOS));
        assertEquals(List.of("karakorum", "r8"), j.get(VISITADAS));
        fim(j, 0);
        j.ativarColuna(1, 0); // Altani não pode pular o yurt de Tolui
        assertFalse(j.get(DESTINOS).contains("r8"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.mover(1, "r8"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
    }

    // ---------------------------------------------------------------- conselheiros, cartas e fim
    // Cartas que ninguém cumpre nos testes abaixo, a não ser quando o teste escolhe outras.
    private static final List<String> CARTAS_DIFICEIS = List.of("yurts10", "cidades3", "regioes2", "yurts6");

    private static Jogo jogoSemCartas(int n) {
        Jogo j = jogo(n);
        j.prepararCartas(CARTAS_DIFICEIS);
        return j;
    }

    // Regras, p. 12 e 13: a parada dupla r11 pede 2 ferros (1 voto); Karakorum pede ferro, pele e boi (2 votos).
    @Test
    public void entregaTesourosAosConselheiros() {
        Jogo j = jogoSemCartas(2);
        assertEquals(7, List.copyOf(j.get(PEDIDOS_CONSELHEIROS).keySet()).size());
        assertEquals(List.of("ferro", "ferro"), j.get(PEDIDOS_CONSELHEIROS).get("r11"));
        assertEquals(List.of("ferro", "pele", "boi"), j.get(PEDIDOS_CONSELHEIROS).get("karakorum"));
        assertEquals(List.of("qualquer", "qualquer", "qualquer"), j.get(PEDIDOS_CONSELHEIROS).get("c8"));
        assertEquals(2, (int) j.get(VOTOS_CONSELHEIROS).get("karakorum"));
        Jogador tolui = j.jogador(0);
        for (Tesouro t : List.of(Tesouro.FERRO, Tesouro.FERRO, Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI))
            tolui.ganharTesouro(t);
        j.ativarColuna(0, 1);
        assertEquals(List.of("karakorum"), j.get(CONSELHEIROS_PARA_ENTREGA));
        j.entregarTesouros(0, "karakorum"); // esteve lá no começo do turno
        assertEquals(2, (int) j.get(VOTOS).get(0));
        assertEquals(2, (int) j.get(VOTOS_NO_CONSELHO));
        assertEquals(2, (int) j.get(TESOUROS).get(0).get("ferro"));
        assertEquals(0, (int) j.get(TESOUROS).get(0).get("boi"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.entregarTesouros(0, "r11"));
        assertTrue(e.getMessage().contains("não esteve"));
        j.mover(0, "r11");
        j.entregarTesouros(0, "r11");
        assertEquals(3, (int) j.get(VOTOS).get(0));
        assertEquals(0, (int) j.get(TESOUROS).get(0).get("ferro"));
        IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> j.entregarTesouros(0, "r11"));
        assertTrue(e2.getMessage().contains("2 ferro"));
        assertEquals(List.of(), j.get(CONSELHEIROS_PARA_ENTREGA));
        assertThrows(IllegalArgumentException.class, () -> j.entregarTesouros(0, "r9")); // r9 não tem conselheiro
    }

    // No conselheiro que aceita 3 tesouros quaisquer, vão os que o jogador tem em maior quantidade.
    @Test
    public void conselheiroDe3QuaisquerUsaOsTesourosMaisNumerosos() {
        Conselheiro c8 = new Mapa().getConselheiro("c8");
        Jogador j = new Jogador(Herdeiro.TOLUI, 1);
        j.ganharTesouro(Tesouro.SEDA);
        j.ganharTesouro(Tesouro.PELE);
        assertNull(Kurultai.entrega(c8, j)); // só 2 tesouros
        j.ganharTesouro(Tesouro.PELE);
        j.ganharTesouro(Tesouro.PELE);
        assertEquals(List.of(Tesouro.PELE, Tesouro.PELE, Tesouro.SEDA), Kurultai.entrega(c8, j)); // empate: o primeiro tipo
        j.ganharTesouro(Tesouro.SEDA);
        assertEquals(List.of(Tesouro.PELE, Tesouro.SEDA, Tesouro.PELE), Kurultai.entrega(c8, j));
    }

    // Regras, p. 13 e 18: o primeiro a cumprir uma carta de conselheiro aberto ganha 1 voto; ninguém mais ganha.
    @Test
    public void primeiroACumprirACartaGanha1Voto() {
        Jogo j = jogo(2);
        j.prepararCartas(List.of("espadas4", "yurts10", "cidades3", "regioes2"));
        assertEquals(List.of("espadas4", "yurts10", "cidades3", "regioes2"), List.copyOf(j.get(CARTAS_ABERTAS).keySet()));
        assertEquals("Primeiro a ter 4 espadas", j.get(CARTAS_ABERTAS).get("espadas4"));
        for (int k = 0; k < 3; k++) j.jogador(0).ganhar(Tributo.ESPADA);
        j.ativarColuna(0, 0);
        assertEquals(List.of(Evento.TRIBUTO, Evento.CARTA), eventos(j, () -> j.pegarTributo(0, "t6"))); // 4ª espada
        assertEquals("t6", j.get(ULTIMA_PROVINCIA));
        assertEquals("espadas4", j.get(ULTIMA_CARTA));
        assertEquals(0, (int) j.get(DONOS_CARTAS).get("espadas4"));
        assertEquals(-1, (int) j.get(DONOS_CARTAS).get("yurts10"));
        assertEquals(1, (int) j.get(VOTOS).get(0));
        fim(j, 0);
        for (int k = 0; k < 4; k++) j.jogador(1).ganhar(Tributo.ESPADA); // Altani também tem 4 espadas
        j.ativarColuna(1, 1);
        j.pegarTributo(1, "t5"); // qualquer ação faz a verificação das cartas
        assertEquals(0, (int) j.get(VOTOS).get(1)); // a carta já foi ganha por Tolui
        assertEquals(0, (int) j.get(DONOS_CARTAS).get("espadas4"));
    }

    // Regras, p. 14: com 2 jogadores, o fim é disparado quando o conselho chega a 10 votos. A rodada atual
    // termina e há mais uma rodada completa; depois disso, ninguém mais joga.
    @Test
    public void fimDaPartidaDepoisDaUltimaRodada() {
        Jogo j = jogoSemCartas(2);
        assertEquals(10, (int) j.get(LIMITE_VOTOS));
        j.jogador(0).ganharVotos(8);
        for (Tesouro t : List.of(Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI)) j.jogador(0).ganharTesouro(t);
        j.ativarColuna(0, 0);
        assertFalse(j.get(FIM_DISPARADO));
        assertEquals(List.of(Evento.CONSELHEIRO, Evento.FIM_DISPARADO),
                eventos(j, () -> j.entregarTesouros(0, "karakorum"))); // 10 votos
        assertEquals("karakorum", j.get(ULTIMO_CONSELHEIRO));
        assertTrue(j.get(FIM_DISPARADO));
        assertEquals(3, (int) j.get(TURNOS_RESTANTES)); // Altani termina a rodada, e cada um joga mais uma vez
        int[][] turnos = {{0, 0}, {1, 1}, {0, 1}, {1, 2}};
        for (int k = 0; k < turnos.length; k++) {
            int[] t = turnos[k];
            if (k > 0) j.ativarColuna(t[0], t[1]);
            assertFalse(j.get(PARTIDA_TERMINADA));
            fim(j, t[0]);
        }
        assertTrue(j.get(PARTIDA_TERMINADA));
        assertEquals(List.of(0), j.get(VENCEDORES));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.ativarColuna(1, 0));
        assertTrue(e.getMessage().contains("terminou"));
    }

    @Test
    public void empateTemVariosVencedores() {
        Jogo j = jogoSemCartas(3);
        j.jogador(0).ganharVotos(2);
        j.jogador(2).ganharVotos(2);
        assertEquals(List.of(0, 2), j.get(VENCEDORES));
    }

    // Observer (cap. 17): cada mudança de estado avisa os observadores, que buscam os dados com get.
    @Test
    public void observadoresSaoAvisadosDasMudancas() {
        Jogo j = jogo(2);
        List<String> posicoes = new ArrayList<>();
        List<Integer> autores = new ArrayList<>();
        ObservadorIF obs = observado -> {
            posicoes.add(observado.get(POSICOES).get(0));
            autores.add(observado.get(AUTOR_DO_EVENTO));
        };
        j.add(obs);
        List<Evento> eventos;
        try {
            eventos = eventos(j, () -> {
                j.selecionarColuna(0, 0);
                j.confirmarColuna(0);
                j.mover(0, "r9");
                fim(j, 0);
            });
            assertEquals(1, (int) j.get(JOGADOR_ATUAL));
            assertEquals(List.of(Evento.PARTIDA), eventos(j, () -> j.novaPartida(List.of("Jochi", "Ogedei"))));
            assertEquals(List.of(), eventos(j, () -> assertThrows(IllegalStateException.class,
                    () -> j.mover(0, "r9")))); // erro: ninguém é avisado
        } finally {
            j.remove(obs);
        }
        assertEquals(List.of(Evento.SELECAO, Evento.COLUNA, Evento.MOVIMENTO, Evento.TURNO), eventos);
        assertEquals("r9", posicoes.get(2));
        assertEquals(List.of(0, 0, 0, 0, -1), autores); // TURNO: o autor é quem encerrou; PARTIDA: ninguém
    }

    // Grupo 7: no último turno, o evento é FIM, e não TURNO.
    @Test
    public void ultimoTurnoAvisaFim() {
        Jogo j = jogoSemCartas(2);
        j.jogador(0).ganharVotos(8);
        for (Tesouro t : List.of(Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI)) j.jogador(0).ganharTesouro(t);
        j.ativarColuna(0, 0);
        j.entregarTesouros(0, "karakorum"); // 10 votos: fim disparado
        int[][] turnos = {{0, 0}, {1, 1}, {0, 1}};
        for (int k = 0; k < turnos.length; k++) {
            if (k > 0) j.ativarColuna(turnos[k][0], turnos[k][1]);
            fim(j, turnos[k][0]);
        }
        j.ativarColuna(1, 2);
        assertEquals(List.of(Evento.FIM), eventos(j, () -> j.encerrarTurno(1)));
        assertEquals(1, (int) j.get(AUTOR_DO_EVENTO));
        assertTrue(j.get(PARTIDA_TERMINADA));
        assertEquals(List.of(0), j.get(VENCEDORES));
    }

    // Toda chave declarada em ObservadoIF tem valor numa partida em andamento.
    @Test
    public void todaChaveTemValor() throws IllegalAccessException {
        Jogo j = jogo(2);
        int chaves = 0;
        for (Field f : ObservadoIF.class.getFields()) {
            if (f.getType() != Chave.class) continue;
            j.get((Chave<?>) f.get(null)); // sem valor: IllegalArgumentException
            chaves++;
        }
        assertEquals(59, chaves); // grupos 1 a 7: 14 + 3 + 9 + 11 + 8 + 6 + 8
    }

    @Test
    public void valoresDeGetSaoSoParaLeituraECopias() {
        Jogo j = jogo(2);
        assertEquals(List.of("Tolui", "Altani"), j.get(NOMES));
        assertEquals(List.of("karakorum", "karakorum"), j.get(POSICOES));
        assertThrows(UnsupportedOperationException.class, () -> j.get(NOMES).add("Jochi"));
        assertThrows(UnsupportedOperationException.class, () -> j.get(PONTOS_PARADAS).remove("r1"));
        assertThrows(UnsupportedOperationException.class, () -> j.get(TESOUROS).get(0).put("seda", 9));
        j.get(PONTOS_PARADAS).get("r1")[0] = -1; // o array é uma cópia
        assertNotEquals(-1, j.get(PONTOS_PARADAS).get("r1")[0]);
        Map<String, Integer> tributos = j.get(ESTOQUE);
        assertEquals(List.of("yurt", "espada", "moeda"), List.copyOf(tributos.keySet()));
    }

    // Executa a ação e devolve os eventos avisados durante ela.
    private static List<Evento> eventos(Jogo j, Runnable acao) {
        List<Evento> eventos = new ArrayList<>();
        ObservadorIF obs = observado -> eventos.add(observado.get(EVENTO));
        j.add(obs);
        try {
            acao.run();
        } finally {
            j.remove(obs);
        }
        return eventos;
    }
}
