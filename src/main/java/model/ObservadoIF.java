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
// mapa são a lista de ids). As chaves estão em interacoes/iteracao3.md, grupos 1 a 7, e as da 4ª iteração em
// interacoes/iteracao4.md.
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
        SECRETA,       // um jogador escolheu a sua carta de conselheiro secreto
        PEAO,          // a Altani escolheu o pião que joga no turno
        SELECAO,       // uma coluna da cartela foi marcada ou desmarcada
        COLUNA,        // uma coluna foi ativada
        MOVIMENTO,     // um pião andou
        TRIBUTO,       // um jogador pegou uma peça de tributo de uma província
        CAVALO_POR_ESPADA, // o Chagatai trocou 1 movimento por 1 espada
        KHAN,          // o Khan foi movido: as províncias apontadas receberam tributos, ou a área de melhorias foi preenchida
        CIDADE,        // um jogador tomou um tesouro de uma cidade com espadas
        TESOURO_COM_MOEDA, // o Ogedei pagou 1 moeda e tomou um tesouro de uma cidade
        CONQUISTA,     // logo depois de CIDADE: era o último tesouro, e o jogador conquistou a cidade
        YURT,          // um jogador construiu um yurt numa parada
        CONSELHEIRO,   // um jogador entregou tesouros a um conselheiro e ganhou votos
        BONUS_GANHO,   // logo depois de CONSELHEIRO: foi a primeira entrega ali, e o jogador levou a peça de bônus
        BONUS_USADO,   // um jogador usou uma peça de bônus
        BAU,           // um jogador ficou com um dos tesouros do baú
        MELHORIA,      // um jogador comprou uma melhoria
        MELHORIA_GRATIS, // um jogador escolheu a melhoria grátis da linha completa
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
    /**
     * Centro de cada cabeça do Khan: x, y. O índice da lista é o número do lugar do Khan: 0 a 3 são as províncias do
     * Khan, e KHAN_AREA (4) é a área de melhorias.
     */
    Chave<List<int[]>> PONTOS_KHAN = new Chave<>("PONTOS_KHAN");
    /** As 3 províncias que recebem tributo de cada lugar do Khan, no mesmo índice de PONTOS_KHAN (vazia na área). */
    Chave<List<List<String>>> PROVINCIAS_KHAN = new Chave<>("PROVINCIAS_KHAN");
    /** O lugar do Khan que é a área de melhorias: 4. */
    Chave<Integer> KHAN_AREA = new Chave<>("KHAN_AREA");
    /** Cidade → centro: x, y. */
    Chave<Map<String, int[]>> PONTOS_CIDADES = new Chave<>("PONTOS_CIDADES");
    /** Parada do conselheiro → os tesouros que ele pede, por exemplo [seda, seda] ou [qualquer, qualquer, qualquer]. */
    Chave<Map<String, List<String>>> PEDIDOS_CONSELHEIROS = new Chave<>("PEDIDOS_CONSELHEIROS");
    /** Parada do conselheiro → votos de cada entrega. */
    Chave<Map<String, Integer>> VOTOS_CONSELHEIROS = new Chave<>("VOTOS_CONSELHEIROS");
    /** O "espaço" que é a pilha de melhorias: 5. Os espaços 0 a 4 são os da área. */
    Chave<Integer> PILHA_MELHORIAS = new Chave<>("PILHA_MELHORIAS");
    /** Moedas que custa uma melhoria: 2. */
    Chave<Integer> CUSTO_MELHORIA = new Chave<>("CUSTO_MELHORIA");
    /** Espaço (0 a 4 na área, PILHA_MELHORIAS na pilha) → retângulo: centro x, centro y, largura, altura. */
    Chave<List<int[]>> RETANGULOS_MELHORIAS = new Chave<>("RETANGULOS_MELHORIAS");
    /** Parada do conselheiro → centro do espaço da sua peça de bônus. */
    Chave<Map<String, int[]>> PONTOS_BONUS = new Chave<>("PONTOS_BONUS");
    /** Peça de bônus → o que ela dá. As chaves do mapa são os ids das 7 peças. */
    Chave<Map<String, String>> DESCRICOES_BONUS = new Chave<>("DESCRICOES_BONUS");
    /** Carta secreta → nome. As chaves do mapa são os ids das 10 cartas. */
    Chave<Map<String, String>> NOMES_CARTAS_SECRETAS = new Chave<>("NOMES_CARTAS_SECRETAS");
    /** Carta secreta → o objetivo, em texto. */
    Chave<Map<String, String>> OBJETIVOS_CARTAS_SECRETAS = new Chave<>("OBJETIVOS_CARTAS_SECRETAS");
    /** Carta secreta → a medida mínima do objetivo para 1, 2 e 3 votos. */
    Chave<Map<String, int[]>> FAIXAS_CARTAS_SECRETAS = new Chave<>("FAIXAS_CARTAS_SECRETAS");
    /** Coluna colorida (0 a 2) → nome da região da sua cor: "China", "Pérsia" e "Rússia". */
    Chave<List<String>> NOMES_REGIOES = new Chave<>("NOMES_REGIOES");

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
    /** Coluna (0 a 3) → melhorias de cada jogador, de baixo para cima, como ids "<tipo>-<cor>" ("seda-verde"). */
    Chave<List<List<List<String>>>> MELHORIAS = new Chave<>("MELHORIAS");
    /** Peças de bônus ganhas por cada jogador, usadas ou não, na ordem em que foram ganhas. */
    Chave<List<List<String>>> BONUS = new Chave<>("BONUS");
    /** Peças de bônus que cada jogador já usou. */
    Chave<List<List<String>>> BONUS_USADOS = new Chave<>("BONUS_USADOS");
    /** A parada de cada pião de cada jogador: 2 piões para a Altani, 1 para os outros. */
    Chave<List<List<String>>> PEOES = new Chave<>("PEOES");
    /** O pião (0 ou 1) que anda e age no turno; sempre 0 para quem tem 1 pião só. POSICOES é a parada dele. */
    Chave<List<Integer>> PEOES_ATIVOS = new Chave<>("PEOES_ATIVOS");
    /** As 2 cartas secretas que cada jogador recebeu no começo. */
    Chave<List<List<String>>> OPCOES_SECRETAS = new Chave<>("OPCOES_SECRETAS");
    /** A carta secreta que cada jogador escolheu, ou null antes da escolha. A View mostra só a do jogador da vez. */
    Chave<List<String>> CARTAS_SECRETAS = new Chave<>("CARTAS_SECRETAS");
    /** A medida atual do objetivo da carta secreta de cada jogador (por exemplo, quantos yurts ligados), ou 0. */
    Chave<List<Integer>> MEDIDAS_SECRETAS = new Chave<>("MEDIDAS_SECRETAS");
    /** Votos (0 a 3) que a carta secreta de cada jogador daria se a partida acabasse agora. */
    Chave<List<Integer>> VOTOS_SECRETAS = new Chave<>("VOTOS_SECRETAS");

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
    /** Antes da 1ª rodada: o jogador que escolhe a carta secreta agora, ou -1 se todos já escolheram. */
    Chave<Integer> JOGADOR_ESCOLHENDO_SECRETA = new Chave<>("JOGADOR_ESCOLHENDO_SECRETA");
    /** Melhorias grátis (linha completa) que o jogador ainda precisa escolher antes de encerrar o turno. */
    Chave<Integer> MELHORIAS_GRATIS = new Chave<>("MELHORIAS_GRATIS");
    /** "yurt", "espada" e "moeda" → peças virtuais que ainda valem neste turno (mesma ordem de ESTOQUE). */
    Chave<Map<String, Integer>> VIRTUAIS = new Chave<>("VIRTUAIS");
    /** Tesouros virtuais que ainda valem neste turno, um nome por peça (por exemplo, [seda, seda, boi]). */
    Chave<List<String>> TESOUROS_VIRTUAIS = new Chave<>("TESOUROS_VIRTUAIS");
    /** Os tesouros do baú aberto, à espera da escolha (vazia se não houver baú aberto). */
    Chave<List<String>> TESOUROS_BAU = new Chave<>("TESOUROS_BAU");
    /** O jogador é o Chagatai e ainda pode trocar 1 movimento por 1 espada agora. */
    Chave<Boolean> PODE_TROCAR_CAVALO_POR_ESPADA = new Chave<>("PODE_TROCAR_CAVALO_POR_ESPADA");
    /** O jogador é o Ogedei e pode pagar 1 moeda por um tesouro agora: CIDADES_PARA_MOEDA não está vazia. */
    Chave<Boolean> PODE_TOMAR_TESOURO_COM_MOEDA = new Chave<>("PODE_TOMAR_TESOURO_COM_MOEDA");
    /** O jogador é a Altani e pode trocar o pião que joga agora (antes de ativar a coluna). */
    Chave<Boolean> PODE_TROCAR_PEAO = new Chave<>("PODE_TROCAR_PEAO");
    /** Cidades de onde o jogador (o Ogedei) pode tomar um tesouro com 1 moeda agora. */
    Chave<List<String>> CIDADES_PARA_MOEDA = new Chave<>("CIDADES_PARA_MOEDA");
    /**
     * Espaços da área (0 a 4) e a pilha (PILHA_MELHORIAS) que o jogador pode pegar agora: comprar ou, quando
     * MELHORIAS_GRATIS > 0, escolher como melhoria grátis.
     */
    Chave<List<Integer>> ESPACOS_COMPRAVEIS = new Chave<>("ESPACOS_COMPRAVEIS");
    /** Peças de bônus que o jogador pode usar agora. */
    Chave<List<String>> BONUS_USAVEIS = new Chave<>("BONUS_USAVEIS");

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
    /** Os 5 espaços da área de melhorias, da esquerda para a direita: o id da melhoria, ou null num espaço vazio. */
    Chave<List<String>> AREA_MELHORIAS = new Chave<>("AREA_MELHORIAS");
    /** Melhorias viradas para baixo na pilha. */
    Chave<Integer> MELHORIAS_NA_PILHA = new Chave<>("MELHORIAS_NA_PILHA");
    /** Parada do conselheiro → a peça de bônus que ainda está nele, ou null se alguém já a levou. */
    Chave<Map<String, String>> BONUS_NOS_CONSELHEIROS = new Chave<>("BONUS_NOS_CONSELHEIROS");

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
    /**
     * Vencedores pela pontuação final (TOTAIS_FINAIS). No empate, ganha quem tem mais tesouros não entregues (a soma
     * de TESOUROS) e, depois, quem está mais perto de Karakorum; se ainda empatar, todos os empatados vencem.
     * Antes do fim, é o resultado se a partida acabasse agora (o mesmo vale para as chaves abaixo).
     */
    Chave<List<Integer>> VENCEDORES = new Chave<>("VENCEDORES");
    /** Coluna colorida (0 a 2) → votos de influência de cada jogador na região da cor da coluna. */
    Chave<List<List<Integer>>> VOTOS_INFLUENCIA = new Chave<>("VOTOS_INFLUENCIA");
    /** Coluna colorida (0 a 2) → yurts de cada jogador na região da cor, com os das cidades conquistadas. */
    Chave<List<List<Integer>>> YURTS_NAS_REGIOES = new Chave<>("YURTS_NAS_REGIOES");
    /** Total final de cada jogador: trilha (VOTOS) + influência nas 3 regiões + carta secreta. */
    Chave<List<Integer>> TOTAIS_FINAIS = new Chave<>("TOTAIS_FINAIS");
    /** Passos de cada jogador até Karakorum, o 2º desempate (da Altani, o pião mais perto). */
    Chave<List<Integer>> DISTANCIAS_KARAKORUM = new Chave<>("DISTANCIAS_KARAKORUM");

    // ---------------------------------------------------------------- grupo 7: o evento e seus detalhes

    /** O tipo da última mudança. */
    Chave<Evento> EVENTO = new Chave<>("EVENTO");
    /** O jogador que causou o último evento, ou -1 em PARTIDA. Em TURNO, é quem encerrou o turno. */
    Chave<Integer> AUTOR_DO_EVENTO = new Chave<>("AUTOR_DO_EVENTO");
    /** Província do último tributo pego (TRIBUTO), ou null. */
    Chave<String> ULTIMA_PROVINCIA = new Chave<>("ULTIMA_PROVINCIA");
    /** Cidade do último tesouro tomado (CIDADE, CONQUISTA, TESOURO_COM_MOEDA), ou null. */
    Chave<String> ULTIMA_CIDADE = new Chave<>("ULTIMA_CIDADE");
    /** Último tesouro tomado de uma cidade (CIDADE, TESOURO_COM_MOEDA), ou null. */
    Chave<String> ULTIMO_TESOURO = new Chave<>("ULTIMO_TESOURO");
    /** Parada do último yurt construído (YURT), ou null. */
    Chave<String> ULTIMA_PARADA_YURT = new Chave<>("ULTIMA_PARADA_YURT");
    /** Parada do conselheiro da última entrega (CONSELHEIRO), ou null. */
    Chave<String> ULTIMO_CONSELHEIRO = new Chave<>("ULTIMO_CONSELHEIRO");
    /** Última carta de conselheiro aberto ganha (CARTA), ou null. */
    Chave<String> ULTIMA_CARTA = new Chave<>("ULTIMA_CARTA");
    /** A melhoria da última compra ou escolha grátis (MELHORIA, MELHORIA_GRATIS), ou null. */
    Chave<String> ULTIMA_MELHORIA = new Chave<>("ULTIMA_MELHORIA");
    /** De onde veio ULTIMA_MELHORIA: o espaço da área (0 a 4) ou PILHA_MELHORIAS; -1 antes da primeira. */
    Chave<Integer> ESPACO_ULTIMA_MELHORIA = new Chave<>("ESPACO_ULTIMA_MELHORIA");
    /** Para onde foi ULTIMA_MELHORIA: a coluna da sua cor, ou a branca (3) na melhoria grátis; -1 antes da primeira. */
    Chave<Integer> COLUNA_ULTIMA_MELHORIA = new Chave<>("COLUNA_ULTIMA_MELHORIA");
    /** A peça de bônus do último BONUS_GANHO ou BONUS_USADO, ou null. */
    Chave<String> ULTIMO_BONUS = new Chave<>("ULTIMO_BONUS");
    /** O tesouro com que o jogador ficou no último BAU, ou null. */
    Chave<String> TESOURO_DO_BAU = new Chave<>("TESOURO_DO_BAU");
    /** As peças que o jogador ganhou na última ida do Khan à área de melhorias (KHAN): "moeda" e/ou "espada". */
    Chave<List<String>> GANHOS_DO_KHAN = new Chave<>("GANHOS_DO_KHAN");

    void add(ObservadorIF observador);

    void remove(ObservadorIF observador);

    <T> T get(Chave<T> chave);
}
