package model;

import static model.ObservadoIF.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

// Cartas de conselheiro secreto (regras, p. 6 e 19): 2 para cada jogador, a escolha de 1 antes da primeira rodada
// e a medida de cada objetivo, que vira de 0 a 3 votos.
public class SecretasTest {

    private static Jogo jogo(int n) {
        Jogo j = Jogo.getInstance();
        j.novaPartida(List.of("Tolui", "Altani", "Jochi", "Chagatai", "Ogedei").subList(0, n));
        return j;
    }

    // ---------------------------------------------------------------- distribuição e escolha (regras, p. 6)

    @Test(timeout = 1000)
    public void cadaJogadorRecebe2CartasDiferentes() {
        Jogo j = jogo(5);
        Set<String> todas = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            List<String> opcoes = j.get(OPCOES_SECRETAS).get(i);
            assertEquals("jogador " + i + ": 2 cartas", 2, opcoes.size());
            todas.addAll(opcoes);
            assertNull("jogador " + i + ": ainda não escolheu", j.get(CARTAS_SECRETAS).get(i));
        }
        assertEquals("com 5 jogadores, as 10 cartas saem",
                new HashSet<>(List.copyOf(Jogo.getInstance().get(NOMES_CARTAS_SECRETAS).keySet())), todas);
        assertEquals("10 cartas secretas",
                10, List.copyOf(Jogo.getInstance().get(NOMES_CARTAS_SECRETAS).keySet()).size());
    }

    @Test(timeout = 1000)
    public void escolhaNaOrdemDosJogadores() {
        Jogo j = jogo(2);
        j.prepararSecretas(List.of("viajante", "soldado", "prefeito", "mensageiro"));
        assertEquals("Tolui escolhe primeiro", 0, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
        IllegalStateException e = assertThrows("Altani não escolhe antes de Tolui",
                IllegalStateException.class, () -> j.escolherCartaSecreta(1, "prefeito"));
        assertTrue(e.getMessage(), e.getMessage().contains("Tolui"));
        IllegalArgumentException e2 = assertThrows("Tolui não escolhe a carta de Altani",
                IllegalArgumentException.class,
                () -> j.escolherCartaSecreta(0, "prefeito")); // é de Altani
        assertTrue(e2.getMessage(), e2.getMessage().contains("CartasSecretas.escolher"));
        assertThrows("carta que não existe é recusada",
                IllegalArgumentException.class, () -> j.escolherCartaSecreta(0, "rei"));
        j.escolherCartaSecreta(0, "soldado");
        assertEquals("Tolui ficou com o soldado", "soldado", j.get(CARTAS_SECRETAS).get(0));
        assertEquals("agora Altani escolhe", 1, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
        j.escolherCartaSecreta(1, "prefeito");
        assertEquals("todos escolheram", -1, (int) j.get(JOGADOR_ESCOLHENDO_SECRETA));
        assertThrows("não se escolhe de novo",
                IllegalStateException.class, () -> j.escolherCartaSecreta(0, "viajante"));
    }

    @Test(timeout = 1000)
    public void ninguemJogaAntesDeTodosEscolherem() {
        Jogo j = jogo(2);
        IllegalStateException e = assertThrows("ninguém ativa coluna antes das cartas secretas",
                IllegalStateException.class, () -> j.ativarColuna(0, 1));
        assertTrue(e.getMessage(), e.getMessage().contains("carta secreta"));
        assertThrows("nem marca coluna", IllegalStateException.class, () -> j.selecionarColuna(0, 1));
        j.escolherCartaSecreta(0, j.get(OPCOES_SECRETAS).get(0).get(1));
        assertThrows("falta Altani escolher", IllegalStateException.class, () -> j.ativarColuna(0, 1));
        j.escolherCartaSecreta(1, j.get(OPCOES_SECRETAS).get(1).get(0));
        j.ativarColuna(0, 1);
        assertEquals("depois das escolhas, Tolui ativa a coluna", 1, (int) j.get(COLUNA_ATIVADA));
    }

    @Test(timeout = 1000)
    public void escolhaAvisaOsObservadores() {
        Jogo j = jogo(2);
        assertEquals("cada escolha avisa SECRETA",
                List.of(Evento.SECRETA, Evento.SECRETA), Partidas.eventos(j, () -> Partidas.escolherSecretas(j)));
    }

    // ---------------------------------------------------------------- votos (regras, p. 19)

    @Test(timeout = 1000)
    public void faixasDeVotos() {
        assertEquals("Viajante com 2 yurts: 0 votos", 0, CartaSecreta.VIAJANTE.votos(2));
        assertEquals("Viajante com 3: 1 voto", 1, CartaSecreta.VIAJANTE.votos(3));
        assertEquals("Viajante com 7: 2 votos", 2, CartaSecreta.VIAJANTE.votos(7));
        assertEquals("Viajante com 8: 3 votos", 3, CartaSecreta.VIAJANTE.votos(8));
        assertEquals("Sacerdote com 9: 3 votos", 3, CartaSecreta.SACERDOTE.votos(9));
        assertArrayEquals("as faixas do Cidadão",
                new int[] {3, 6, 8}, Jogo.getInstance().get(FAIXAS_CARTAS_SECRETAS).get("cidadao"));
        assertEquals("o nome do Cidadão", "Cidadão", Jogo.getInstance().get(NOMES_CARTAS_SECRETAS).get("cidadao"));
    }

    @Test(timeout = 1000)
    public void cartaSecretaPeloJogo() {
        Jogo j = jogo(2);
        j.prepararSecretas(List.of("soldado", "viajante", "prefeito", "mensageiro"));
        Partidas.escolherSecretas(j); // Tolui fica com o soldado
        assertEquals("Soldado sem melhorias", 0, (int) j.get(MEDIDAS_SECRETAS).get(0));
        assertEquals("Soldado sem votos", 0, (int) j.get(VOTOS_SECRETAS).get(0));
        for (String id : List.of("punho-vermelha", "punho-verde", "moeda-amarela", "cavalos-verde"))
            j.jogador(0).adicionarMelhoria(Melhoria.porId(id).getCor().coluna(), Melhoria.porId(id));
        assertEquals("Soldado com 4 melhorias", 4, (int) j.get(MEDIDAS_SECRETAS).get(0));
        assertEquals("4 melhorias: 1 voto", 1, (int) j.get(VOTOS_SECRETAS).get(0));
    }

    // ---------------------------------------------------------------- medidas dos objetivos

    private final Mapa mapa = new Mapa();
    private final Yurts yurts = new Yurts(mapa);
    private final CartasSecretas secretas = new CartasSecretas(mapa, yurts);
    private final Jogador tolui = new Jogador(Herdeiro.TOLUI, 1);

    private void yurt(String parada) {
        mapa.getParada(parada).getYurts().add(tolui);
    }

    private void melhorias(String... ids) {
        for (String id : ids) tolui.adicionarMelhoria(Melhoria.porId(id).getCor().coluna(), Melhoria.porId(id));
    }

    @Test(timeout = 1000)
    public void soldadoColecionadorSacerdoteECavaleiro() {
        melhorias("punho-vermelha", "punho-verde", "cavalos-verde", "seda-amarela", "boi-vermelha", "moeda-amarela");
        tolui.adicionarMelhoria(3, Melhoria.porId("cavalos-amarela")); // a coluna branca conta
        assertEquals("Soldado: 7 melhorias, com a da coluna branca", 7, secretas.medida(tolui, CartaSecreta.SOLDADO));
        assertEquals("Colecionador: punho, cavalos, seda, boi e moeda",
                5, secretas.medida(tolui, CartaSecreta.COLECIONADOR));
        assertEquals("Sacerdote: 2 pares (2 punhos e 2 cavalos)", 2, secretas.medida(tolui, CartaSecreta.SACERDOTE));
        assertEquals("Cavaleiro: 4 melhorias de punho ou cavalos", 4, secretas.medida(tolui, CartaSecreta.CAVALEIRO));
    }

    // Viajante: o maior grupo de yurts ligados por rotas.
    @Test(timeout = 1000)
    public void viajanteContaOMaiorGrupoLigado() {
        Parada a = mapa.getParada("r9");
        Parada b = null, c = null;
        for (Parada v : a.getVizinhas())
            if (!v.getId().equals(Mapa.KARAKORUM) && b == null) b = v;
        for (Parada v : b.getVizinhas())
            if (v != a && !v.getId().equals(Mapa.KARAKORUM) && c == null) c = v;
        yurt(a.getId());
        yurt(b.getId());
        assertEquals("Viajante: 2 yurts ligados", 2, secretas.medida(tolui, CartaSecreta.VIAJANTE));
        yurt(c.getId());
        yurt("c10"); // longe, num grupo só dele
        assertEquals("Viajante: o maior grupo tem 3 yurts; c10 fica num grupo só dele",
                3, secretas.medida(tolui, CartaSecreta.VIAJANTE));
    }

    @Test(timeout = 1000)
    public void mensageiroContaParadasDuplasComYurt() {
        List<Parada> duplas = new ArrayList<>();
        for (Parada p : mapa.getParadas())
            if (p.isDupla()) duplas.add(p);
        assertEquals("6 paradas duplas", 6, duplas.size());
        duplas.get(0).getYurts().add(tolui);
        duplas.get(0).getYurts().add(tolui); // 2 yurts na mesma dupla contam 1 parada
        duplas.get(1).getYurts().add(tolui);
        assertEquals("Mensageiro: 2 paradas duplas, mesmo com 2 yurts numa delas",
                2, secretas.medida(tolui, CartaSecreta.MENSAGEIRO));
    }

    // Sarai fica no anel r8, r9 e r10.
    @Test(timeout = 1000)
    public void prefeitoECidadaoContamCidadesComYurtsAoRedor() {
        for (Cidade c : mapa.getCidades())
            if (!c.nome().equals("Sarai")) assertFalse(c.nome() + " não fica ao lado de r8 nem de r9",
                    c.isAdjacente(mapa.getParada("r9")) || c.isAdjacente(mapa.getParada("r8")));
        yurt("r9");
        assertEquals("Cidadão: Sarai tem 1 yurt ao redor", 1, secretas.medida(tolui, CartaSecreta.CIDADAO));
        assertEquals("Prefeito pede 2 yurts ao redor", 0, secretas.medida(tolui, CartaSecreta.PREFEITO));
        yurt("r8");
        assertEquals("Prefeito: Sarai tem 2 yurts ao redor", 1, secretas.medida(tolui, CartaSecreta.PREFEITO));
        mapa.getCidade("Kiev").conquistar(tolui); // o yurt dentro da cidade não conta
        assertEquals("Cidadão: o yurt dentro de Kiev não conta", 1, secretas.medida(tolui, CartaSecreta.CIDADAO));
    }

    // A província do Khan é a que tem o ícone mais perto da cabeça do Khan.
    @Test(timeout = 1000)
    public void bajuladorContaProvinciasDoKhanComYurtsAoRedor() {
        List<String> proprias = new ArrayList<>();
        for (LocalKhan l : mapa.getLocaisKhan()) proprias.add(l.getPropria().getId());
        assertEquals("as províncias do Khan", List.of("t1", "t5", "t8", "t12"), proprias);
        yurt("r1"); // t1 fica entre r1, r3, p1 e r5
        assertEquals("Bajulador: t1 com 1 yurt ao redor não basta", 0, secretas.medida(tolui, CartaSecreta.BAJULADOR));
        yurt("r3");
        assertEquals("Bajulador: t1 com 2 yurts ao redor", 1, secretas.medida(tolui, CartaSecreta.BAJULADOR));
    }

    @Test(timeout = 1000)
    public void governadorPedeYurtsEMelhoriasDaMesmaRegiao() {
        assertEquals("r8 fica na Rússia", Regiao.RUSSIA, mapa.getParada("r8").getRegiao());
        assertEquals("r9 fica na Rússia", Regiao.RUSSIA, mapa.getParada("r9").getRegiao());
        yurt("r8");
        yurt("r9");
        melhorias("punho-amarela");
        assertEquals("Governador: 1 melhoria amarela não basta", 0, secretas.medida(tolui, CartaSecreta.GOVERNADOR));
        melhorias("moeda-amarela"); // 2 melhorias na coluna amarela (Rússia)
        assertEquals("Governador: 2 yurts e 2 melhorias na Rússia", 1, secretas.medida(tolui, CartaSecreta.GOVERNADOR));
        melhorias("punho-verde", "moeda-verde"); // a Pérsia não tem yurts dele
        assertEquals("Governador: a Pérsia sem yurts não conta", 1, secretas.medida(tolui, CartaSecreta.GOVERNADOR));
    }
}
