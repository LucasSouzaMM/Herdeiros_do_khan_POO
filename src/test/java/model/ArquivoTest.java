package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import model.ObservadoIF.Chave;

// Salvar e recuperar a partida inteira (4ª iteração), e os arquivos antigos da 2ª e da 3ª iteração.
public class ArquivoTest {

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    // Uma partida no meio do turno de Tolui, com um pouco de tudo: melhorias, virtuais, tributos, yurts, ataque,
    // entrega com peça de bônus, baú aberto, cartas, cartas secretas e os 2 piões da Altani.
    private static Jogo partidaCheia() {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Altani", "Tolui", "Jochi", "Ogedei"));
        j.prepararCartas(List.of("tesouros4", "yurts6", "bonus2", "linha"));
        j.prepararCidades(List.of("Sarai", "Kiev", "Pequim", "Bagdá", "Cabul", "Moscou", "Samarcanda", "Kaesong", "Cantão"),
                List.of(Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO,
                        Tesouro.PELE, Tesouro.ESPECIARIA, Tesouro.ESPECIARIA, Tesouro.BOI, Tesouro.SEDA, Tesouro.FERRO,
                        Tesouro.PELE, Tesouro.BOI, Tesouro.SEDA));
        Partidas.escolherSecretas(j);
        // Altani joga com o pião 2 e vai até p4.
        j.selecionarPeao(0, 1);
        j.ativarColuna(0, 1);
        j.mover(0, "p4");
        j.encerrarTurno(0);
        // Tolui: melhoria, moeda virtual, tributo, yurt vizinho, ataque a Sarai, entrega em Karakorum e baú.
        Jogador tolui = j.jogador(1);
        tolui.ganhar(Tributo.MOEDA);
        tolui.ganhar(Tributo.YURT);
        tolui.ganhar(Tributo.ESPADA);
        tolui.adicionarMelhoria(0, Melhoria.porId("seda-vermelha"));
        for (Tesouro t : List.of(Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI)) tolui.ganharTesouro(t);
        tolui.ganharBonus(Bonus.BAU);
        j.ativarColuna(1, 0); // vermelha: moeda virtual e a seda virtual
        j.comprarMelhoria(1, 0);
        j.pegarTributo(1, "t5");
        j.construirYurt(1, "r11"); // vizinha de Karakorum
        j.entregarTesouros(1, "karakorum");
        j.mover(1, "r9");
        j.atacarCidade(1, "Sarai", "boi");
        j.usarBonus(1, "bau");
        return j;
    }

    // Os detalhes do último evento (grupo 7) descrevem a última ação, e não o estado da partida: não são gravados.
    private static final Set<Chave<?>> DETALHES = Set.of(EVENTO, AUTOR_DO_EVENTO, ULTIMA_PROVINCIA, ULTIMA_CIDADE,
            ULTIMO_TESOURO, ULTIMA_PARADA_YURT, ULTIMO_CONSELHEIRO, ULTIMA_CARTA, ULTIMA_MELHORIA, ESPACO_ULTIMA_MELHORIA,
            COLUNA_ULTIMA_MELHORIA, ULTIMO_BONUS, TESOURO_DO_BAU, GANHOS_DO_KHAN);

    // Tudo o que a interface consegue ler da partida (o valor de cada chave de get), num texto só, para comparar
    // antes e depois de carregar.
    private static String retrato(Jogo j) throws IllegalAccessException {
        StringBuilder s = new StringBuilder();
        for (Field f : ObservadoIF.class.getFields()) {
            if (f.getType() != Chave.class || DETALHES.contains(f.get(null))) continue;
            s.append(f.getName()).append('=').append(texto(j.get((Chave<?>) f.get(null)))).append('\n');
        }
        return s.toString();
    }

    // Como toString, mas mostra o conteúdo dos int[] (que aparecem dentro das listas e dos mapas).
    private static String texto(Object valor) {
        if (valor instanceof int[] a) return Arrays.toString(a);
        if (valor instanceof List<?> lista) {
            List<String> itens = new ArrayList<>();
            for (Object item : lista) itens.add(texto(item));
            return itens.toString();
        }
        if (valor instanceof Map<?, ?> mapa) {
            List<String> itens = new ArrayList<>();
            for (Map.Entry<?, ?> e : mapa.entrySet()) itens.add(e.getKey() + ":" + texto(e.getValue()));
            return itens.toString();
        }
        return String.valueOf(valor);
    }

