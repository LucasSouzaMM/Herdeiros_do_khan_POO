package model;

import java.util.List;
import java.util.Map;

// Padrão Observer (cap. 17): o observado avisa os observadores a cada mudança de estado.
// No notify, o observador chama get(chave) para buscar os dados que precisa e depois se redesenha.
//
// Contrato de inscrição da View (decisão B da 3ª iteração):
// - A View se inscreve pelo controlador: Controlador.registra(this) devolve o observado tipado só como
//   ObservadoIF, como Ctrl.registra no exemplo do relógio do cap. 17.
// - A View lê a partida apenas por get(chave), no notify ou logo depois de se inscrever. Ela não guarda nenhuma
//   outra referência ao Model.
// - Do pacote model, a View importa só ObservadoIF e ObservadorIF. Nunca importa Jogo nem outra classe.
// Jogo é público só para o pacote controller poder usá-lo. No cap. 17, Ctrl fica no mesmo pacote do observado,
// que pode então ser package-private; aqui o controlador tem pacote próprio, e o Java não impede a View de
// importar Jogo. Por isso a regra vale por contrato. Para conferir, este comando não pode imprimir nada:
//   grep -rnE "import model\.[A-Za-z*]+;" src/main/java/view | grep -vE "ObservadoIF|ObservadorIF"
//
// No cap. 17, get recebe e devolve int (get(1) são os minutos do relógio). Aqui cada chave é uma constante
// tipada: get(HERDEIROS) devolve List<String> sem conversão, e usar a chave errada é erro de compilação.
// Os valores são tipos comuns do Java (String, Integer, Boolean, List, Map, int[]), nunca classes do Model,
// e não podem ser alterados: cada get devolve uma cópia só para leitura.
// As listas "por jogador" têm o jogador como índice. Os mapas guardam a ordem do tabuleiro (as chaves de um
// mapa são a lista de ids). As chaves estão em interacoes/iteracao3.md, grupos 1 a 7.
public interface ObservadoIF {

    /** Chave de get: o tipo T é o tipo do valor que get devolve. Só esta interface cria chaves. */
    final class Chave<T> {
        private final String nome;

