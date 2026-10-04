package model;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

// Indexes start at 0: "jogador 1" in teste_herdeiros.md is index 0.
public class JogoTest {

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    private static Jogo jogo(int n) {
        return new Jogo(List.of("Ana", "Bia", "Caio", "Davi", "Eva").subList(0, n));
    }

    // T1
    @Test
    public void criaPartidaCom2Jogadores() {
        Jogo j = jogo(2);
        assertEquals(2, j.getNumeroJogadores());
        assertEquals(0, j.getJogadorAtual());
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

    // T6, CA02 (o máximo é 5, como nas regras e no T9)
    @Test
    public void rejeitaMaisDe5Jogadores() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Jogo(List.of("A", "B", "C", "D", "E", "F")));
        assertTrue(e.getMessage().contains("mínimo: 2"));
        assertTrue(e.getMessage().contains("máximo: 5"));
    }

    // T3
    @Test
    public void passarTurnoPassaParaOProximo() {
        Jogo j = jogo(2);
        j.encerrarTurno(0);
        assertEquals(1, j.getJogadorAtual());
        j.encerrarTurno(1);
        assertEquals(0, j.getJogadorAtual());
    }

    // T4, CA03
    @Test
    public void acaoForaDoTurnoFalha() {
        Jogo j = jogo(2);
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

    // RF04
    @Test
    public void salvaECarregaPartida() throws Exception {
        Jogo j = jogo(3);
        j.encerrarTurno(0);
        j.ativarColuna(1, 3);
        Path arquivo = pasta.newFile("partida.txt").toPath();
        j.salvar(arquivo);

        Jogo c = Jogo.carregar(arquivo);
        assertEquals(3, c.getNumeroJogadores());
        assertEquals("Caio", c.getNome(2));
        assertEquals(1, c.getJogadorAtual());
        assertEquals(3, c.getColunaAtivada());
        assertEquals(2, c.getMoedas(2));
    }

    @Test
    public void carregarArquivoInvalidoFalha() throws Exception {
        Path arquivo = pasta.newFile("ruim.txt").toPath();
        Files.writeString(arquivo, "jogadores=dois\n");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage().contains("Jogo.carregar"));
    }
}
