package view;

import static model.ObservadoIF.*;

import java.util.ArrayList;
import java.util.List;

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
                escrever("Vez de " + nomes.get(observado.get(JOGADOR_ATUAL)) + ".");
            }
            case SELECAO -> {
                int coluna = observado.get(COLUNA_SELECIONADA);
                escrever(coluna == -1 ? "Coluna desmarcada."
                        : "Coluna " + (coluna + 1) + " marcada. Clique em Confirmar coluna.");
            }
            case COLUNA -> {
                int coluna = observado.get(COLUNA_ATIVADA);
                escrever(quem + " ativou a coluna " + (coluna + 1) + ": " + observado.get(MOVIMENTOS)
                        + " movimento(s), " + observado.get(PUNHOS) + " punho(s)"
                        + (observado.get(KHAN_PENDENTE) ? " e o Khan" : "") + ".");
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
            case KHAN -> escrever(quem + " moveu o Khan. As províncias "
                    + String.join(", ", observado.get(PROVINCIAS_KHAN).get(observado.get(POSICAO_KHAN)))
                    + " receberam tributo.");
            case CIDADE -> escrever(quem + " tomou 1 " + observado.get(ULTIMO_TESOURO) + " de "
                    + observado.get(ULTIMA_CIDADE) + ". Espadas restantes: " + observado.get(ESPADAS).get(autor) + ".");
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
            case CARTA -> escrever(quem + " cumpriu a carta \"" + observado.get(CARTAS_ABERTAS).get(observado.get(ULTIMA_CARTA))
                    + "\" e ganhou 1 voto.");
            case FIM_DISPARADO -> escrever("O fim da partida foi disparado: termina a rodada atual e há mais uma rodada"
                    + " completa.");
            case TURNO -> {
                escrever(quem + " encerrou o turno.");
                escrever("Vez de " + nomes.get(observado.get(JOGADOR_ATUAL)) + ".");
            }
            case FIM -> {
                List<Integer> votos = observado.get(VOTOS);
                StringBuilder s = new StringBuilder("A partida terminou. Votos:");
                for (int i = 0; i < nomes.size(); i++) s.append(' ').append(nomes.get(i)).append(' ').append(votos.get(i)).append(';');
                List<String> vencedores = new ArrayList<>();
                for (int v : observado.get(VENCEDORES)) vencedores.add(nomes.get(v));
                escrever(s.append(" vencedor(es): ").append(String.join(", ", vencedores)).append('.').toString());
            }
        }
    }

    private static void escrever(String linha) {
        System.out.println(linha);
    }
}
