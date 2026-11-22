package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
        j.prepararBonus(List.of()); // sem peças de bônus: o trono daria 1 voto a mais nas entregas
        Partidas.escolherSecretas(j);
        for (int i = 0; i < n; i++) j.jogador(i).limparMelhorias(); // sem as 2 melhorias sorteadas do Jochi
        return j;
    }

    // Encerra o turno, movendo antes o Khan quando a coluna manda (§7.3).
    private static void fim(Jogo j, int jogador) {
        if (j.get(KHAN_PENDENTE)) j.moverKhan(jogador, (int) j.get(POSICAO_KHAN) == 0 ? 1 : 0);
        j.encerrarTurno(jogador);
    }

    // T1
    @Test(timeout = 1000)
    public void criaPartidaCom2Jogadores() {
        Jogo j = jogo(2);
        assertEquals("2 jogadores", 2, j.get(NOMES).size());
        assertEquals("começa o 1º", 0, (int) j.get(JOGADOR_ATUAL));
        assertEquals("o 1º é Tolui", "Tolui", j.get(NOMES).get(0));
        assertEquals("o 2º é Altani", "Altani", j.get(NOMES).get(1));
    }

    // Singleton (cap. 17)
    @Test(timeout = 1000)
    public void haUmaSoInstancia() {
        assertSame("getInstance devolve sempre o mesmo Jogo", Jogo.getInstance(), Jogo.getInstance());
        assertSame("a partida nova usa a mesma instância", Jogo.getInstance(), jogo(2));
    }

    @Test(timeout = 1000)
    public void novaPartidaZeraOEstadoAnterior() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        j.novaPartida(List.of("Ogedei", "Jochi", "Altani"));
        assertEquals("3 jogadores na partida nova", 3, j.get(NOMES).size());
        assertEquals("a partida nova começa no 1º", 0, (int) j.get(JOGADOR_ATUAL));
        assertEquals("nenhuma coluna ativada", -1, (int) j.get(COLUNA_ATIVADA));
        assertEquals("o pião volta a Karakorum", "karakorum", j.get(POSICOES).get(0));
        assertFalse("as colunas voltam a ficar livres", j.get(COLUNAS_BLOQUEADAS).get(0).contains(1));
    }

    @Test(timeout = 1000)
    public void partidaInvalidaNaoApagaAAnterior() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        assertThrows("1 jogador só é recusado", IllegalArgumentException.class, () -> j.novaPartida(List.of("Tolui")));
        assertEquals("a partida anterior continua com 2", 2, j.get(NOMES).size());
        assertEquals("a coluna ativada continua", 1, (int) j.get(COLUNA_ATIVADA));
    }

    // T2, CA01
    @Test(timeout = 1000)
    public void rejeitaMenosDe2Jogadores() {
        IllegalArgumentException e = assertThrows("-1 jogadores é recusado", IllegalArgumentException.class,
                () -> Jogo.validarNumeroJogadores(-1));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.validarNumeroJogadores"));
        assertTrue(e.getMessage(), e.getMessage().contains("mínimo: 2"));
        assertTrue(e.getMessage(), e.getMessage().contains("máximo: 5"));
        assertThrows("1 jogador é recusado", IllegalArgumentException.class, () -> jogo(1));
    }

    // T6, CA02
    @Test(timeout = 1000)
    public void rejeitaMaisDe5Jogadores() {
        IllegalArgumentException e = assertThrows("6 jogadores são recusados", IllegalArgumentException.class,
                () -> Jogo.getInstance().novaPartida(List.of("Tolui", "Altani", "Jochi", "Chagatai", "Ogedei", "Tolui")));
        assertTrue(e.getMessage(), e.getMessage().contains("mínimo: 2"));
        assertTrue(e.getMessage(), e.getMessage().contains("máximo: 5"));
    }

    // T3
    @Test(timeout = 1000)
    public void passarTurnoPassaParaOProximo() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        assertEquals("depois do 1º, joga o 2º", 1, (int) j.get(JOGADOR_ATUAL));
        j.ativarColuna(1, 0);
        fim(j, 1);
        assertEquals("depois do último, volta o 1º", 0, (int) j.get(JOGADOR_ATUAL));
    }

    // T4, CA03
    @Test(timeout = 1000)
    public void acaoForaDoTurnoFalha() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        IllegalStateException e = assertThrows("Tolui não joga na vez de Altani",
                IllegalStateException.class, () -> j.ativarColuna(0, 1));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.ativarColuna"));
        assertEquals("a recusa não ativa coluna", -1, (int) j.get(COLUNA_ATIVADA));
    }

    // T5
    @Test(timeout = 1000)
    public void acaoNoTurnoFunciona() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertEquals("a coluna amarela ativada", 2, (int) j.get(COLUNA_ATIVADA));
    }

    @Test(timeout = 1000)
    public void soUmaColunaPorTurno() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertThrows("não ativa 2 colunas no turno", IllegalStateException.class, () -> j.ativarColuna(0, 1));
        fim(j, 0);
        assertEquals("o turno novo começa sem coluna", -1, (int) j.get(COLUNA_ATIVADA));
        j.ativarColuna(1, 1);
        assertEquals("Altani ativa a coluna verde", 1, (int) j.get(COLUNA_ATIVADA));
    }

    // T7
    @Test(timeout = 1000)
    public void naoPassaTurnoDeOutroJogador() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows("Altani não encerra o turno de Tolui",
                IllegalStateException.class, () -> j.encerrarTurno(1));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals("continua a vez de Tolui", 0, (int) j.get(JOGADOR_ATUAL));
    }

    @Test(timeout = 1000)
    public void rejeitaJogadorInexistente() {
        Jogo j = jogo(2);
        IllegalArgumentException e = assertThrows("o jogador 3 não existe numa partida de 2",
                IllegalArgumentException.class, () -> j.ativarColuna(2, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo"));
    }

    // T8
    @Test(timeout = 1000)
    public void moedasIniciaisCom3Jogadores() {
        Jogo j = jogo(3);
        assertEquals("o 1º começa com 1 moeda", 1, (int) j.get(MOEDAS).get(0));
        assertEquals("o 2º começa com 1 moeda", 1, (int) j.get(MOEDAS).get(1));
        assertEquals("o 3º começa com 2 moedas", 2, (int) j.get(MOEDAS).get(2));
    }

    // T9
    @Test(timeout = 1000)
    public void moedasIniciaisCom5Jogadores() {
        Jogo j = jogo(5);
        assertEquals("o 1º começa com 1 moeda", 1, (int) j.get(MOEDAS).get(0));
        assertEquals("o 2º começa com 1 moeda", 1, (int) j.get(MOEDAS).get(1));
        for (int i = 2; i < 5; i++) assertEquals("o " + (i + 1) + "º começa com 2 moedas",
                2, (int) j.get(MOEDAS).get(i));
    }

    // G5, G9: todos os piões começam em Karakorum (regras, §5).
    @Test(timeout = 1000)
    public void pioesComecamEmKarakorum() {
        Jogo j = jogo(5);
        for (int i = 0; i < 5; i++) assertEquals("o pião " + i + " começa em Karakorum",
                "karakorum", j.get(POSICOES).get(i));
    }

    // J1
    @Test(timeout = 1000)
    public void colunaDaOsCavalosDaCartelaDoHerdeiro() {
        Jogo j = jogo(2);
        assertEquals("sem coluna, sem movimentos", 0, (int) j.get(MOVIMENTOS));
        j.ativarColuna(0, 1); // coluna verde de Tolui: 2 cavalos
        assertEquals("coluna verde de Tolui: 2 cavalos", 2, (int) j.get(MOVIMENTOS));
        fim(j, 0);
        assertEquals("o turno novo começa sem movimentos", 0, (int) j.get(MOVIMENTOS));
        j.ativarColuna(1, 0); // coluna vermelha de Altani: 1 cavalo
        assertEquals("coluna vermelha de Altani: 1 cavalo", 1, (int) j.get(MOVIMENTOS));
    }

    // §7: punhos e Khan da coluna ativada.
    @Test(timeout = 1000)
    public void colunaDaOsPunhosEOKhanDaCartela() {
        Jogo j = jogo(2);
        assertEquals("sem coluna, sem punhos", 0, (int) j.get(PUNHOS));
        assertFalse("sem coluna, sem Khan", j.get(KHAN_PENDENTE));
        j.ativarColuna(0, 2); // coluna amarela de Tolui: 1 cavalo, 2 punhos e Khan
        assertEquals("coluna amarela de Tolui: 1 cavalo", 1, (int) j.get(MOVIMENTOS));
        assertEquals("coluna amarela de Tolui: 2 punhos", 2, (int) j.get(PUNHOS));
        assertTrue("coluna amarela de Tolui: Khan", j.get(KHAN_PENDENTE));
        fim(j, 0);
        j.ativarColuna(1, 1); // coluna verde de Altani: 2 cavalos e 1 punho, sem Khan
        assertEquals("coluna verde de Altani: 1 punho", 1, (int) j.get(PUNHOS));
        assertFalse("coluna verde de Altani: sem Khan", j.get(KHAN_PENDENTE));
        fim(j, 1);
        j.ativarColuna(0, (int) j.get(COLUNA_REATIVACAO)); // coluna branca: 2 cavalos e Khan, sem punhos
        assertEquals("coluna branca: 2 cavalos", 2, (int) j.get(MOVIMENTOS));
        assertEquals("coluna branca: sem punhos", 0, (int) j.get(PUNHOS));
        assertTrue("coluna branca: Khan", j.get(KHAN_PENDENTE));
    }

    // RF11, RF12: marcar a coluna na cartela e depois confirmar.
    @Test(timeout = 1000)
    public void marcaEConfirmaColuna() {
        Jogo j = jogo(2);
        j.selecionarColuna(0, 2);
        assertEquals("a coluna amarela marcada", 2, (int) j.get(COLUNA_SELECIONADA));
        assertEquals("marcar não ativa", -1, (int) j.get(COLUNA_ATIVADA));
        j.selecionarColuna(0, 1); // troca a marcação
        assertEquals("marcar outra troca a marcação", 1, (int) j.get(COLUNA_SELECIONADA));
        j.selecionarColuna(0, 1); // desmarca
        assertEquals("marcar de novo desmarca", -1, (int) j.get(COLUNA_SELECIONADA));
        j.selecionarColuna(0, 0);
        j.confirmarColuna(0);
        assertEquals("confirmar ativa a marcada", 0, (int) j.get(COLUNA_ATIVADA));
        assertEquals("confirmar apaga a marcação", -1, (int) j.get(COLUNA_SELECIONADA));
        assertThrows("depois de ativar, não marca outra", IllegalStateException.class, () -> j.selecionarColuna(0, 1));
    }

    @Test(timeout = 1000)
    public void naoMarcaColunaBloqueadaNemConfirmaSemMarcar() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows("não confirma sem marcar",
                IllegalStateException.class, () -> j.confirmarColuna(0));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.confirmarColuna"));
        j.ativarColuna(0, 0);
        fim(j, 0);
        j.ativarColuna(1, 0);
        fim(j, 1);
        e = assertThrows("não marca coluna bloqueada", IllegalStateException.class, () -> j.selecionarColuna(0, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("bloqueada"));
        assertEquals("nada ficou marcado", -1, (int) j.get(COLUNA_SELECIONADA));
        assertThrows("não marca na vez do outro", IllegalStateException.class, () -> j.selecionarColuna(1, 2));
        assertThrows("a coluna 5 não existe", IllegalArgumentException.class, () -> j.selecionarColuna(0, 4));
    }

    // J2
    @Test(timeout = 1000)
    public void naoEncerraTurnoSemAtivarColuna() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows("não encerra o turno sem ativar coluna",
                IllegalStateException.class, () -> j.encerrarTurno(0));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals("continua a vez de Tolui", 0, (int) j.get(JOGADOR_ATUAL));
    }

    // J4, RF04
    @Test(timeout = 1000)
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
        assertSame("o Singleton recebe a partida salva", j, Jogo.carregar(arquivo));
        assertEquals("3 jogadores", 3, j.get(NOMES).size());
        assertEquals("os herdeiros na ordem",
                List.of("Tolui", "Altani", "Jochi"), List.of(j.get(NOMES).get(0), j.get(NOMES).get(1), j.get(NOMES).get(2)));
        assertEquals("as moedas",
                List.of(1, 1, 2), List.of((int) j.get(MOEDAS).get(0), (int) j.get(MOEDAS).get(1), (int) j.get(MOEDAS).get(2)));
        assertEquals("Tolui em r9", "r9", j.get(POSICOES).get(0));
        assertEquals("Altani em r11", "r11", j.get(POSICOES).get(1));
        assertEquals("Jochi em Karakorum", "karakorum", j.get(POSICOES).get(2));
        assertEquals("a vez de Altani", 1, (int) j.get(JOGADOR_ATUAL));
        assertEquals("a coluna verde de Altani", 1, (int) j.get(COLUNA_ATIVADA));
        assertEquals("1 dos 2 movimentos sobrando", 1, (int) j.get(MOVIMENTOS));
        assertTrue("a coluna vermelha de Tolui continua bloqueada", j.get(COLUNAS_BLOQUEADAS).get(0).contains(0));
    }

    @Test(timeout = 1000)
    public void carregarParadaInexistenteFalha() throws Exception {
        Path arquivo = pasta.newFile("parada.txt").toPath();
        jogo(2).salvar(arquivo);
        Files.writeString(arquivo, Files.readString(arquivo).replace("jogador0.posicao=karakorum", "jogador0.posicao=x1"));
        IllegalArgumentException e = assertThrows("parada que não existe no arquivo é recusada",
                IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage(), e.getMessage().contains("x1"));
    }

    // Os movimentos abaixo começam em Karakorum, vizinha de r9, r11, c4 e p4. r9 é vizinha de r8 e de r10.
    // A coluna 0 (vermelha) dá 1 cavalo a Tolui, Altani e Jochi; a coluna 1 (verde) dá 2 a Tolui e Altani.

    // J3: o turno só passa quando o jogador clica em Próximo.
    @Test(timeout = 1000)
    public void turnoNaoPassaQuandoOsMovimentosAcabam() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        j.mover(0, "r10");
        assertEquals("Tolui em r10", "r10", j.get(POSICOES).get(0));
        assertEquals("os movimentos acabaram", 0, (int) j.get(MOVIMENTOS));
        assertEquals("o turno não passa sozinho", 0, (int) j.get(JOGADOR_ATUAL));
        assertEquals("a coluna continua ativada", 1, (int) j.get(COLUNA_ATIVADA));
        fim(j, 0);
        assertEquals("Próximo passa o turno", 1, (int) j.get(JOGADOR_ATUAL));
    }

    // J8
    @Test(timeout = 1000)
    public void moveParaParadaVizinha() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        assertEquals("Tolui em r9", "r9", j.get(POSICOES).get(0));
        assertEquals("o único movimento foi gasto", 0, (int) j.get(MOVIMENTOS));
    }

    // §7.1: cada movimento leva a uma parada vizinha.
    @Test(timeout = 1000)
    public void naoPulaParadas() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        IllegalArgumentException e = assertThrows("r10 não é vizinha de Karakorum",
                IllegalArgumentException.class, () -> j.mover(0, "r10"));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.mover"));
        assertTrue(e.getMessage(), e.getMessage().contains("vizinha"));
        assertEquals("a recusa não move", "karakorum", j.get(POSICOES).get(0));
        assertEquals("a recusa não gasta movimento", 2, (int) j.get(MOVIMENTOS));
    }

    // J9, CA04
    @Test(timeout = 1000)
    public void naoMoveAlemDosMovimentos() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        IllegalStateException e = assertThrows("sem movimentos, não anda",
                IllegalStateException.class, () -> j.mover(0, "r8"));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.mover"));
        assertEquals("Tolui fica em r9", "r9", j.get(POSICOES).get(0));
        assertEquals("o turno não passa sozinho", 0, (int) j.get(JOGADOR_ATUAL));
    }

    // J10
    @Test(timeout = 1000)
    public void moveEmVariosCliques() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "r9");
        assertEquals("sobra 1 movimento", 1, (int) j.get(MOVIMENTOS));
        j.mover(0, "r8");
        assertEquals("Tolui em r8", "r8", j.get(POSICOES).get(0));
        assertEquals("os 2 movimentos foram gastos", 0, (int) j.get(MOVIMENTOS));
    }

    @Test(timeout = 1000)
    public void guardaAsParadasVisitadasEMostraOsDestinos() {
        Jogo j = jogo(2);
        assertEquals("sem coluna, nenhuma visitada", List.of(), j.get(VISITADAS));
        assertEquals("sem coluna, nenhum destino", List.of(), j.get(DESTINOS));
        j.ativarColuna(0, 1);
        assertEquals("a parada inicial conta como visitada", List.of("karakorum"), j.get(VISITADAS));
        assertEquals("as vizinhas de Karakorum", Set.of("r9", "r11", "c4", "p4"), new HashSet<>(j.get(DESTINOS)));
        j.mover(0, "r9");
        j.mover(0, "karakorum"); // pode voltar
        assertEquals("pode voltar a Karakorum, que já foi visitada", List.of("karakorum", "r9"), j.get(VISITADAS));
        assertEquals("sem movimentos, nenhum destino", List.of(), j.get(DESTINOS));
        fim(j, 0);
        assertEquals("o turno novo começa sem visitadas", List.of(), j.get(VISITADAS));
    }

    // J11
    @Test(timeout = 1000)
    public void passaPorParadaOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        fim(j, 0);
        j.ativarColuna(1, 1);
        assertTrue("r9 está ocupada, mas sobra 1 movimento para sair de lá", j.get(DESTINOS).contains("r9"));
        j.mover(1, "r9");
        assertEquals("as vizinhas de r9", Set.of("r6", "r8", "r10", "karakorum"), new HashSet<>(j.get(DESTINOS)));
        j.mover(1, "r8");
        assertEquals("Altani passou por r9 e parou em r8", "r8", j.get(POSICOES).get(1));
    }

    // J12
    @Test(timeout = 1000)
    public void naoTerminaEmParadaSimplesOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        fim(j, 0);
        j.ativarColuna(1, 0);
        assertFalse("com 1 movimento, não termina em r9 ocupada", j.get(DESTINOS).contains("r9"));
        IllegalArgumentException e = assertThrows("não termina em parada simples ocupada",
                IllegalArgumentException.class, () -> j.mover(1, "r9"));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.mover"));
        assertEquals("a recusa não move", "karakorum", j.get(POSICOES).get(1));
    }

    @Test(timeout = 1000)
    public void naoEncerraTurnoEmParadaOcupada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        j.mover(0, "r9");
        fim(j, 0);
        j.ativarColuna(1, 1);
        j.mover(1, "r9");
        IllegalStateException e = assertThrows("não encerra o turno em parada ocupada",
                IllegalStateException.class, () -> j.encerrarTurno(1));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.encerrarTurno"));
        assertEquals("continua a vez de Altani", 1, (int) j.get(JOGADOR_ATUAL));
        j.mover(1, "r10");
        fim(j, 1);
        assertEquals("saiu de r9 e passou o turno", 0, (int) j.get(JOGADOR_ATUAL));
    }

    // J13
    @Test(timeout = 1000)
    public void paradaDuplaAceitaDoisPioes() {
        Jogo j = jogo(3);
        j.ativarColuna(0, 0);
        j.mover(0, "r11");
        fim(j, 0);
        j.ativarColuna(1, 0);
        j.mover(1, "r11");
        assertEquals("a parada dupla r11 aceita o 2º pião", "r11", j.get(POSICOES).get(1));
        fim(j, 1);
        j.ativarColuna(2, 0);
        assertThrows("a parada dupla não aceita o 3º pião", IllegalArgumentException.class, () -> j.mover(2, "r11"));
        assertEquals("a recusa não move", "karakorum", j.get(POSICOES).get(2));
    }

    // J15
    @Test(timeout = 1000)
    public void naoMoveSemAtivarColuna() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows("não anda sem ativar coluna",
                IllegalStateException.class, () -> j.mover(0, "r9"));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.mover"));
        assertEquals("a recusa não move", "karakorum", j.get(POSICOES).get(0));
    }

    // CA03
    @Test(timeout = 1000)
    public void naoMoveForaDoTurno() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertThrows("Altani não anda na vez de Tolui", IllegalStateException.class, () -> j.mover(1, "r9"));
        assertEquals("a recusa não move", "karakorum", j.get(POSICOES).get(1));
    }

    @Test(timeout = 1000)
    public void naoMoveParaAMesmaParadaNemParaParadaInexistente() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertThrows("não anda para onde já está", IllegalArgumentException.class, () -> j.mover(0, "karakorum"));
        assertThrows("parada que não existe", IllegalArgumentException.class, () -> j.mover(0, "x1"));
        assertEquals("as recusas não gastam movimento", 1, (int) j.get(MOVIMENTOS));
    }

    // J17
    @Test(timeout = 1000)
    public void paradaEmPontoDoTabuleiro() {
        Jogo j = jogo(2);
        assertEquals("o ponto de r1", "r1", j.paradaEm(490, 255));
        assertNull("o canto não tem parada", j.paradaEm(10, 10));
    }

    // Dados de que a interface precisa para desenhar os piões e o modo de depuração (RNF10: a view só fala com Jogo).
    @Test(timeout = 1000)
    public void dadosDoMapaParaAInterface() {
        Jogo j = jogo(2);
        assertEquals("34 paradas", 34, List.copyOf(j.get(PONTOS_PARADAS).keySet()).size());
        assertArrayEquals("os 2 medalhões de r4", new int[] {855, 170, 930, 250}, j.get(PONTOS_PARADAS).get("r4"));
        assertEquals("as vizinhas de r4", List.of("r1", "r6"), j.get(VIZINHAS_PARADAS).get("r4"));
        assertTrue("Karakorum tem o raio maior",
                (int) j.get(RAIOS_PARADAS).get("karakorum") > (int) j.get(RAIOS_PARADAS).get("r1"));
        assertEquals("os herdeiros na ordem da tela",
                List.of("Altani", "Jochi", "Chagatai", "Tolui", "Ogedei"), Jogo.getInstance().get(HERDEIROS));
    }

    @Test(timeout = 1000)
    public void carregarArquivoInvalidoFalha() throws Exception {
        Path arquivo = pasta.newFile("ruim.txt").toPath();
        Files.writeString(arquivo, "jogadores=dois\n");
        IllegalArgumentException e = assertThrows("arquivo com número inválido é recusado",
                IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage(), e.getMessage().contains("Arquivo.carregar"));
    }

    // J5
    @Test(timeout = 1000)
    public void carregarHerdeiroInexistenteFalha() throws Exception {
        Path arquivo = pasta.newFile("herdeiro.txt").toPath();
        jogo(2).salvar(arquivo);
        Files.writeString(arquivo, Files.readString(arquivo).replace("Tolui", "Fulano"));
        IllegalArgumentException e = assertThrows("herdeiro que não existe no arquivo é recusado",
                IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage(), e.getMessage().contains("Fulano"));
        assertEquals("a partida anterior continua", "Tolui", Jogo.getInstance().get(NOMES).get(0));
    }

    // J6
    @Test(timeout = 1000)
    public void rejeitaHerdeiroRepetido() {
        IllegalArgumentException e = assertThrows("herdeiro repetido é recusado", IllegalArgumentException.class,
                () -> Jogo.getInstance().novaPartida(List.of("Tolui", "Altani", "Tolui")));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.novaPartida"));
        assertTrue(e.getMessage(), e.getMessage().contains("Tolui"));
    }

    // J7, RF07
    @Test(timeout = 1000)
    public void sorteiaHerdeirosDiferentes() {
        Jogo j = Jogo.getInstance();
        Set<List<String>> combinacoes = new HashSet<>();
        for (int k = 0; k < 20; k++) {
            j.sortear(3);
            assertEquals("3 jogadores sorteados", 3, j.get(NOMES).size());
            List<String> nomes = List.of(j.get(NOMES).get(0), j.get(NOMES).get(1), j.get(NOMES).get(2));
            assertEquals("sem herdeiro repetido", 3, new HashSet<>(nomes).size());
            combinacoes.add(nomes);
        }
        assertTrue("20 sorteios dão pelo menos 2 combinações", combinacoes.size() >= 2);
    }

    @Test(timeout = 1000)
    public void sortearValidaNumeroDeJogadores() {
        IllegalArgumentException e = assertThrows("sortear 6 jogadores é recusado",
                IllegalArgumentException.class, () -> Jogo.getInstance().sortear(6));
        assertTrue(e.getMessage(), e.getMessage().contains("máximo: 5"));
    }

    @Test(timeout = 1000)
    public void colunaUsadaFicaBloqueada() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        j.ativarColuna(1, 0);
        fim(j, 1);
        assertTrue("a coluna vermelha de Tolui ficou bloqueada", j.get(COLUNAS_BLOQUEADAS).get(0).contains(0));
        IllegalStateException e = assertThrows("não ativa a coluna bloqueada",
                IllegalStateException.class, () -> j.ativarColuna(0, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.ativarColuna"));
        assertEquals("a recusa não ativa coluna", -1, (int) j.get(COLUNA_ATIVADA));
    }

    @Test(timeout = 1000)
    public void colunaDeReativacaoLiberaAsOutras() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        fim(j, 0);
        j.ativarColuna(1, 0);
        fim(j, 1);
        j.ativarColuna(0, (int) j.get(COLUNA_REATIVACAO));
        assertFalse("a coluna branca libera a vermelha", j.get(COLUNAS_BLOQUEADAS).get(0).contains(0));
        assertFalse("a coluna branca também fica livre",
                j.get(COLUNAS_BLOQUEADAS).get(0).contains((int) j.get(COLUNA_REATIVACAO)));
        fim(j, 0);
        j.ativarColuna(1, (int) j.get(COLUNA_REATIVACAO));
        fim(j, 1);
        j.ativarColuna(0, 0); // liberada de novo
        assertEquals("a vermelha foi liberada de novo", 0, (int) j.get(COLUNA_ATIVADA));
    }

    // ---------------------------------------------------------------- tributos e Khan
    // Karakorum fica na borda das províncias t4 (yurt), t5 (moeda), t6 (espada) e t9 (yurt).
    // Tolui: coluna 0 = 1 cavalo e 1 punho; coluna 1 = 2 cavalos e 1 punho; coluna 2 = 1 cavalo, 2 punhos e Khan.

    // §5: 1 peça de tributo em cada província, e o Khan fora do tabuleiro.
    @Test(timeout = 1000)
    public void provinciasComecamCom1Peca() {
        Jogo j = jogo(2);
        assertEquals("12 províncias", 12, List.copyOf(j.get(PONTOS_PROVINCIAS).keySet()).size());
        for (String p : List.copyOf(j.get(PONTOS_PROVINCIAS).keySet())) assertEquals(p + ": começa com 1 peça", 1,
                (int) j.get(PECAS_PROVINCIAS).get(p));
        assertEquals("o Khan começa fora do tabuleiro", -1, (int) j.get(POSICAO_KHAN));
        assertEquals("Tolui começa sem espadas", 0, (int) j.get(ESPADAS).get(0));
        assertEquals("Tolui começa sem yurts", 0, (int) j.get(PECAS_YURT).get(0));
    }

    // §7.2
    @Test(timeout = 1000)
    public void pegaTributoDeProvinciaAdjacente() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        assertEquals("as províncias ao lado de Karakorum",
                Set.of("t4", "t5", "t6", "t9"), new HashSet<>(j.get(PROVINCIAS_ALCANCAVEIS)));
        j.pegarTributo(0, "t5"); // moeda, antes de andar
        assertEquals("1 moeda inicial + 1 de t5", 2, (int) j.get(MOEDAS).get(0));
        assertEquals("t5 ficou vazia", 0, (int) j.get(PECAS_PROVINCIAS).get("t5"));
        assertEquals("o punho foi gasto", 0, (int) j.get(PUNHOS));
        assertEquals("sem punhos, nenhuma província alcançável", List.of(), j.get(PROVINCIAS_ALCANCAVEIS));
        IllegalStateException e = assertThrows("sem punhos, não pega tributo",
                IllegalStateException.class, () -> j.pegarTributo(0, "t6"));
        assertTrue(e.getMessage(), e.getMessage().contains("punhos"));
        assertEquals("a recusa não tira a peça de t6", 1, (int) j.get(PECAS_PROVINCIAS).get("t6"));
    }

    @Test(timeout = 1000)
    public void naoPegaTributoDeProvinciaNaoAdjacente() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0);
        IllegalArgumentException e = assertThrows("t1 não fica ao lado de Karakorum",
                IllegalArgumentException.class, () -> j.pegarTributo(0, "t1"));
        assertTrue(e.getMessage(), e.getMessage().contains("Tributos.pegar"));
        assertEquals("a recusa não gasta o punho", 1, (int) j.get(PUNHOS));
        assertEquals("a recusa não tira a peça de t1", 1, (int) j.get(PECAS_PROVINCIAS).get("t1"));
        assertThrows("província que não existe", IllegalArgumentException.class, () -> j.pegarTributo(0, "t99"));
    }

    // §7.2: vale qualquer parada onde o pião esteve no turno, antes, durante ou depois do movimento.
    @Test(timeout = 1000)
    public void pegaTributoDasParadasDoCaminho() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 1);
        j.mover(0, "p4");
        j.mover(0, "p3");
        assertTrue("as províncias ao lado de p4 e p3",
                j.get(PROVINCIAS_ALCANCAVEIS).containsAll(List.of("t4", "t7", "t8", "t9")));
        j.pegarTributo(0, "t7"); // espada, adjacente a p3
        assertEquals("ganhou a espada de t7, ao lado de p3", 1, (int) j.get(ESPADAS).get(0));
        assertEquals("t7 ficou vazia", 0, (int) j.get(PECAS_PROVINCIAS).get("t7"));
    }

    @Test(timeout = 1000)
    public void naoPegaTributoDeProvinciaVaziaNemSemColuna() {
        Jogo j = jogo(2);
        assertThrows("não pega tributo sem coluna", IllegalStateException.class, () -> j.pegarTributo(0, "t5"));
        j.ativarColuna(0, 2); // 2 punhos
        j.pegarTributo(0, "t9");
        assertEquals("ganhou o yurt de t9", 1, (int) j.get(PECAS_YURT).get(0));
        IllegalStateException e = assertThrows("não pega de província vazia",
                IllegalStateException.class, () -> j.pegarTributo(0, "t9"));
        assertTrue(e.getMessage(), e.getMessage().contains("vazia"));
        assertEquals("a recusa não gasta o punho", 1, (int) j.get(PUNHOS));
        assertThrows("não pega tributo na vez do outro", IllegalStateException.class, () -> j.pegarTributo(1, "t4"));
    }

    // §7.3: o Khan é obrigatório quando a coluna tem o ícone dele.
    @Test(timeout = 1000)
    public void khanObrigatorioDaTributosAs3Provincias() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        assertTrue("a coluna amarela manda mover o Khan", j.get(KHAN_PENDENTE));
        IllegalStateException e = assertThrows("não encerra o turno sem mover o Khan",
                IllegalStateException.class, () -> j.encerrarTurno(0));
        assertTrue(e.getMessage(), e.getMessage().contains("Khan"));
        assertEquals("o Khan 1 dá tributo a t4, t5 e t6", List.of("t4", "t5", "t6"), j.get(PROVINCIAS_KHAN).get(1));
        j.moverKhan(0, 1);
        assertEquals("o Khan no lugar 1", 1, (int) j.get(POSICAO_KHAN));
        assertFalse("o Khan já foi movido", j.get(KHAN_PENDENTE));
        for (String p : List.of("t4", "t5", "t6")) assertEquals(p + ": 1 peça inicial + 1 do Khan",
                2, (int) j.get(PECAS_PROVINCIAS).get(p));
        assertEquals("t1 não recebe", 1, (int) j.get(PECAS_PROVINCIAS).get("t1"));
        j.encerrarTurno(0);
        assertEquals("com o Khan movido, o turno passa", 1, (int) j.get(JOGADOR_ATUAL));
    }

    @Test(timeout = 1000)
    public void khanNaoFicaOndeEstaNemSeMoveSemOIcone() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 0); // sem Khan
        IllegalStateException e = assertThrows("a coluna vermelha de Tolui não tem Khan",
                IllegalStateException.class, () -> j.moverKhan(0, 1));
        assertTrue(e.getMessage(), e.getMessage().contains("Tributos.moverKhan"));
        j.encerrarTurno(0);
        j.ativarColuna(1, 0); // Altani: coluna vermelha com Khan
        j.moverKhan(1, 2);
        assertThrows("só uma vez por turno", IllegalStateException.class, () -> j.moverKhan(1, 3));
        j.encerrarTurno(1);
        j.ativarColuna(0, 2);
        IllegalArgumentException e2 = assertThrows("o Khan não fica onde está",
                IllegalArgumentException.class, () -> j.moverKhan(0, 2));
        assertTrue(e2.getMessage(), e2.getMessage().contains("outra"));
        assertThrows("o lugar 5 não existe (4 é a área de melhorias)",
                IllegalArgumentException.class, () -> j.moverKhan(0, 5));
        j.moverKhan(0, 3);
        assertEquals("o Khan no lugar 3", 3, (int) j.get(POSICAO_KHAN));
    }

    // §7.3: no máximo 3 peças por província.
    @Test(timeout = 1000)
    public void provinciaGuardaNoMaximo3Pecas() {
        Jogo j = jogo(2);
        int[][] jogadas = {{0, 2, 0}, {1, 0, 1}, {0, 3, 0}, {1, 3, 1}, {0, 2, 0}}; // jogador, coluna, local do Khan
        for (int[] jogada : jogadas) {
            j.ativarColuna(jogada[0], jogada[1]);
            j.moverKhan(jogada[0], jogada[2]);
            j.encerrarTurno(jogada[0]);
        }
        for (String p : List.of("t1", "t2", "t3")) assertEquals(p + ": no máximo 3 peças",
                3, (int) j.get(PECAS_PROVINCIAS).get(p));
        for (String p : List.of("t4", "t5", "t6")) assertEquals(p + ": no máximo 3 peças",
                3, (int) j.get(PECAS_PROVINCIAS).get(p));
        assertEquals("t7 não recebeu", 1, (int) j.get(PECAS_PROVINCIAS).get("t7"));
    }

    @Test(timeout = 1000)
    public void novaPartidaReiniciaTributosEKhan() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        j.pegarTributo(0, "t5");
        j.moverKhan(0, 0);
        jogo(2);
        assertEquals("o Khan volta para fora do tabuleiro", -1, (int) j.get(POSICAO_KHAN));
        assertEquals("t5 volta a ter 1 peça", 1, (int) j.get(PECAS_PROVINCIAS).get("t5"));
        assertEquals("t1 volta a ter 1 peça", 1, (int) j.get(PECAS_PROVINCIAS).get("t1"));
        assertEquals("Tolui volta a ter 1 moeda", 1, (int) j.get(MOEDAS).get(0));
    }

    @Test(timeout = 1000)
    public void provinciaEKhanEmPontoDoTabuleiro() {
        Jogo j = jogo(2);
        for (String p : List.copyOf(j.get(PONTOS_PROVINCIAS).keySet())) {
            int[] c = j.get(PONTOS_PROVINCIAS).get(p);
            assertEquals(p + ": o ponto do ícone", p, j.provinciaEm(c[0], c[1]));
            assertNull(p + ": o ícone não fica em cima de uma parada", j.paradaEm(c[0], c[1]));
        }
        for (int i = 0; i < j.get(PONTOS_KHAN).size(); i++) {
            int[] c = j.get(PONTOS_KHAN).get(i);
            assertEquals("Khan " + i, i, j.localKhanEm(c[0], c[1]));
            assertNull("Khan " + i + ": não é parada", j.paradaEm(c[0], c[1]));
            assertNull("Khan " + i + ": não é província", j.provinciaEm(c[0], c[1]));
        }
        assertEquals("o canto não é lugar do Khan", -1, j.localKhanEm(10, 10));
        assertNull("o canto não é província", j.provinciaEm(10, 10));
    }

    // ---------------------------------------------------------------- estoque de tributos
    // Regras, p. 3: 10 espadas, 10 yurts e 15 moedas no jogo.

    @Test(timeout = 1000)
    public void estoqueDeTributosELimitado() {
        Jogo j = jogo(2);
        assertEquals("10 espadas, 4 nas províncias", 6, (int) j.get(ESTOQUE).get("espada"));
        assertEquals("10 yurts, 4 nas províncias", 6, (int) j.get(ESTOQUE).get("yurt"));
        assertEquals("15 moedas, 4 nas províncias e 2 com os jogadores", 9, (int) j.get(ESTOQUE).get("moeda"));
        j.ativarColuna(0, 2);
        j.pegarTributo(0, "t6"); // da província para o jogador: o estoque não muda
        assertEquals("da província para o jogador, o estoque não muda", 6, (int) j.get(ESTOQUE).get("espada"));
        j.moverKhan(0, 1); // t4, t5 e t6 recebem 1 peça do estoque
        assertEquals("t4, t5 e t6 recebem 1 peça do estoque", 5, (int) j.get(ESTOQUE).get("espada"));
        assertEquals("1 yurt do estoque para t4", 5, (int) j.get(ESTOQUE).get("yurt"));
        assertEquals("1 moeda do estoque para t5", 8, (int) j.get(ESTOQUE).get("moeda"));
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
    @Test(timeout = 1000)
    public void partidaComecaCom3CidadesReveladas() {
        Jogo j = jogo(2);
        assertEquals("9 cidades", 9, List.copyOf(j.get(PONTOS_CIDADES).keySet()).size());
        int reveladas = 0;
        for (String c : List.copyOf(j.get(PONTOS_CIDADES).keySet())) {
            if (j.get(CIDADES_REVELADAS).contains(c)) {
                reveladas++;
                assertEquals(c + ": revelada com 4 tesouros", 4, j.get(TESOUROS_CIDADES).get(c).size());
            } else {
                assertEquals(c + ": na pilha, sem tesouros", List.of(), j.get(TESOUROS_CIDADES).get(c));
            }
            assertEquals(c + ": sem dono", -1, (int) j.get(DONOS_CIDADES).get(c));
        }
        assertEquals("3 cidades reveladas", 3, reveladas);
        assertEquals("6 na pilha", 6, (int) j.get(CIDADES_NA_PILHA));
    }

    @Test(timeout = 1000)
    public void prepararCidadesUsaAOrdemEOSacoDados() {
        Jogo j = jogo(2);
        j.prepararCidades(ORDEM, saco(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE, Tesouro.ESPECIARIA));
        assertEquals("Sarai recebe os 4 primeiros do saco",
                List.of("boi", "seda", "ferro", "pele"), j.get(TESOUROS_CIDADES).get("Sarai"));
        assertEquals("Kiev recebe o que sobrou", List.of("especiaria"), j.get(TESOUROS_CIDADES).get("Kiev"));
        assertTrue("Pequim é a 3ª revelada", j.get(CIDADES_REVELADAS).contains("Pequim"));
        assertEquals("o saco acabou antes de Pequim", List.of(), j.get(TESOUROS_CIDADES).get("Pequim"));
        assertFalse("Bagdá continua na pilha", j.get(CIDADES_REVELADAS).contains("Bagdá"));
    }

    // Regras, p. 10: o 1º tesouro custa 1 espada; os seguintes da mesma cidade, no mesmo turno, custam 2.
    @Test(timeout = 1000)
    public void atacaCidadeAdjacenteComEspadas() {
        Jogo j = toluiComDuasEspadasEmR9(saco(Tesouro.BOI, Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO,
                Tesouro.PELE, Tesouro.PELE, Tesouro.PELE, Tesouro.PELE));
        assertEquals("Tolui pegou 2 espadas", 2, (int) j.get(ESPADAS).get(0));
        assertEquals("Sarai está ao lado de r9", List.of("Sarai"), j.get(CIDADES_ATACAVEIS));
        j.atacarCidade(0, "Sarai", "boi");
        assertEquals("o 1º tesouro custa 1 espada", 1, (int) j.get(ESPADAS).get(0));
        assertEquals("ganhou o boi", 1, (int) j.get(TESOUROS).get(0).get("boi"));
        assertEquals("Sarai ficou com 3", List.of("boi", "seda", "ferro"), j.get(TESOUROS_CIDADES).get("Sarai"));
        assertEquals("o próximo tesouro custaria 2 espadas", List.of(), j.get(CIDADES_ATACAVEIS));
        IllegalStateException e = assertThrows("o 2º tesouro custa 2 espadas",
                IllegalStateException.class, () -> j.atacarCidade(0, "Sarai", "seda"));
        assertTrue(e.getMessage(), e.getMessage().contains("2 espada"));
        assertEquals("a recusa não gasta a espada", 1, (int) j.get(ESPADAS).get(0));
        assertEquals("a recusa não tira o tesouro", 3, j.get(TESOUROS_CIDADES).get("Sarai").size());
    }

    @Test(timeout = 1000)
    public void naoAtacaCidadeLongeNemEscondidaNemTesouroQueNaoTem() {
        Jogo j = jogo(2);
        j.prepararCidades(ORDEM, saco(Tesouro.BOI, Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE,
                Tesouro.PELE, Tesouro.PELE, Tesouro.PELE));
        assertThrows("não ataca sem coluna", IllegalStateException.class, () -> j.atacarCidade(0, "Sarai", "boi"));
        j.ativarColuna(0, 2);
        j.moverKhan(0, 1);
        j.pegarTributo(0, "t6");
        IllegalArgumentException e = assertThrows("Tolui ainda está em Karakorum, longe de Sarai",
                IllegalArgumentException.class, () -> j.atacarCidade(0, "Sarai", "boi"));
        assertTrue(e.getMessage(), e.getMessage().contains("adjacente")); // Tolui ainda está em Karakorum
        j.mover(0, "r9");
        assertThrows("Sarai não tem especiaria",
                IllegalArgumentException.class, () -> j.atacarCidade(0, "Sarai", "especiaria"));
        assertThrows("ouro não é tesouro", IllegalArgumentException.class, () -> j.atacarCidade(0, "Sarai", "ouro"));
        IllegalStateException e2 = assertThrows("Bagdá não está revelada",
                IllegalStateException.class, () -> j.atacarCidade(0, "Bagdá", "boi"));
        assertTrue(e2.getMessage(), e2.getMessage().contains("revelada"));
        assertEquals("as recusas não gastam a espada", 1, (int) j.get(ESPADAS).get(0));
    }

    // Regras, p. 10: quem toma o último tesouro conquista a cidade, e a próxima cidade da pilha é revelada.
    @Test(timeout = 1000)
    public void conquistaRevelaAProximaCidade() {
        // Sarai recebe só 1 tesouro: o saco tem 1 peça para ela e as outras cidades ficam vazias.
        Jogo j = jogo(2);
        j.prepararCidades(List.of("Kiev", "Pequim", "Sarai", "Bagdá", "Cabul", "Moscou", "Samarcanda", "Kaesong",
                "Cantão"), saco(Tesouro.BOI, Tesouro.BOI, Tesouro.BOI, Tesouro.BOI, Tesouro.SEDA, Tesouro.SEDA,
                Tesouro.SEDA, Tesouro.SEDA, Tesouro.PELE));
        assertEquals("Sarai tem 1 tesouro só", List.of("pele"), j.get(TESOUROS_CIDADES).get("Sarai"));
        j.ativarColuna(0, 2);
        j.moverKhan(0, 1);
        j.pegarTributo(0, "t6");
        j.mover(0, "r9");
        assertEquals("tomar o último tesouro avisa CIDADE e CONQUISTA",
                List.of(Evento.CIDADE, Evento.CONQUISTA), Partidas.eventos(j, () -> j.atacarCidade(0, "Sarai", "pele")));
        assertEquals("a última cidade atacada", "Sarai", j.get(ULTIMA_CIDADE));
        assertEquals("o último tesouro tomado", "pele", j.get(ULTIMO_TESOURO));
        assertEquals("Tolui é o dono de Sarai", 0, (int) j.get(DONOS_CIDADES).get("Sarai"));
        assertEquals("restam 10 yurts: 1 em Karakorum (Tolui) e 1 no meio de Sarai",
                10, (int) j.get(YURTS_RESTANTES).get(0));
        assertEquals("ganhou a pele", 1, (int) j.get(TESOUROS).get(0).get("pele"));
        assertEquals("a espada foi gasta no ataque", 0, (int) j.get(ESPADAS).get(0));
        assertTrue("a conquista revela Bagdá, a próxima da pilha", j.get(CIDADES_REVELADAS).contains("Bagdá"));
        assertEquals("ficam 5 cidades na pilha", 5, (int) j.get(CIDADES_NA_PILHA));
        IllegalStateException e = assertThrows("cidade conquistada não pode ser atacada",
                IllegalStateException.class, () -> j.atacarCidade(0, "Sarai", "pele"));
        assertTrue(e.getMessage(), e.getMessage().contains("conquistada"));
        assertFalse("Sarai sai das cidades atacáveis", j.get(CIDADES_ATACAVEIS).contains("Sarai"));
    }

    // C2 (vídeo 1): a conquista põe um yurt no meio da cidade; quem já usou os 12 yurts não toma o último tesouro.
    @Test(timeout = 1000)
    public void semYurtNaoConquistaACidade() {
        Mapa mapa = new Mapa();
        Yurts yurts = new Yurts(mapa);
        Cidades cidades = new Cidades(mapa, yurts);
        cidades.preparar(List.of("Kiev", "Pequim", "Sarai", "Bagdá", "Cabul", "Moscou", "Samarcanda", "Kaesong",
                "Cantão"), saco(Tesouro.BOI, Tesouro.BOI, Tesouro.BOI, Tesouro.BOI, Tesouro.SEDA, Tesouro.SEDA,
                Tesouro.SEDA, Tesouro.SEDA, Tesouro.PELE)); // Sarai fica com 1 tesouro só
        Cidade sarai = mapa.getCidade("Sarai");
        Jogador tolui = new Jogador(Herdeiro.TOLUI, 1);
        tolui.setPosicao(mapa.getParada("r9")); // vizinha de Sarai
        tolui.ganhar(Tributo.ESPADA);
        for (Parada p : mapa.getParadas())
            if (p.getCapacidadeYurts() > 0 && yurts.restantes(tolui) > 0) p.getYurts().add(tolui);
        assertEquals("os 12 yurts de Tolui estão no mapa", 0, yurts.restantes(tolui));
        Turno turno = new Turno(tolui, 1);
        assertFalse("sem yurt, Sarai (1 tesouro) não é atacável", cidades.atacaveis(turno).contains(sarai));
        IllegalStateException e = assertThrows("sem yurt, não toma o último tesouro", IllegalStateException.class,
                () -> cidades.atacar(tolui, turno, sarai, Tesouro.PELE));
        assertTrue(e.getMessage(), e.getMessage().contains("12 yurts"));
        assertEquals("nada mudou: o tesouro continua em Sarai", List.of(Tesouro.PELE), sarai.getTesouros());
        assertEquals("a recusa não gasta a espada", 1, turno.disponivel(Tributo.ESPADA));
        // Com 1 yurt de volta, a conquista vale.
        mapa.getParada("r9").getYurts().remove(tolui);
        assertTrue("com 1 yurt, Sarai é atacável", cidades.atacaveis(turno).contains(sarai));
        assertTrue("com 1 yurt, o ataque conquista Sarai", cidades.atacar(tolui, turno, sarai, Tesouro.PELE));
        assertEquals("Tolui é o dono de Sarai", tolui, sarai.getDono());
        assertEquals("o último yurt foi para o meio de Sarai", 0, yurts.restantes(tolui));
    }

    // ---------------------------------------------------------------- yurts
    // t9 e t4 são as províncias de yurt vizinhas de Karakorum. r11 é uma parada dupla vizinha de Karakorum.

    // Regras, p. 11: o yurt é construído numa parada onde o pião esteve no turno, gastando 1 peça de yurt.
    @Test(timeout = 1000)
    public void constroiYurtNumaParadaDoCaminho() {
        // Com o Ogedei, porque o Tolui também constrói nas paradas vizinhas ao pião (habilidade, em HabilidadesTest).
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Ogedei", "Altani"));
        j.prepararBonus(List.of());
        Partidas.escolherSecretas(j);
        assertEquals("Ogedei começa com 12 yurts", 12, (int) j.get(YURTS_RESTANTES).get(0));
        j.ativarColuna(0, 0); // Ogedei: 2 cavalos e 1 punho
        assertThrows("não constrói sem peça de yurt",
                IllegalStateException.class, () -> j.construirYurt(0, "karakorum"));
        j.pegarTributo(0, "t9");
        assertEquals("Karakorum não pode ter yurts", List.of(), j.get(PARADAS_PARA_YURT));
        IllegalArgumentException e = assertThrows("não constrói em Karakorum",
                IllegalArgumentException.class, () -> j.construirYurt(0, "karakorum"));
        assertTrue(e.getMessage(), e.getMessage().contains("Karakorum"));
        j.mover(0, "r9");
        assertEquals("r9 está no caminho", List.of("r9"), j.get(PARADAS_PARA_YURT));
        assertThrows("não esteve em r8", IllegalArgumentException.class, () -> j.construirYurt(0, "r8"));
        int estoque = (int) j.get(ESTOQUE).get("yurt");
        j.construirYurt(0, "r9");
        assertEquals("o yurt de Ogedei em r9", List.of(0), j.get(YURTS_NAS_PARADAS).get("r9"));
        assertEquals("a peça de yurt foi gasta", 0, (int) j.get(PECAS_YURT).get(0));
        assertEquals("1 dos 12 yurts no mapa", 11, (int) j.get(YURTS_RESTANTES).get(0));
        assertEquals("a peça usada volta para o estoque", estoque + 1, (int) j.get(ESTOQUE).get("yurt"));
        assertEquals("sem peça, nenhuma parada", List.of(), j.get(PARADAS_PARA_YURT));
    }

    @Test(timeout = 1000)
    public void paradaDuplaTemLugarPara2Yurts() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2); // Tolui: 1 cavalo, 2 punhos e Khan
        j.pegarTributo(0, "t9");
        j.pegarTributo(0, "t4");
        j.mover(0, "r11");
        j.construirYurt(0, "r11");
        j.construirYurt(0, "r11");
        assertEquals("2 yurts de Tolui na parada dupla r11", List.of(0, 0), j.get(YURTS_NAS_PARADAS).get("r11"));
        assertEquals("12 - 2 em r11 - 1 em Karakorum (Tolui)", 9, (int) j.get(YURTS_RESTANTES).get(0));
    }

    @Test(timeout = 1000)
    public void naoConstroiOndeJaTemYurt() {
        Jogo j = jogo(2);
        j.ativarColuna(0, 2);
        j.pegarTributo(0, "t9");
        j.pegarTributo(0, "t4");
        j.mover(0, "r9");
        j.construirYurt(0, "r9");
        IllegalArgumentException e = assertThrows("r9 já tem yurt",
                IllegalArgumentException.class, () -> j.construirYurt(0, "r9"));
        assertTrue(e.getMessage(), e.getMessage().contains("não cabe"));
        assertEquals("a recusa não gasta a peça", 1, (int) j.get(PECAS_YURT).get(0));
    }

    // Regras, p. 7: o jogador pode pular os próprios yurts ao se mover; a parada pulada não conta como visitada.
    @Test(timeout = 1000)
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
        assertTrue("com o yurt em r9, Tolui chega às vizinhas de r9",
                j.get(DESTINOS).containsAll(List.of("r9", "r8", "r10", "r6")));
        j.mover(0, "r8"); // pula r9
        assertEquals("Tolui pulou r9 e chegou em r8", "r8", j.get(POSICOES).get(0));
        assertEquals("pular custa 1 movimento só", 0, (int) j.get(MOVIMENTOS));
        assertEquals("a parada pulada não conta como visitada", List.of("karakorum", "r8"), j.get(VISITADAS));
        fim(j, 0);
        j.ativarColuna(1, 0); // Altani não pode pular o yurt de Tolui
        assertFalse("Altani não pula o yurt de Tolui", j.get(DESTINOS).contains("r8"));
        IllegalArgumentException e = assertThrows("Altani não chega em r8 com 1 movimento",
                IllegalArgumentException.class, () -> j.mover(1, "r8"));
        assertTrue(e.getMessage(), e.getMessage().contains("Jogo.mover"));
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
    @Test(timeout = 1000)
    public void entregaTesourosAosConselheiros() {
        Jogo j = jogoSemCartas(2);
        assertEquals("7 conselheiros", 7, List.copyOf(j.get(PEDIDOS_CONSELHEIROS).keySet()).size());
        assertEquals("r11 pede 2 ferros", List.of("ferro", "ferro"), j.get(PEDIDOS_CONSELHEIROS).get("r11"));
        assertEquals("Karakorum pede ferro, pele e boi",
                List.of("ferro", "pele", "boi"), j.get(PEDIDOS_CONSELHEIROS).get("karakorum"));
        assertEquals("c8 aceita 3 quaisquer",
                List.of("qualquer", "qualquer", "qualquer"), j.get(PEDIDOS_CONSELHEIROS).get("c8"));
        assertEquals("Karakorum dá 2 votos", 2, (int) j.get(VOTOS_CONSELHEIROS).get("karakorum"));
        Jogador tolui = j.jogador(0);
        for (Tesouro t : List.of(Tesouro.FERRO, Tesouro.FERRO, Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI))
            tolui.ganharTesouro(t);
        j.ativarColuna(0, 1);
        assertEquals("Tolui pode entregar em Karakorum", List.of("karakorum"), j.get(CONSELHEIROS_PARA_ENTREGA));
        j.entregarTesouros(0, "karakorum"); // esteve lá no começo do turno
        assertEquals("os 2 votos de Karakorum", 2, (int) j.get(VOTOS).get(0));
        assertEquals("a ficha neutra anda junto", 2, (int) j.get(VOTOS_NO_CONSELHO));
        assertEquals("3 ferros - 1", 2, (int) j.get(TESOUROS).get(0).get("ferro"));
        assertEquals("o boi foi entregue", 0, (int) j.get(TESOUROS).get(0).get("boi"));
        IllegalArgumentException e = assertThrows("não entrega onde não esteve",
                IllegalArgumentException.class, () -> j.entregarTesouros(0, "r11"));
        assertTrue(e.getMessage(), e.getMessage().contains("não esteve"));
        j.mover(0, "r11");
        j.entregarTesouros(0, "r11");
        assertEquals("1 voto de r11", 3, (int) j.get(VOTOS).get(0));
        assertEquals("os 2 ferros foram entregues", 0, (int) j.get(TESOUROS).get(0).get("ferro"));
        IllegalStateException e2 = assertThrows("sem os 2 ferros, não entrega de novo",
                IllegalStateException.class, () -> j.entregarTesouros(0, "r11"));
        assertTrue(e2.getMessage(), e2.getMessage().contains("2 ferro"));
        assertEquals("sem tesouros, nenhum conselheiro", List.of(), j.get(CONSELHEIROS_PARA_ENTREGA));
        assertThrows("r9 não tem conselheiro", IllegalArgumentException.class, () -> j.entregarTesouros(0, "r9"));
    }

    // No conselheiro que aceita 3 tesouros quaisquer, vão os que o jogador tem em maior quantidade.
    @Test(timeout = 1000)
    public void conselheiroDe3QuaisquerUsaOsTesourosMaisNumerosos() {
        Conselheiro c8 = new Mapa().getConselheiro("c8");
        Jogador j = new Jogador(Herdeiro.TOLUI, 1);
        j.ganharTesouro(Tesouro.SEDA);
        j.ganharTesouro(Tesouro.PELE);
        assertNull("só 2 tesouros não bastam", Kurultai.entrega(c8, j));
        j.ganharTesouro(Tesouro.PELE);
        j.ganharTesouro(Tesouro.PELE);
        assertEquals("3 peles, 1 seda: as 2 peles e, no empate, o primeiro tipo",
                List.of(Tesouro.PELE, Tesouro.PELE, Tesouro.SEDA), Kurultai.entrega(c8, j));
        j.ganharTesouro(Tesouro.SEDA);
        assertEquals("as mais numerosas: pele, seda e pele",
                List.of(Tesouro.PELE, Tesouro.SEDA, Tesouro.PELE), Kurultai.entrega(c8, j));
    }

    // Regras, p. 13 e 18: o primeiro a cumprir uma carta de conselheiro aberto ganha 1 voto; ninguém mais ganha.
    @Test(timeout = 1000)
    public void primeiroACumprirACartaGanha1Voto() {
        Jogo j = jogo(2);
        j.prepararCartas(List.of("espadas4", "yurts10", "cidades3", "regioes2"));
        assertEquals("as 4 cartas na ordem dada",
                List.of("espadas4", "yurts10", "cidades3", "regioes2"), List.copyOf(j.get(CARTAS_ABERTAS).keySet()));
        assertEquals("o texto da carta", "Primeiro a ter 4 espadas", j.get(CARTAS_ABERTAS).get("espadas4"));
        for (int k = 0; k < 3; k++) j.jogador(0).ganhar(Tributo.ESPADA);
        j.ativarColuna(0, 0);
        assertEquals("a 4ª espada avisa TRIBUTO e CARTA",
                List.of(Evento.TRIBUTO, Evento.CARTA), Partidas.eventos(j, () -> j.pegarTributo(0, "t6")));
        assertEquals("a última província", "t6", j.get(ULTIMA_PROVINCIA));
        assertEquals("a última carta", "espadas4", j.get(ULTIMA_CARTA));
        assertEquals("Tolui é o dono da carta", 0, (int) j.get(DONOS_CARTAS).get("espadas4"));
        assertEquals("a carta yurts10 continua sem dono", -1, (int) j.get(DONOS_CARTAS).get("yurts10"));
        assertEquals("o voto da carta", 1, (int) j.get(VOTOS).get(0));
        fim(j, 0);
        for (int k = 0; k < 4; k++) j.jogador(1).ganhar(Tributo.ESPADA); // Altani também tem 4 espadas
        j.ativarColuna(1, 1);
        j.pegarTributo(1, "t5"); // qualquer ação faz a verificação das cartas
        assertEquals("a carta já foi ganha por Tolui", 0, (int) j.get(VOTOS).get(1));
        assertEquals("Tolui continua o dono", 0, (int) j.get(DONOS_CARTAS).get("espadas4"));
    }

    // Regras, p. 14: com 2 jogadores, o fim é disparado quando o conselho chega a 10 votos. A rodada atual
    // termina e há mais uma rodada completa; depois disso, ninguém mais joga.
    @Test(timeout = 1000)
    public void fimDaPartidaDepoisDaUltimaRodada() {
        Jogo j = jogoSemCartas(2);
        assertEquals("2 jogadores: o fim é com 10 votos", 10, (int) j.get(LIMITE_VOTOS));
        j.jogador(0).ganharVotos(8);
        for (Tesouro t : List.of(Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI)) j.jogador(0).ganharTesouro(t);
        j.ativarColuna(0, 0);
        assertFalse("8 votos ainda não disparam o fim", j.get(FIM_DISPARADO));
        assertEquals("a entrega dos 10 votos avisa CONSELHEIRO e FIM_DISPARADO",
                List.of(Evento.CONSELHEIRO, Evento.FIM_DISPARADO),
                Partidas.eventos(j, () -> j.entregarTesouros(0, "karakorum"))); // 10 votos
        assertEquals("o último conselheiro", "karakorum", j.get(ULTIMO_CONSELHEIRO));
        assertTrue("o fim foi disparado", j.get(FIM_DISPARADO));
        assertEquals("Altani termina a rodada, e cada um joga mais uma vez", 3, (int) j.get(TURNOS_RESTANTES));
        int[][] turnos = {{0, 0}, {1, 1}, {0, 1}, {1, 2}};
        for (int k = 0; k < turnos.length; k++) {
            int[] t = turnos[k];
            if (k > 0) j.ativarColuna(t[0], t[1]);
            assertFalse("turno " + k + ": a partida ainda não terminou", j.get(PARTIDA_TERMINADA));
            fim(j, t[0]);
        }
        assertTrue("depois dos 3 turnos, a partida terminou", j.get(PARTIDA_TERMINADA));
        assertEquals("Tolui vence", List.of(0), j.get(VENCEDORES));
        IllegalStateException e = assertThrows("ninguém joga depois do fim",
                IllegalStateException.class, () -> j.ativarColuna(1, 0));
        assertTrue(e.getMessage(), e.getMessage().contains("terminou"));
    }

    @Test(timeout = 1000)
    public void empateTemVariosVencedores() {
        Jogo j = jogoSemCartas(3);
        // Cartas secretas que não dão votos nesta situação (o yurt do Tolui em Karakorum não tem região).
        j.prepararSecretas(List.of("viajante", "mensageiro", "prefeito", "cidadao", "governador", "bajulador"));
        Partidas.escolherSecretas(j);
        j.jogador(0).ganharVotos(2);
        j.jogador(2).ganharVotos(2);
        assertEquals("Tolui e Jochi empatam com 2 votos", List.of(0, 2), j.get(VENCEDORES));
    }

    // Observer (cap. 17): cada mudança de estado avisa os observadores, que buscam os dados com get.
    @Test(timeout = 1000)
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
            eventos = Partidas.eventos(j, () -> {
                j.selecionarColuna(0, 0);
                j.confirmarColuna(0);
                j.mover(0, "r9");
                fim(j, 0);
            });
            assertEquals("o turno passou para Altani", 1, (int) j.get(JOGADOR_ATUAL));
            assertEquals("a partida nova avisa PARTIDA",
                    List.of(Evento.PARTIDA), Partidas.eventos(j, () -> j.novaPartida(List.of("Jochi", "Ogedei"))));
            assertEquals("um erro não avisa ninguém", List.of(), Partidas.eventos(j, () -> assertThrows("Tolui não joga na vez de Jochi", IllegalStateException.class,
                    () -> j.mover(0, "r9")))); // erro: ninguém é avisado
        } finally {
            j.remove(obs);
        }
        assertEquals("marcar, confirmar, mover e encerrar avisam SELECAO, COLUNA, MOVIMENTO e TURNO",
                List.of(Evento.SELECAO, Evento.COLUNA, Evento.MOVIMENTO, Evento.TURNO), eventos);
        assertEquals("no 3º aviso, o pião já está em r9", "r9", posicoes.get(2));
        assertEquals("o autor de TURNO é quem encerrou; o de PARTIDA, ninguém (-1)", List.of(0, 0, 0, 0, -1), autores);
    }

    // Grupo 7: no último turno, o evento é FIM, e não TURNO.
    @Test(timeout = 1000)
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
        assertEquals("o último turno avisa FIM, e não TURNO",
                List.of(Evento.FIM), Partidas.eventos(j, () -> j.encerrarTurno(1)));
        assertEquals("o autor do FIM é Altani", 1, (int) j.get(AUTOR_DO_EVENTO));
        assertTrue("a partida terminou", j.get(PARTIDA_TERMINADA));
        assertEquals("Tolui vence", List.of(0), j.get(VENCEDORES));
    }

    // Toda chave declarada em ObservadoIF tem valor numa partida em andamento.
    @Test(timeout = 1000)
    public void todaChaveTemValor() throws IllegalAccessException {
        Jogo j = jogo(2);
        int chaves = 0;
        for (Field f : ObservadoIF.class.getFields()) {
            if (f.getType() != Chave.class) continue;
            j.get((Chave<?>) f.get(null)); // sem valor: IllegalArgumentException
            chaves++;
        }
        assertEquals("102 chaves: grupos 1 a 7 com 24 + 3 + 18 + 22 + 11 + 10 + 14", 102, chaves);
    }

    // Turno e tabuleiro da 4ª iteração: escolha das cartas secretas, área de melhorias e peças de bônus.
    @Test(timeout = 1000)
    public void turnoETabuleiroDaQuartaIteracao() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani"));
        assertEquals("Tolui escolhe a carta secreta primeiro", 0, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
        j.escolherCartaSecreta(0, j.get(OPCOES_SECRETAS).get(0).get(0));
        assertEquals("depois, Altani", 1, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
        j.escolherCartaSecreta(1, j.get(OPCOES_SECRETAS).get(1).get(0));
        assertEquals("todos escolheram", -1, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
        assertEquals("5 espaços na área", 5, j.get(AREA_MELHORIAS).size());
        assertFalse("a área começa cheia", j.get(AREA_MELHORIAS).contains(null));
        assertEquals("44 - 5 na área", 44 - 5, (int) j.get(MELHORIAS_NA_PILHA));
        assertEquals("uma peça de bônus por conselheiro",
                j.get(PEDIDOS_CONSELHEIROS).keySet(), j.get(BONUS_NOS_CONSELHEIROS).keySet());
        assertFalse("nenhum conselheiro sem peça", j.get(BONUS_NOS_CONSELHEIROS).containsValue(null));
        // Antes de ativar a coluna, o turno não tem nada.
        assertEquals("os 3 recursos virtuais",
                List.of("yurt", "espada", "moeda"), List.copyOf(j.get(VIRTUAIS).keySet()));
        assertFalse("antes da coluna, nenhum recurso virtual", j.get(VIRTUAIS).values().stream().anyMatch(n -> n != 0));
        assertEquals("antes da coluna, nenhum tesouro virtual", List.of(), j.get(TESOUROS_VIRTUAIS));
        assertEquals("nenhum baú aberto", List.of(), j.get(TESOUROS_BAU));
        assertEquals("nenhuma melhoria grátis", 0, (int) j.get(MELHORIAS_GRATIS));
        assertFalse("Tolui não é a Chagatai", j.get(PODE_TROCAR_CAVALO_POR_ESPADA));
        assertFalse("Tolui não é o Ogedei", j.get(PODE_TOMAR_TESOURO_COM_MOEDA));
        // Comprar uma melhoria da área: ela vai para a coluna da sua cor, e o espaço fica vazio.
        j.jogador(0).ganhar(Tributo.MOEDA); // 1 + 1 = 2 moedas, o custo da melhoria
        j.ativarColuna(0, 0);
        String comprada = j.get(AREA_MELHORIAS).get(0);
        j.comprarMelhoria(0, 0);
        assertNull("o espaço 0 ficou vazio", j.get(AREA_MELHORIAS).get(0));
        assertTrue("a melhoria comprada foi para uma coluna de Tolui",
                j.get(MELHORIAS).get(0).stream().anyMatch(c -> c.contains(comprada)));
    }

    // Dados dos jogadores da 4ª iteração: piões, melhorias, peças de bônus e cartas secretas.
    @Test(timeout = 1000)
    public void dadosDosJogadoresDaQuartaIteracao() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Altani", "Jochi"));
        j.prepararBonus(List.of());
        assertEquals("Altani tem 2 piões e Jochi 1, todos em Karakorum",
                List.of(List.of("karakorum", "karakorum"), List.of("karakorum")), j.get(PEOES));
        assertEquals("começam com o pião 1", List.of(0, 0), j.get(PEOES_ATIVOS));
        j.selecionarPeao(0, 1);
        assertEquals("Altani escolheu o pião 2", 1, (int) j.get(PEOES_ATIVOS).get(0));
        // Jochi começa com 2 melhorias: uma na coluna da sua cor e outra na coluna branca.
        List<List<String>> colunas = j.get(MELHORIAS).get(1);
        assertEquals("4 colunas", 4, colunas.size());
        assertEquals("1 melhoria do Jochi na coluna branca", 1, colunas.get(3).size());
        assertEquals("2 melhorias do Jochi", 2, colunas.stream().mapToInt(List::size).sum());
        assertEquals("ninguém começa com peças de bônus", List.of(List.of(), List.of()), j.get(BONUS));
        assertEquals("nenhuma peça usada", List.of(List.of(), List.of()), j.get(BONUS_USADOS));
        assertEquals("2 cartas secretas para escolher", 2, j.get(OPCOES_SECRETAS).get(0).size());
        assertEquals("ninguém escolheu a carta secreta ainda", Arrays.asList(null, null), j.get(CARTAS_SECRETAS));
        Partidas.escolherSecretas(j);
        assertEquals("Tolui ficou com a 1ª das 2", j.get(OPCOES_SECRETAS).get(0).get(0), j.get(CARTAS_SECRETAS).get(0));
        assertEquals("uma medida por jogador", 2, j.get(MEDIDAS_SECRETAS).size());
        assertEquals("uma contagem de votos por jogador", 2, j.get(VOTOS_SECRETAS).size());
        assertThrows("as listas de get não podem ser alteradas pela View",
                UnsupportedOperationException.class, () -> j.get(MELHORIAS).get(1).get(3).add("x"));
    }

    // Dados fixos da 4ª iteração: área de melhorias, peças de bônus, cartas secretas e regiões.
    @Test(timeout = 1000)
    public void dadosFixosDaQuartaIteracao() {
        Jogo j = jogo(2);
        assertEquals("o lugar 4 do Khan é a área de melhorias", 4, (int) j.get(KHAN_AREA));
        assertEquals("4 províncias do Khan e a área", 5, j.get(PONTOS_KHAN).size());
        assertEquals("a área não tem províncias", List.of(), j.get(PROVINCIAS_KHAN).get(j.get(KHAN_AREA)));
        assertEquals("o índice da pilha de melhorias", 5, (int) j.get(PILHA_MELHORIAS));
        assertEquals("a melhoria custa 2 moedas", 2, (int) j.get(CUSTO_MELHORIA));
        assertEquals("5 espaços e a pilha", 6, j.get(RETANGULOS_MELHORIAS).size());
        assertArrayEquals("o retângulo do espaço 0",
                new int[] {304, 2800, 228, 120}, j.get(RETANGULOS_MELHORIAS).get(0));
        assertEquals("uma peça de bônus em cada conselheiro",
                j.get(PEDIDOS_CONSELHEIROS).keySet(), j.get(PONTOS_BONUS).keySet());
        assertEquals("7 peças de bônus", 7, j.get(DESCRICOES_BONUS).size());
        assertEquals("10 cartas secretas", 10, j.get(NOMES_CARTAS_SECRETAS).size());
        assertEquals("cada carta tem objetivo",
                j.get(NOMES_CARTAS_SECRETAS).keySet(), j.get(OBJETIVOS_CARTAS_SECRETAS).keySet());
        assertEquals("cada carta tem faixas",
                j.get(NOMES_CARTAS_SECRETAS).keySet(), j.get(FAIXAS_CARTAS_SECRETAS).keySet());
        assertEquals("3 faixas de votos", 3, j.get(FAIXAS_CARTAS_SECRETAS).get("viajante").length);
        assertEquals("as 3 regiões", List.of("China", "Pérsia", "Rússia"), j.get(NOMES_REGIOES));
        j.get(FAIXAS_CARTAS_SECRETAS).get("viajante")[0] = -1; // o array é uma cópia
        assertNotEquals("o array devolvido por get é uma cópia", -1, j.get(FAIXAS_CARTAS_SECRETAS).get("viajante")[0]);
    }

    @Test(timeout = 1000)
    public void valoresDeGetSaoSoParaLeituraECopias() {
        Jogo j = jogo(2);
        assertEquals("os nomes", List.of("Tolui", "Altani"), j.get(NOMES));
        assertEquals("as posições", List.of("karakorum", "karakorum"), j.get(POSICOES));
        assertThrows("a lista de nomes não pode ser alterada",
                UnsupportedOperationException.class, () -> j.get(NOMES).add("Jochi"));
        assertThrows("o mapa de pontos não pode ser alterado",
                UnsupportedOperationException.class, () -> j.get(PONTOS_PARADAS).remove("r1"));
        assertThrows("o mapa de tesouros não pode ser alterado",
                UnsupportedOperationException.class, () -> j.get(TESOUROS).get(0).put("seda", 9));
        j.get(PONTOS_PARADAS).get("r1")[0] = -1; // o array é uma cópia
        assertNotEquals("o array devolvido por get é uma cópia", -1, j.get(PONTOS_PARADAS).get("r1")[0]);
        Map<String, Integer> tributos = j.get(ESTOQUE);
        assertEquals("os 3 tipos de tributo", List.of("yurt", "espada", "moeda"), List.copyOf(tributos.keySet()));
    }

}
