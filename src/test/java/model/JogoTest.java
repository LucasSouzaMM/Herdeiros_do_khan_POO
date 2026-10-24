package model;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

// Indexes start at 0: "jogador 1" in teste_herdeiros.md is index 0.
// Columns are 0 to 3 from left to right on the player board: red, green, yellow, white.
public class JogoTest {

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    private static Jogo jogo(int n) {
        return new Jogo(List.of("Tolui", "Altani", "Jochi", "Chagatai", "Ogedei").subList(0, n));
    }

    // T1
    @Test
    public void criaPartidaCom2Jogadores() {
        Jogo j = jogo(2);
        assertEquals(2, j.getNumeroJogadores());
        assertEquals(0, j.getJogadorAtual());
        assertEquals("Tolui", j.getNome(0));
        assertEquals("Altani", j.getNome(1));
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
                () -> new Jogo(List.of("Tolui", "Altani", "Jochi", "Chagatai", "Ogedei", "Tolui")));
        assertTrue(e.getMessage().contains("mínimo: 2"));
        assertTrue(e.getMessage().contains("máximo: 5"));
    }

    // T3
    @Test
    public void passarTurnoPassaParaOProximo() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.encerrarTurno(0);
        assertEquals(1, j.getJogadorAtual());
        j.ativarColuna(1, 0);
        j.encerrarTurno(1);
        assertEquals(0, j.getJogadorAtual());
    }

    // T4, CA03
    @Test
    public void acaoForaDoTurnoFalha() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.encerrarTurno(0);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.ativarColuna(0, 1));
        assertTrue(e.getMessage().contains("Jogo.ativarColuna"));
        assertEquals(-1, j.getColunaAtivada());
    }

    // T5
    @Test
    public void acaoNoTurnoFunciona() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertEquals(2, j.getColunaAtivada());
    }

    @Test
    public void soUmaColunaPorTurno() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertThrows(IllegalStateException.class, () -> j.ativarColuna(0, 1));
        j.encerrarTurno(0);
        assertEquals(-1, j.getColunaAtivada());
        j.ativarColuna(1, 1);
        assertEquals(1, j.getColunaAtivada());
    }

    // T7
    @Test
    public void naoPassaTurnoDeOutroJogador() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.encerrarTurno(1));
        assertTrue(e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals(0, j.getJogadorAtual());
    }

    @Test
    public void rejeitaJogadorInexistente() {
        Jogo j = jogo(2);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.getMoedas(2));
        assertTrue(e.getMessage().contains("Jogo"));
    }

    // T8
    @Test
    public void moedasIniciaisCom3Jogadores() {
        Jogo j = jogo(3);
        assertEquals(1, j.getMoedas(0));
        assertEquals(1, j.getMoedas(1));
        assertEquals(2, j.getMoedas(2));
    }

    // T9
    @Test
    public void moedasIniciaisCom5Jogadores() {
        Jogo j = jogo(5);
        assertEquals(1, j.getMoedas(0));
        assertEquals(1, j.getMoedas(1));
        for (int i = 2; i < 5; i++) assertEquals(2, j.getMoedas(i));
    }

    // G5, G9: all pawns start on Karakorum (rules §5).
    @Test
    public void pioesComecamEmKarakorum() {
        Jogo j = jogo(5);
        for (int i = 0; i < 5; i++) assertEquals("karakorum", j.getPosicao(i));
    }

    // J1
    @Test
    public void colunaDaOsCavalosDaCartelaDoHerdeiro() {
        Jogo j = jogo(2);
        assertEquals(0, j.getMovimentos());
        j.ativarColuna(0, 1); // green column of Tolui: 2 horses
        assertEquals(2, j.getMovimentos());
        j.encerrarTurno(0);
        assertEquals(0, j.getMovimentos());
        j.ativarColuna(1, 0); // red column of Altani: 1 horse
        assertEquals(1, j.getMovimentos());
    }

    // J2
    @Test
    public void naoEncerraTurnoSemAtivarColuna() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.encerrarTurno(0));
        assertTrue(e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals(0, j.getJogadorAtual());
    }

    // J4, RF04
    @Test
    public void salvaECarregaPartida() throws Exception {
        Jogo j = jogo(3);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        j.ativarColuna(1, 1);
        j.mover(1, "r11");
        Path arquivo = pasta.newFile("partida.txt").toPath();
        j.salvar(arquivo);

        Jogo c = Jogo.carregar(arquivo);
        assertEquals(3, c.getNumeroJogadores());
        for (int i = 0; i < 3; i++) {
            assertEquals(j.getNome(i), c.getNome(i));
            assertEquals(j.getMoedas(i), c.getMoedas(i));
        }
        assertEquals("r9", c.getPosicao(0));
        assertEquals("r11", c.getPosicao(1));
        assertEquals("karakorum", c.getPosicao(2));
        assertEquals(1, c.getJogadorAtual());
        assertEquals(1, c.getColunaAtivada());
        assertEquals(1, c.getMovimentos());
    }

    @Test
    public void carregarParadaInexistenteFalha() throws Exception {
        Path arquivo = pasta.newFile("parada.txt").toPath();
        jogo(2).salvar(arquivo);
        Files.writeString(arquivo, Files.readString(arquivo).replace("jogador0.posicao=karakorum", "jogador0.posicao=x1"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage().contains("x1"));
    }

    // Moves below start on Karakorum, whose neighbors are r9, r11, c4 and p4.
    // Column 0 (red) gives 1 horse to Tolui, Altani and Jochi; column 1 (green) gives 2 to Tolui and Altani.

    // J3
    @Test
    public void turnoPassaQuandoOsMovimentosAcabam() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r10"); // 2 steps: karakorum, r9 or r11, r10
        assertEquals("r10", j.getPosicao(0));
        assertEquals(1, j.getJogadorAtual());
        assertEquals(0, j.getMovimentos());
        assertEquals(-1, j.getColunaAtivada());
    }

    // J8
    @Test
    public void moveParaParadaVizinha() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        assertEquals("r9", j.getPosicao(0));
        assertEquals(1, j.getJogadorAtual());
    }

    // J9, CA04
    @Test
    public void naoMoveAlemDosMovimentos() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.mover(0, "r10"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
        assertEquals("karakorum", j.getPosicao(0));
        assertEquals(1, j.getMovimentos());
        assertEquals(0, j.getJogadorAtual());
    }

    // J10
    @Test
    public void moveEmVariosCliques() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        assertEquals(1, j.getMovimentos());
        assertEquals(0, j.getJogadorAtual());
        j.mover(0, "r8");
        assertEquals("r8", j.getPosicao(0));
        assertEquals(1, j.getJogadorAtual());
    }

    // J11
    @Test
    public void passaPorParadaOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        j.ativarColuna(1, 1);
        j.mover(1, "r8"); // the only 2-step path goes through r9, where Tolui is
        assertEquals("r8", j.getPosicao(1));
    }

    // J12
    @Test
    public void naoTerminaEmParadaSimplesOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        j.ativarColuna(1, 0);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> j.mover(1, "r9"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
        assertEquals("karakorum", j.getPosicao(1));
    }

    // J13
    @Test
    public void paradaDuplaAceitaDoisPioes() {
        Jogo j = jogo(3);
        j.ativarColuna(0, 0);
        j.mover(0, "r11");
        j.ativarColuna(1, 0);
        j.mover(1, "r11");
        assertEquals("r11", j.getPosicao(1));
        j.ativarColuna(2, 0);
        assertThrows(IllegalArgumentException.class, () -> j.mover(2, "r11"));
        assertEquals("karakorum", j.getPosicao(2));
    }

    // J14
    @Test
    public void karakorumAceitaTodosOsPioes() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        j.ativarColuna(1, 0);
        j.encerrarTurno(1);
        j.ativarColuna(0, 0);
        j.mover(0, "karakorum"); // Altani is still on Karakorum
        assertEquals("karakorum", j.getPosicao(0));
    }

    // J15
    @Test
    public void naoMoveSemAtivarColuna() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> j.mover(0, "r9"));
        assertTrue(e.getMessage().contains("Jogo.mover"));
        assertEquals("karakorum", j.getPosicao(0));
    }

    // CA03
    @Test
    public void naoMoveForaDoTurno() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertThrows(IllegalStateException.class, () -> j.mover(1, "r9"));
        assertEquals("karakorum", j.getPosicao(1));
    }

    @Test
    public void naoMoveParaAMesmaParadaNemParaParadaInexistente() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertThrows(IllegalArgumentException.class, () -> j.mover(0, "karakorum"));
        assertThrows(IllegalArgumentException.class, () -> j.mover(0, "x1"));
        assertEquals(1, j.getMovimentos());
    }

    // J17
    @Test
    public void paradaEmPontoDoTabuleiro() {
        Jogo j = jogo(2);
        assertEquals("r1", j.paradaEm(490, 255));
        assertNull(j.paradaEm(10, 10));
    }

    // Data the view needs to draw pawns and the debug overlay (RNF10: the view only talks to Jogo).
    @Test
    public void dadosDoMapaParaAInterface() {
        Jogo j = jogo(2);
        assertEquals(34, j.getParadas().size());
        assertArrayEquals(new int[] {855, 170, 930, 250}, j.getPontos("r4"));
        assertEquals(List.of("r1", "r6"), j.getVizinhas("r4"));
        assertTrue(j.getRaio("karakorum") > j.getRaio("r1"));
        assertEquals(List.of("Altani", "Jochi", "Chagatai", "Tolui", "Ogedei"), Jogo.getHerdeiros());
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
    }

    // J6
    @Test
    public void rejeitaHerdeiroRepetido() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Jogo(List.of("Tolui", "Altani", "Tolui")));
        assertTrue(e.getMessage().contains("Jogo.Jogo"));
        assertTrue(e.getMessage().contains("Tolui"));
    }

    // J7, RF07
    @Test
    public void sorteiaHerdeirosDiferentes() {
        Set<List<String>> combinacoes = new HashSet<>();
        for (int k = 0; k < 20; k++) {
            Jogo j = Jogo.sortear(3);
            assertEquals(3, j.getNumeroJogadores());
            List<String> nomes = List.of(j.getNome(0), j.getNome(1), j.getNome(2));
            assertEquals(3, new HashSet<>(nomes).size());
            combinacoes.add(nomes);
        }
        assertTrue(combinacoes.size() >= 2);
    }

    @Test
    public void sortearValidaNumeroDeJogadores() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Jogo.sortear(6));
        assertTrue(e.getMessage().contains("máximo: 5"));
    }
}
