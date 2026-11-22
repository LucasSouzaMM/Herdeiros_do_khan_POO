package model;

import java.util.List;

// Habilidades dos herdeiros (texto das cartelas e regras, p. 20). Cada herdeiro quebra uma regra do jogo:
// - Altani tem 2 piões; em cada turno, só o pião escolhido antes de ativar a coluna anda e age.
// - Jochi começa com 2 melhorias sorteadas, uma delas na coluna branca, e por isso não ganha a melhoria grátis da
//   1ª linha completa (o espaço da coluna branca já está ocupado).
// - Chagatai, uma vez por turno, troca 1 movimento por 1 espada do estoque.
// - Tolui começa com 1 yurt em Karakorum (sem região) e pode construir yurts nas paradas vizinhas ao seu pião
//   (essa parte fica em Yurts).
// - Ogedei, uma vez por turno, paga 1 moeda para tomar o 1º tesouro de uma cidade com 2 ou mais tesouros, de
//   qualquer lugar do mapa.
class Habilidades {
    private final Mapa mapa;
    private final Tributos tributos;
    private final Melhorias melhorias;

    Habilidades(Mapa mapa, Tributos tributos, Melhorias melhorias) {
        this.mapa = mapa;
        this.tributos = tributos;
        this.melhorias = melhorias;
    }

    /** Começo da partida: o yurt do Tolui em Karakorum e as 2 melhorias do Jochi. */
    void preparar(List<Jogador> jogadores) {
        for (Jogador j : jogadores) {
            if (j.getHerdeiro() == Herdeiro.TOLUI) mapa.getParada(Mapa.KARAKORUM).getYurts().add(j);
            if (j.getHerdeiro() == Herdeiro.JOCHI) {
                Melhoria colorida = melhorias.tirarDaPilha(), branca = melhorias.tirarDaPilha();
                if (colorida != null) j.adicionarMelhoria(colorida.getCor().coluna(), colorida);
                if (branca != null) j.adicionarMelhoria(Cor.BRANCA.coluna(), branca);
                j.premiarLinha(); // a 1ª linha não dá melhoria grátis: o espaço da coluna branca já está ocupado
            }
        }
    }

    /** Altani escolhe, antes de ativar a coluna, qual dos seus 2 piões anda e age neste turno. */
    static void selecionarPeao(Jogador j, Turno turno, int peao) {
        String erro = erroDaTrocaDePeao(j, turno);
        if (erro != null) throw new IllegalStateException("Erro em Habilidades.selecionarPeao: " + erro);
        j.setPeaoAtivo(peao);
    }

    /** Por que o jogador não pode trocar de pião agora (turno é null antes de ativar a coluna), ou null se pode. */
    static String erroDaTrocaDePeao(Jogador j, Turno turno) {
        if (j.getPosicoes().size() < 2) return "só a Altani tem 2 piões.";
        if (turno != null) return "o pião é escolhido antes de ativar a coluna.";
        return null;
    }

    /** Chagatai: uma vez por turno, deixa de usar 1 movimento e ganha 1 espada do estoque, que pode guardar. */
    void trocarCavaloPorEspada(Jogador j, Turno turno, List<Jogador> jogadores) {
        String erro = erroDaTrocaDeCavalo(j, turno, jogadores);
        if (erro != null) throw new IllegalStateException("Erro em Habilidades.trocarCavaloPorEspada: " + erro);
        turno.gastar(1);
        j.ganhar(Tributo.ESPADA);
        turno.usarHabilidade();
    }

    /** Por que o jogador não pode trocar 1 movimento por 1 espada agora, ou null se pode. */
    String erroDaTrocaDeCavalo(Jogador j, Turno turno, List<Jogador> jogadores) {
        if (j.getHerdeiro() != Herdeiro.CHAGATAI) return "só o Chagatai troca cavalo por espada.";
        if (turno.isHabilidadeUsada()) return "o Chagatai já trocou um cavalo por espada neste turno.";
        if (turno.getMovimentos() == 0) return "não sobrou movimento para trocar.";
        if (!Movimento.temSaida(j.getPosicao(), turno.getMovimentos() - 1, j, jogadores))
            return "o Chagatai está numa parada ocupada e precisa dos movimentos que sobram para sair dela.";
        if (tributos.estoque(Tributo.ESPADA, jogadores) == 0) return "não há espadas no estoque.";
        return null;
    }

    /**
     * Ogedei: uma vez por turno, paga 1 moeda (real ou virtual) para tomar o tesouro de uma cidade revelada que tenha
     * 2 ou mais tesouros, mesmo longe dela. Essa tomada não conta como ataque: depois, se ele estiver ao lado da
     * cidade, o próximo tesouro custa 1 espada.
     */
    void tomarTesouroComMoeda(Jogador j, Turno turno, Cidade c, Tesouro t) {
        String erro = erroDoTesouroComMoeda(j, turno, c);
        if (erro != null) throw new IllegalStateException("Erro em Habilidades.tomarTesouroComMoeda: " + erro);
        if (!c.getTesouros().contains(t))
            throw new IllegalArgumentException("Erro em Habilidades.tomarTesouroComMoeda: " + c.nome() + " não tem o tesouro "
                    + t.getNome() + ".");
        turno.pagar(Tributo.MOEDA, 1);
        c.retirar(t);
        j.ganharTesouro(t);
        turno.usarHabilidade();
    }

    /** Por que o jogador não pode pagar 1 moeda por um tesouro da cidade agora, ou null se pode. */
    static String erroDoTesouroComMoeda(Jogador j, Turno turno, Cidade c) {
        if (j.getHerdeiro() != Herdeiro.OGEDEI) return "só o Ogedei toma tesouro com moeda.";
        if (turno.isHabilidadeUsada()) return "o Ogedei já tomou um tesouro com moeda neste turno.";
        if (!c.isRevelada() || c.getDono() != null) return c.nome() + " não está revelada ou já foi conquistada.";
        if (c.getTesouros().size() < 2) return "com moeda, só dá para tomar tesouro de uma cidade com 2 ou mais tesouros.";
        if (turno.disponivel(Tributo.MOEDA) < 1) return "o Ogedei não tem moeda.";
        return null;
    }
}