    @Test(timeout = 1000)
    public void salvarECarregarDevolveAPartidaInteira() throws Exception {
        Jogo j = partidaCheia();
        assertEquals("a partida tem um baú aberto", 3, j.get(TESOUROS_BAU).size());
        String antes = retrato(j);
        Path arquivo = pasta.newFile("cheia.txt").toPath();
        j.salvar(arquivo);
        // A3: o arquivo é ASCII puro; Bagdá e Cantão vão com o acento escrito como \u00e1 e \u00e3.
        for (byte b : Files.readAllBytes(arquivo)) assertTrue("byte fora do ASCII: " + b, b >= 0);
        assertTrue("Bagdá vai com o á escrito como \\u00e1",
                Files.readString(arquivo).contains("cidade.Bagd\\u00e1.revelada="));
        j.novaPartida(List.of("Chagatai", "Jochi"));
        assertSame("carregar devolve o mesmo Jogo (Singleton)", j, Jogo.carregar(arquivo));
        assertEquals("a partida carregada é igual à salva", antes, retrato(j));
        // Gravar de novo dá o mesmo texto: inclui a ordem das pilhas e do saco, que a interface não mostra.
        Path outro = pasta.newFile("outra.txt").toPath();
        j.salvar(outro);
        assertEquals("gravar de novo dá o mesmo texto, com a ordem das pilhas e do saco",
                Files.readString(arquivo), Files.readString(outro));
    }