        private Chave(String nome) {
            this.nome = nome;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    /** O tipo da última mudança, devolvido por get(EVENTO). */
    enum Evento {
        PARTIDA,       // uma partida foi criada ou carregada
        SELECAO,       // uma coluna da cartela foi marcada ou desmarcada
        COLUNA,        // uma coluna foi ativada
        MOVIMENTO,     // um pião andou
        TRIBUTO,       // um jogador pegou uma peça de tributo de uma província
        KHAN,          // o Khan foi movido e as províncias apontadas receberam tributos
        CIDADE,        // um jogador tomou um tesouro de uma cidade
        CONQUISTA,     // logo depois de CIDADE: era o último tesouro, e o jogador conquistou a cidade
        YURT,          // um jogador construiu um yurt numa parada
        CONSELHEIRO,   // um jogador entregou tesouros a um conselheiro e ganhou votos
        CARTA,         // um jogador cumpriu uma carta de conselheiro aberto (um evento por carta)
        FIM_DISPARADO, // o conselho encheu ou a última cidade foi conquistada: começam as últimas rodadas
        TURNO,         // a vez passou para o próximo jogador
        FIM            // a última rodada acabou: a partida terminou
    }

    // ---------------------------------------------------------------- grupo 1: dados fixos (ler uma vez)

    /** Os 5 herdeiros, na ordem das cartelas. */
    Chave<List<String>> HERDEIROS = new Chave<>("HERDEIROS");
    /** Os 5 tipos de tesouro. */
    Chave<List<String>> TIPOS_TESOURO = new Chave<>("TIPOS_TESOURO");
    /** A coluna (0 a 3) que, ao ser ativada, libera as colunas bloqueadas: a branca. */
    Chave<Integer> COLUNA_REATIVACAO = new Chave<>("COLUNA_REATIVACAO");
    /** Parada → centro de cada medalhão na imagem original do tabuleiro: x1, y1[, x2, y2]. */
    Chave<Map<String, int[]>> PONTOS_PARADAS = new Chave<>("PONTOS_PARADAS");
    /** Parada → raio da área de clique, em pixels da imagem original. */
    Chave<Map<String, Integer>> RAIOS_PARADAS = new Chave<>("RAIOS_PARADAS");
    /** Parada → paradas vizinhas (modo de depuração). */
    Chave<Map<String, List<String>>> VIZINHAS_PARADAS = new Chave<>("VIZINHAS_PARADAS");
    /** Província → centro do ícone: x, y. */
    Chave<Map<String, int[]>> PONTOS_PROVINCIAS = new Chave<>("PONTOS_PROVINCIAS");
    /** Província → tipo de tributo: "yurt", "espada" ou "moeda". */
    Chave<Map<String, String>> TIPOS_PROVINCIAS = new Chave<>("TIPOS_PROVINCIAS");
    /** Província → paradas na borda (modo de depuração). */
    Chave<Map<String, List<String>>> ADJACENTES_PROVINCIAS = new Chave<>("ADJACENTES_PROVINCIAS");
    /** Centro de cada cabeça do Khan: x, y. O índice da lista é o número da província do Khan (0 a 3). */
    Chave<List<int[]>> PONTOS_KHAN = new Chave<>("PONTOS_KHAN");
    /** As 3 províncias que recebem tributo de cada província do Khan, no mesmo índice de PONTOS_KHAN. */
    Chave<List<List<String>>> PROVINCIAS_KHAN = new Chave<>("PROVINCIAS_KHAN");
    /** Cidade → centro: x, y. */
    Chave<Map<String, int[]>> PONTOS_CIDADES = new Chave<>("PONTOS_CIDADES");
    /** Parada do conselheiro → os tesouros que ele pede, por exemplo [seda, seda] ou [qualquer, qualquer, qualquer]. */
    Chave<Map<String, List<String>>> PEDIDOS_CONSELHEIROS = new Chave<>("PEDIDOS_CONSELHEIROS");
    /** Parada do conselheiro → votos de cada entrega. */
    Chave<Map<String, Integer>> VOTOS_CONSELHEIROS = new Chave<>("VOTOS_CONSELHEIROS");

    // ---------------------------------------------------------------- grupo 2: dados da partida

    /** O herdeiro de cada jogador. */
    Chave<List<String>> NOMES = new Chave<>("NOMES");
    /** Votos no conselho que disparam o fim da partida (10, 14, 16 ou 17). */
    Chave<Integer> LIMITE_VOTOS = new Chave<>("LIMITE_VOTOS");
    /** Carta de conselheiro aberto → o que ela pede, em texto, na ordem da mesa. */
    Chave<Map<String, String>> CARTAS_ABERTAS = new Chave<>("CARTAS_ABERTAS");

    // ---------------------------------------------------------------- grupo 3: jogadores

    /** O jogador da vez. */
    Chave<Integer> JOGADOR_ATUAL = new Chave<>("JOGADOR_ATUAL");
    /** Moedas de cada jogador. */
    Chave<List<Integer>> MOEDAS = new Chave<>("MOEDAS");
    /** Peças de tributo de espada de cada jogador. */
    Chave<List<Integer>> ESPADAS = new Chave<>("ESPADAS");
    /** Peças de tributo de yurt de cada jogador (não são os yurts do tabuleiro). */
    Chave<List<Integer>> PECAS_YURT = new Chave<>("PECAS_YURT");
    /** Yurts que cada jogador ainda pode colocar no tabuleiro, dos 12 que tem. */
    Chave<List<Integer>> YURTS_RESTANTES = new Chave<>("YURTS_RESTANTES");
    /** Tesouros de cada jogador: tipo → quantidade. */
    Chave<List<Map<String, Integer>>> TESOUROS = new Chave<>("TESOUROS");
    /** Votos de cada jogador na trilha do Kurultai. */
    Chave<List<Integer>> VOTOS = new Chave<>("VOTOS");
    /** Parada onde está o pião de cada jogador. */
    Chave<List<String>> POSICOES = new Chave<>("POSICOES");
    /** Colunas bloqueadas (0 a 3) de cada jogador, até ele ativar a coluna branca. */
    Chave<List<List<Integer>>> COLUNAS_BLOQUEADAS = new Chave<>("COLUNAS_BLOQUEADAS");

    // ---------------------------------------------------------------- grupo 4: o turno do jogador da vez

    /** Coluna marcada na cartela e ainda não ativada, ou -1. */
    Chave<Integer> COLUNA_SELECIONADA = new Chave<>("COLUNA_SELECIONADA");
    /** Coluna ativada neste turno, ou -1. */
    Chave<Integer> COLUNA_ATIVADA = new Chave<>("COLUNA_ATIVADA");
    /** Movimentos que restam neste turno. */
    Chave<Integer> MOVIMENTOS = new Chave<>("MOVIMENTOS");
    /** Punhos que restam neste turno. */
    Chave<Integer> PUNHOS = new Chave<>("PUNHOS");
    /** A coluna ativada manda mover o Khan, e isso ainda não foi feito. */
    Chave<Boolean> KHAN_PENDENTE = new Chave<>("KHAN_PENDENTE");
    /** Paradas onde o pião esteve neste turno, a começar pela de onde saiu. */
    Chave<List<String>> VISITADAS = new Chave<>("VISITADAS");
    /** Paradas para onde o pião pode andar agora. */
    Chave<List<String>> DESTINOS = new Chave<>("DESTINOS");
    /** Províncias de onde o jogador pode pegar tributo agora. */
    Chave<List<String>> PROVINCIAS_ALCANCAVEIS = new Chave<>("PROVINCIAS_ALCANCAVEIS");
    /** Cidades que o jogador pode atacar agora. */
    Chave<List<String>> CIDADES_ATACAVEIS = new Chave<>("CIDADES_ATACAVEIS");
    /** Paradas onde o jogador pode construir um yurt agora. */
    Chave<List<String>> PARADAS_PARA_YURT = new Chave<>("PARADAS_PARA_YURT");
    /** Conselheiros a quem o jogador pode entregar tesouros agora. */
    Chave<List<String>> CONSELHEIROS_PARA_ENTREGA = new Chave<>("CONSELHEIROS_PARA_ENTREGA");

    // ---------------------------------------------------------------- grupo 5: o tabuleiro

    /** Parada → donos dos yurts construídos nela, na ordem da construção (lista vazia se não houver). */
    Chave<Map<String, List<Integer>>> YURTS_NAS_PARADAS = new Chave<>("YURTS_NAS_PARADAS");
    /** Cidades com a peça já revelada. */
    Chave<List<String>> CIDADES_REVELADAS = new Chave<>("CIDADES_REVELADAS");
    /** Cidade → tesouros que ainda estão nela. */
    Chave<Map<String, List<String>>> TESOUROS_CIDADES = new Chave<>("TESOUROS_CIDADES");
    /** Cidade → jogador que a conquistou, ou -1. */
    Chave<Map<String, Integer>> DONOS_CIDADES = new Chave<>("DONOS_CIDADES");
    /** Peças de cidade ainda na pilha, sem revelar. */
    Chave<Integer> CIDADES_NA_PILHA = new Chave<>("CIDADES_NA_PILHA");
    /** Província → peças de tributo nela (0 a 3). */
    Chave<Map<String, Integer>> PECAS_PROVINCIAS = new Chave<>("PECAS_PROVINCIAS");
    /** Província do Khan onde ele está (0 a 3), ou -1 se ele ainda está fora do tabuleiro. */
    Chave<Integer> POSICAO_KHAN = new Chave<>("POSICAO_KHAN");
    /** Tipo de tributo → peças no estoque, ao lado do tabuleiro. */
    Chave<Map<String, Integer>> ESTOQUE = new Chave<>("ESTOQUE");

    // ---------------------------------------------------------------- grupo 6: Kurultai e fim da partida

    /** Votos de todos os jogadores somados: a posição da ficha neutra. */
    Chave<Integer> VOTOS_NO_CONSELHO = new Chave<>("VOTOS_NO_CONSELHO");
    /** Carta de conselheiro aberto → jogador que a ganhou, ou -1. */
    Chave<Map<String, Integer>> DONOS_CARTAS = new Chave<>("DONOS_CARTAS");
    /** O fim da partida foi disparado. */
    Chave<Boolean> FIM_DISPARADO = new Chave<>("FIM_DISPARADO");
    /** Turnos que ainda faltam depois do atual, quando o fim já foi disparado. */
    Chave<Integer> TURNOS_RESTANTES = new Chave<>("TURNOS_RESTANTES");
    /** A partida terminou. */
    Chave<Boolean> PARTIDA_TERMINADA = new Chave<>("PARTIDA_TERMINADA");
    /** Jogadores com mais votos. */
    Chave<List<Integer>> VENCEDORES = new Chave<>("VENCEDORES");

    // ---------------------------------------------------------------- grupo 7: o evento e seus detalhes

    /** O tipo da última mudança. */
    Chave<Evento> EVENTO = new Chave<>("EVENTO");
    /** O jogador que causou o último evento, ou -1 em PARTIDA. Em TURNO, é quem encerrou o turno. */
    Chave<Integer> AUTOR_DO_EVENTO = new Chave<>("AUTOR_DO_EVENTO");
    /** Província do último tributo pego (TRIBUTO), ou null. */
    Chave<String> ULTIMA_PROVINCIA = new Chave<>("ULTIMA_PROVINCIA");
    /** Cidade do último tesouro tomado (CIDADE, CONQUISTA), ou null. */
    Chave<String> ULTIMA_CIDADE = new Chave<>("ULTIMA_CIDADE");
    /** Último tesouro tomado de uma cidade (CIDADE), ou null. */
    Chave<String> ULTIMO_TESOURO = new Chave<>("ULTIMO_TESOURO");
    /** Parada do último yurt construído (YURT), ou null. */
    Chave<String> ULTIMA_PARADA_YURT = new Chave<>("ULTIMA_PARADA_YURT");
    /** Parada do conselheiro da última entrega (CONSELHEIRO), ou null. */
    Chave<String> ULTIMO_CONSELHEIRO = new Chave<>("ULTIMO_CONSELHEIRO");
    /** Última carta de conselheiro aberto ganha (CARTA), ou null. */
    Chave<String> ULTIMA_CARTA = new Chave<>("ULTIMA_CARTA");

    void add(ObservadorIF observador);

    void remove(ObservadorIF observador);

    <T> T get(Chave<T> chave);
}
