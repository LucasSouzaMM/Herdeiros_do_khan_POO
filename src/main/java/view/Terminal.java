package view;

import static model.ObservadoIF.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import controller.Controlador;
import model.ObservadoIF;
import model.ObservadorIF;

// Observador da partida que escreve no terminal cada mudança (RF02). Ele não guarda estado: a cada aviso, lê
// com get o tipo do evento, os detalhes e os valores que a mensagem mostra. Os erros e as mensagens de salvar
// e carregar são escritos pelo controlador, porque não mudam a partida.
// Ele não faz a leitura inicial depois de se inscrever (decisão A): só escreve o que muda a partir daí.
public class Terminal implements ObservadorIF {

    public Terminal() {
        Controlador.getInstance().registra(this);
    }

    @Override
    public void notify(ObservadoIF observado) {
        Evento evento = observado.get(EVENTO);
        List<String> nomes = observado.get(NOMES);
        int autor = observado.get(AUTOR_DO_EVENTO);
        String quem = autor == -1 ? null : nomes.get(autor);
        switch (evento) {
            case PARTIDA -> {
                List<String> ordem = new ArrayList<>();
                for (int i = 0; i < nomes.size(); i++) ordem.add((i + 1) + ". " + nomes.get(i));
                escrever("Partida iniciada. Ordem dos jogadores: " + String.join(" ", ordem));
                int escolhendo = observado.get(JOGADOR_ESCOLHENDO_SECRETA);
                if (escolhendo != -1)
                    escrever("Antes da primeira rodada, cada jogador escolhe a sua carta secreta. Começa "
                            + nomes.get(escolhendo) + ".");
                else escrever("Vez de " + nomes.get(observado.get(JOGADOR_ATUAL)) + ".");
            }
            case SECRETA -> { // o terminal não diz qual carta, porque ela é secreta
                escrever(quem + " escolheu a sua carta secreta.");
                int proximo = observado.get(JOGADOR_ESCOLHENDO_SECRETA);
                if (proximo != -1) escrever("Agora " + nomes.get(proximo) + " escolhe a carta secreta.");
                else escrever("Vez de " + nomes.get(observado.get(JOGADOR_ATUAL)) + ".");
            }
            case PEAO -> {
                int peao = observado.get(PEOES_ATIVOS).get(autor);
                escrever(quem + " vai jogar com o pião " + (peao + 1) + ", em " + observado.get(PEOES).get(autor).get(peao)
                        + ".");
            }
            case SELECAO -> {
                int coluna = observado.get(COLUNA_SELECIONADA);
                escrever(coluna == -1 ? "Coluna desmarcada."
                        : "Coluna " + (coluna + 1) + " marcada. Clique em Confirmar coluna.");
            }
            case COLUNA -> {
                int coluna = observado.get(COLUNA_ATIVADA);
                List<String> virtuais = new ArrayList<>();
                for (Map.Entry<String, Integer> tipo : observado.get(VIRTUAIS).entrySet())
                    if (tipo.getValue() > 0) virtuais.add(tipo.getValue() + " " + tipo.getKey());
                for (String tesouro : observado.get(TESOUROS_VIRTUAIS)) virtuais.add("1 " + tesouro);
                escrever(quem + " ativou a coluna " + (coluna + 1) + ": " + observado.get(MOVIMENTOS)
                        + " movimento(s), " + observado.get(PUNHOS) + " punho(s)"
                        + (observado.get(KHAN_PENDENTE) ? " e o Khan" : "")
                        + (virtuais.isEmpty() ? "" : ". Virtuais neste turno: " + String.join(", ", virtuais)) + ".");
                if (coluna == observado.get(COLUNA_REATIVACAO))
                    escrever("As colunas bloqueadas de " + quem + " foram liberadas.");
            }
            case MOVIMENTO -> escrever(quem + " andou para " + observado.get(POSICOES).get(autor)
                    + ". Movimentos restantes: " + observado.get(MOVIMENTOS) + ".");
            case TRIBUTO -> {
                String provincia = observado.get(ULTIMA_PROVINCIA);
                escrever(quem + " pegou 1 " + observado.get(TIPOS_PROVINCIAS).get(provincia) + " da província "
                        + provincia + ". Punhos restantes: " + observado.get(PUNHOS) + ".");
            }
            case CAVALO_POR_ESPADA -> escrever(quem + " trocou 1 movimento por 1 espada. Movimentos restantes: "
                    + observado.get(MOVIMENTOS) + ". Espadas: " + observado.get(ESPADAS).get(autor) + ".");
            case KHAN -> {
                int local = observado.get(POSICAO_KHAN);
                if (local == observado.get(KHAN_AREA)) {
                    StringBuilder s = new StringBuilder(quem + " moveu o Khan para a área de melhorias, que foi preenchida.");
                    for (String peca : observado.get(GANHOS_DO_KHAN)) s.append(" Ganhou 1 ").append(peca).append('.');
                    escrever(s.toString());
                } else {
                    escrever(quem + " moveu o Khan. As províncias "
                            + String.join(", ", observado.get(PROVINCIAS_KHAN).get(local)) + " receberam tributo.");
                }
            }
            case CIDADE -> escrever(quem + " tomou 1 " + observado.get(ULTIMO_TESOURO) + " de "
                    + observado.get(ULTIMA_CIDADE) + ". Espadas restantes: " + observado.get(ESPADAS).get(autor) + ".");
            case TESOURO_COM_MOEDA -> escrever(quem + " pagou 1 moeda e tomou 1 " + observado.get(ULTIMO_TESOURO) + " de "
                    + observado.get(ULTIMA_CIDADE) + ".");
            case CONQUISTA -> escrever(quem + " conquistou " + observado.get(ULTIMA_CIDADE)
                    + "! Cidades ainda na pilha: " + observado.get(CIDADES_NA_PILHA) + ".");
            case YURT -> escrever(quem + " construiu um yurt em " + observado.get(ULTIMA_PARADA_YURT)
                    + ". Yurts restantes: " + observado.get(YURTS_RESTANTES).get(autor) + ".");
            case CONSELHEIRO -> {
                String parada = observado.get(ULTIMO_CONSELHEIRO);
                escrever(quem + " entregou " + String.join(", ", observado.get(PEDIDOS_CONSELHEIROS).get(parada))
                        + " ao conselheiro de " + parada + " e ganhou " + observado.get(VOTOS_CONSELHEIROS).get(parada)
                        + " voto(s). Votos: " + observado.get(VOTOS).get(autor) + ".");
            }
            case BONUS_GANHO -> {
                String peca = observado.get(ULTIMO_BONUS);
                escrever(quem + " foi o primeiro a entregar ali e ganhou a peça de bônus " + peca + " ("
                        + observado.get(DESCRICOES_BONUS).get(peca) + ").");
            }
            case BONUS_USADO -> {
                String peca = observado.get(ULTIMO_BONUS);
                escrever(quem + " usou a peça de bônus " + peca + " (" + observado.get(DESCRICOES_BONUS).get(peca) + ").");
                List<String> bau = observado.get(TESOUROS_BAU);
                if (!bau.isEmpty()) escrever("Baú: " + String.join(", ", bau) + ". Escolha 1 tesouro.");
            }
            case BAU -> escrever(quem + " ficou com " + observado.get(TESOURO_DO_BAU)
                    + " do baú; os outros saíram do jogo.");
            case MELHORIA, MELHORIA_GRATIS -> {
                int espaco = observado.get(ESPACO_ULTIMA_MELHORIA);
                String origem = espaco == observado.get(PILHA_MELHORIAS) ? "do topo da pilha"
                        : "do espaço " + (espaco + 1) + " da área";
                String melhoria = observado.get(ULTIMA_MELHORIA);
                int coluna = observado.get(COLUNA_ULTIMA_MELHORIA);
                escrever(evento == Evento.MELHORIA
                        ? quem + " comprou a melhoria " + melhoria + " " + origem + " para a coluna " + (coluna + 1)
                                + ". Moedas: " + observado.get(MOEDAS).get(autor) + "."
                        : quem + " escolheu a melhoria grátis " + melhoria + " " + origem + " para a coluna "
                                + (coluna + 1) + ".");
                if ((int) observado.get(MELHORIAS_GRATIS) > 0)
                    escrever(quem + " completou uma linha de melhorias e ganhou 1 melhoria grátis para a coluna"
                            + " branca. Clique numa melhoria da área ou na pilha.");
            }
            case CARTA -> escrever(quem + " cumpriu a carta \"" + observado.get(CARTAS_ABERTAS).get(observado.get(ULTIMA_CARTA))
                    + "\" e ganhou 1 voto.");
            case FIM_DISPARADO -> escrever("O fim da partida foi disparado: termina a rodada atual e há mais uma rodada"
                    + " completa.");
            case TURNO -> {
                escrever(quem + " encerrou o turno.");
                escrever("Vez de " + nomes.get(observado.get(JOGADOR_ATUAL)) + ".");
            }
            case FIM -> {
                // Regras, p. 14 a 16: a trilha, a influência em cada região, a carta secreta e o total de cada jogador.
                escrever(quem + " encerrou o turno.");
                escrever("A partida terminou. Pontuação final:");
                List<String> regioes = observado.get(NOMES_REGIOES);
                List<String> secretas = observado.get(CARTAS_SECRETAS);
                for (int i = 0; i < nomes.size(); i++) {
                    StringBuilder s = new StringBuilder("  " + nomes.get(i) + ": trilha " + observado.get(VOTOS).get(i));
                    for (int c = 0; c < regioes.size(); c++)
                        s.append(", ").append(regioes.get(c)).append(' ').append(observado.get(VOTOS_INFLUENCIA).get(i).get(c));
                    s.append(", carta secreta");
                    if (secretas.get(i) != null)
                        s.append(" (").append(observado.get(NOMES_CARTAS_SECRETAS).get(secretas.get(i))).append(')');
                    int total = observado.get(TOTAIS_FINAIS).get(i);
                    s.append(' ').append(observado.get(VOTOS_SECRETAS).get(i)).append(" = ").append(total)
                            .append(total == 1 ? " voto." : " votos.");
                    escrever(s.toString());
                }
                List<String> vencedores = new ArrayList<>();
                for (int v : observado.get(VENCEDORES)) vencedores.add(nomes.get(v));
                escrever((vencedores.size() > 1 ? "Vencedores: " : "Vencedor: ") + String.join(", ", vencedores) + ".");
            }
        }
    }

    private static void escrever(String linha) {
        System.out.println(linha);
    }
}