    @Test(timeout = 1000)
    public void partidaCarregadaContinua() throws Exception {
        Jogo j = partidaCheia();
        Path arquivo = pasta.newFile("continua.txt").toPath();
        j.salvar(arquivo);
        Jogo.carregar(arquivo);
        assertThrows("o baú ainda está aberto depois de carregar",
                IllegalStateException.class, () -> j.encerrarTurno(1));
        j.escolherTesouroBau(1, j.get(TESOUROS_BAU).get(0));
        j.encerrarTurno(1);
        assertEquals("depois do Tolui, joga o Jochi", 2, (int) j.get(JOGADOR_ATUAL));
        j.ativarColuna(2, 0);
        assertEquals("as cartas secretas não são escolhidas de novo", -1, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
    }

    @Test(timeout = 1000)
    public void carregarAvisaOsObservadores() throws Exception {
        Jogo j = partidaCheia();
        Path arquivo = pasta.newFile("aviso.txt").toPath();
        j.salvar(arquivo);
        List<Evento> eventos = new ArrayList<>();
        ObservadorIF obs = observado -> eventos.add(observado.get(EVENTO));
        j.add(obs);
        try {
            Jogo.carregar(arquivo);
        } finally {
            j.remove(obs);
        }
        assertEquals("carregar avisa PARTIDA uma vez só", List.of(Evento.PARTIDA), eventos);
    }

    @Test(timeout = 1000)
    public void fimDaPartidaTambemESalvo() throws Exception {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Ogedei", "Altani"));
        j.prepararBonus(List.of());
        Partidas.escolherSecretas(j);
        j.jogador(0).ganharVotos(8);
        for (Tesouro t : List.of(Tesouro.FERRO, Tesouro.PELE, Tesouro.BOI)) j.jogador(0).ganharTesouro(t);
        j.ativarColuna(0, 0);
        j.entregarTesouros(0, "karakorum");
        assertTrue("os 10 votos de Ogedei disparam o fim", j.get(FIM_DISPARADO));
        Path arquivo = pasta.newFile("fim.txt").toPath();
        j.salvar(arquivo);
        j.novaPartida(List.of("Tolui", "Jochi"));
        Jogo.carregar(arquivo);
        assertTrue("o fim disparado volta do arquivo", j.get(FIM_DISPARADO));
        assertEquals("faltam 3 turnos", 3, (int) j.get(TURNOS_RESTANTES));
        assertEquals("8 votos + 2 de Karakorum", 10, (int) j.get(VOTOS).get(0));
    }

    // ---------------------------------------------------------------- arquivos antigos

    // O formato da 3ª iteração: só jogadores, moedas, posições, colunas bloqueadas e o turno.
    @Test(timeout = 1000)
    public void abreOArquivoDaTerceiraIteracao() throws Exception {
        Path arquivo = pasta.newFile("iteracao3.txt").toPath();
        Files.writeString(arquivo, String.join("\n", "jogadores=3", "jogadorAtual=1", "coluna=-1", "movimentos=0",
                "jogador0.herdeiro=Chagatai", "jogador0.moedas=1", "jogador0.posicao=r5", "jogador0.bloqueadas=0",
                "jogador1.herdeiro=Tolui", "jogador1.moedas=1", "jogador1.posicao=c4", "jogador1.bloqueadas=4",
                "jogador2.herdeiro=Ogedei", "jogador2.moedas=2", "jogador2.posicao=p4", "jogador2.bloqueadas=2") + "\n");
        Jogo j = Jogo.carregar(arquivo);
        assertEquals("os 3 herdeiros na ordem do arquivo",
                List.of("Chagatai", "Tolui", "Ogedei"), List.of(j.get(NOMES).get(0), j.get(NOMES).get(1), j.get(NOMES).get(2)));
        assertEquals("o jogador atual", 1, (int) j.get(JOGADOR_ATUAL));
        assertEquals("a posição de Tolui", "c4", j.get(POSICOES).get(1));
        assertTrue("a coluna 3 de Tolui está bloqueada", j.get(COLUNAS_BLOQUEADAS).get(1).contains(2));
        assertEquals("as moedas de Ogedei", 2, (int) j.get(MOEDAS).get(2));
        // O que o arquivo antigo não tem começa como numa partida nova.
        assertEquals("o arquivo antigo não tem as cartas secretas: o 1º jogador escolhe",
                0, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
        assertEquals("o yurt do Tolui em Karakorum", List.of(1), j.get(YURTS_NAS_PARADAS).get("karakorum"));
        assertEquals("o arquivo antigo não tem as melhorias: a pilha começa cheia",
                39, (int) j.get(MELHORIAS_NA_PILHA));
    }

    // A 2ª iteração não tinha as colunas bloqueadas.
    @Test(timeout = 1000)
    public void abreOArquivoDaSegundaIteracao() throws Exception {
        Path arquivo = pasta.newFile("iteracao2.txt").toPath();
        Files.writeString(arquivo, "jogadores=2\njogadorAtual=0\ncoluna=1\nmovimentos=1\njogador0.herdeiro=Altani\n"
                + "jogador0.moedas=1\njogador0.posicao=r9\njogador1.herdeiro=Jochi\njogador1.moedas=1\njogador1.posicao=karakorum\n");
        Jogo j = Jogo.carregar(arquivo);
        assertEquals("o 2º pião da Altani fica em Karakorum", List.of("r9", "karakorum"), j.get(PEOES).get(0));
        assertEquals("a coluna ativada", 1, (int) j.get(COLUNA_ATIVADA));
        assertEquals("os movimentos que sobravam", 1, (int) j.get(MOVIMENTOS));
        assertFalse("a 2ª iteração não bloqueava colunas", j.get(COLUNAS_BLOQUEADAS).get(0).contains(0));
    }

    // ---------------------------------------------------------------- arquivos inválidos

    private static void trocarEConferir(Path arquivo, String de, String para, String naMensagem) throws Exception {
        String texto = Files.readString(arquivo);
        assertTrue(de, texto.contains(de));
        Files.writeString(arquivo, texto.replace(de, para));
        IllegalArgumentException e = assertThrows(de + " -> " + para + " é recusado",
                IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage(), e.getMessage().contains(naMensagem));
        Files.writeString(arquivo, texto);
    }

    @Test(timeout = 1000)
    public void arquivoComValorInvalidoNaoTrocaAPartida() throws Exception {
        Jogo j = partidaCheia();
        Path arquivo = pasta.newFile("ruim.txt").toPath();
        j.salvar(arquivo);
        String antes = retrato(j);
        trocarEConferir(arquivo, "jogador1.melhorias0=seda-vermelha", "jogador1.melhorias0=ouro-vermelha", "ouro");
        trocarEConferir(arquivo, "fim.terminada=false", "fim.terminada=talvez", "fim.terminada");
        trocarEConferir(arquivo, "khan=-1", "khan=9", "Khan");
        trocarEConferir(arquivo, "jogador0.peaoAtivo=1", "jogador0.peaoAtivo=2", "pião ativo");
        trocarEConferir(arquivo, "turno.bau=", "turno.bau=ouro,", "ouro");
        assertEquals("a partida anterior continua", antes, retrato(j));
    }

    @Test(timeout = 1000)
    public void arquivoSemUmaChaveNovaEInvalido() throws Exception {
        Jogo j = partidaCheia();
        Path arquivo = pasta.newFile("falta.txt").toPath();
        j.salvar(arquivo);
        String texto = Files.readString(arquivo);
        Files.writeString(arquivo, texto.replaceAll("(?m)^cartas=.*\\n", ""));
        IllegalArgumentException e = assertThrows("arquivo sem a chave cartas é recusado",
                IllegalArgumentException.class, () -> Jogo.carregar(arquivo));
        assertTrue(e.getMessage(), e.getMessage().contains("'cartas'"));
    }
}
