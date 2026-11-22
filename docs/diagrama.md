# Diagrama de classes

This diagram shows the code after iteration 4. It follows the rules in [rules.md](rules.md). Section numbers such as "§8.1" point to that file. The decisions of each iteration are in `interacoes/iteracaoN.md`.

The code uses MVC with 4 packages:

- `model`: the game. `Jogo` is the only `public` class, together with the Observer interfaces `ObservadoIF` and `ObservadorIF`. All other model classes are package-private.
- `view`: the windows (Swing and Java2D), the drawing helpers and the terminal output.
- `controller`: `Controlador`. It receives the actions of the windows and calls `Jogo`.
- `app`: `Main`. It starts the terminal observer and the first window.

## Status after iteration 4

| Part | In the code now | Still to do |
|---|---|---|
| Turn | Columns with horses, fists and the Khan (§7). Blocked columns until the white column is activated. Moves, with jumps over your own yurts. Virtual pieces of the column. | |
| Tributes and the Khan | 12 provinces, 4 Khan places and the melhoria area, the limited supply, "take tribute" and "move the Khan" (§7.2, §7.3). | |
| Cities | City tiles, treasures, attack with swords, conquest (§8.1). | Conquest when the player has no yurts left (to decide). |
| Yurts | Build yurts, 12 for each player, count by region (§8.2). | |
| Melhorias | 44 pieces, the area with 5 spaces and the pile, purchase, complete rows and the free melhoria, the Khan in the area. | The color of each piece is a guess (no images). |
| Kurultai | 7 counselors, deliveries, bonus pieces, 14 open counselor cards, secret cards, votes, end of the game, influence in the regions, tie-break (§8.4 to §8.6, §14). | At most 1 virtual treasure in each delivery, and where the 2 other treasures of the chest go (to decide). |
| Heirs | Horses, fists, the Khan column and the printed virtual coin of each heir. The abilities of the 5 heirs, with the 2 pawns of Altani. | |
| Save and load | The complete game (`Arquivo`, version 4). Files of iterations 2 and 3 also load. | ASCII-only file and a `.txt` filter (assignment of iteration 4). |
| Windows | Board, cartela, cards, secret cards and result. | New game dialog at the end, "Salvar jogo" enabled only before the player starts (assignment of iteration 4). |

## Packages and the Observer

The model tells the views about each change with the Observer pattern of chapter 17. The view has no access to the model: it reads the game only with `get`.

```mermaid
classDiagram
    direction LR

    namespace app {
        class Main
    }
    namespace controller {
        class Controlador {
            <<Singleton>>
            +getInstance()$ Controlador
            +registra(ObservadorIF observador) ObservadoIF
            +remove(ObservadorIF observador) void
            +novaPartida(List~String~ herdeiros) String
            +sortear(int jogadores) String
            +carregar(Path arquivo) String
            +salvar(Path arquivo) String
            +escolherCartaSecreta(String carta) String
            +selecionarColuna(int coluna) String
            +confirmarColuna() String
            +clicarTabuleiro(int x, int y) String
            +construirYurt(int x, int y) String
            +atacarCidade(String cidade, String tesouro) String
            +entregarTesouros(String parada) String
            +usarBonus(String peca) String
            +escolherTesouroBau(String tesouro) String
            +trocarPeao() String
            +trocarCavaloPorEspada() String
            +tomarTesouroComMoeda(String cidade, String tesouro) String
            +proximo() String
        }
    }
    namespace model {
        class ObservadoIF {
            <<interface>>
            +add(ObservadorIF observador) void
            +remove(ObservadorIF observador) void
            +get(Chave~T~ chave) T
        }
        class ObservadorIF {
            <<interface>>
            +notify(ObservadoIF observado) void
        }
        class Jogo {
            <<Façade, Singleton>>
        }
    }
    namespace view {
        class JanelaInicial
        class JanelaHerdeiros
        class JanelaTabuleiro
        class JanelaCartela
        class JanelaCartas
        class JanelaSecretas
        class JanelaResultado
        class Terminal
        class Desenho
        class Imagens
    }

    Main ..> Terminal : creates
    Main ..> JanelaInicial : creates
    JanelaInicial ..> JanelaHerdeiros : opens
    JanelaInicial ..> JanelaTabuleiro : opens
    JanelaHerdeiros ..> JanelaTabuleiro : opens on PARTIDA
    JanelaTabuleiro *-- JanelaCartela
    JanelaTabuleiro *-- JanelaCartas
    JanelaTabuleiro *-- JanelaSecretas
    JanelaTabuleiro *-- JanelaResultado
    JanelaHerdeiros ..|> ObservadorIF
    JanelaTabuleiro ..|> ObservadorIF : inner class Painel
    JanelaCartela ..|> ObservadorIF
    JanelaCartas ..|> ObservadorIF
    JanelaSecretas ..|> ObservadorIF
    JanelaResultado ..|> ObservadorIF
    Terminal ..|> ObservadorIF
    JanelaTabuleiro --> Controlador : actions
    JanelaCartela --> Controlador : actions
    JanelaCartas --> Controlador : actions
    JanelaSecretas --> Controlador : actions
    Controlador --> Jogo : commands
    Jogo ..|> ObservadoIF
    Jogo --> "*" ObservadorIF : notifies
```

How the Observer works:

1. A view calls `Controlador.registra(this)`. The controller adds the view to `Jogo` and returns `Jogo` typed only as `ObservadoIF`, as `Ctrl.registra` in chapter 17.
2. The view reads the fixed data (the map, the heirs, the counselors) one time. Then it calls its own `notify` one time, to get the current state.
3. After each change, `Jogo` calls `notify(this)` on all observers. In `notify`, the view reads with `get` all the values that it draws, keeps them in fields, and repaints. `paintComponent` uses only these fields.
4. The actions of the view go to the controller. Each action returns `null` or the error message. The view shows the error itself, because an error does not change the game.

The contract of the view is in the comment of `ObservadoIF.java`. From the `model` package, a view imports only `ObservadoIF` and `ObservadorIF`. This command must print nothing:

```bash
grep -rnE "import model\.[A-Za-z*]+;" src/main/java/view | grep -vE "ObservadoIF|ObservadorIF"
```

### Keys and events

`get` takes a typed key: `<T> T get(Chave<T> chave)`. For example, `get(NOMES)` returns `List<String>`, so the view needs no cast, and a wrong key is a compile error. The class `Chave<T>` and the 102 keys are nested in `ObservadoIF`, in 7 groups:

| Group | Contents | Examples |
|---|---|---|
| 1. Fixed data | The map, the heirs, the treasure types, the counselors, the melhoria area, the bonus pieces, the secret cards. The view reads them one time. | `PONTOS_PARADAS`, `PEDIDOS_CONSELHEIROS`, `RETANGULOS_MELHORIAS`, `NOMES_CARTAS_SECRETAS` |
| 2. Game data | Values that change only when a game starts. | `NOMES`, `LIMITE_VOTOS`, `CARTAS_ABERTAS` |
| 3. Players | Lists with the player as the index. | `MOEDAS`, `TESOUROS`, `PEOES`, `MELHORIAS`, `BONUS`, `CARTAS_SECRETAS` |
| 4. Turn | The state of the turn of the current player, and what the player can do now. | `MOVIMENTOS`, `DESTINOS`, `VIRTUAIS`, `ESPACOS_COMPRAVEIS`, `PODE_TROCAR_PEAO` |
| 5. Board | Yurts, cities, provinces, the Khan, the supply, the melhoria area and the bonus pieces. | `YURTS_NAS_PARADAS`, `POSICAO_KHAN`, `AREA_MELHORIAS`, `BONUS_NOS_CONSELHEIROS` |
| 6. Kurultai | Votes, the end of the game and the final score. | `VOTOS_NO_CONSELHO`, `VENCEDORES`, `VOTOS_INFLUENCIA`, `TOTAIS_FINAIS` |
| 7. Event | The kind of the last change and its details. | `EVENTO`, `AUTOR_DO_EVENTO`, `ULTIMA_CIDADE`, `ULTIMA_MELHORIA` |

The values are plain Java types (`String`, `Integer`, `Boolean`, `List`, `Map`, `int[]`), never model classes. Each call to `get` returns a new read-only copy. The keys of iteration 3 are in `interacoes/iteracao3.md`, and the keys of iteration 4 are in `interacoes/iteracao4.md`.

`get(EVENTO)` returns a value of the nested `enum Evento`. There is one event for each action. In one action, the order of the events is: the event of the action, then `CONQUISTA` or `BONUS_GANHO`, then one `CARTA` for each card, then `FIM_DISPARADO`.

## Model classes

`Jogo` is a Façade (chapter 17). It keeps what belongs to the game as a whole: the players, the turn, the checks of whose turn it is, and the events. It sends each rule to a smaller class.

```mermaid
classDiagram
    direction LR

    class Jogo {
        <<public>>
        -Jogo instancia$
        -Random aleatorio
        -List~ObservadorIF~ observadores
        -List~Jogador~ jogadores
        -int jogadorAtual
        -Turno turno
        -int selecionada
        -Evento ultimoEvento
        -int autorDoEvento
        +getInstance()$ Jogo
        +novaPartida(List~String~ herdeiros) void
        +sortear(int n) void
        +escolherCartaSecreta(int jogador, String carta) void
        +selecionarPeao(int jogador, int peao) void
        +selecionarColuna(int jogador, int coluna) void
        +confirmarColuna(int jogador) void
        +ativarColuna(int jogador, int coluna) void
        +mover(int jogador, String paradaId) void
        +pegarTributo(int jogador, String provinciaId) void
        +moverKhan(int jogador, int local) void
        +atacarCidade(int jogador, String cidade, String tesouro) void
        +construirYurt(int jogador, String paradaId) void
        +entregarTesouros(int jogador, String paradaId) void
        +comprarMelhoria(int jogador, int espaco) void
        +escolherMelhoriaGratis(int jogador, int espaco) void
        +usarBonus(int jogador, String peca) void
        +escolherTesouroBau(int jogador, String tesouro) void
        +trocarCavaloPorEspada(int jogador) void
        +tomarTesouroComMoeda(int jogador, String cidade, String tesouro) void
        +encerrarTurno(int jogador) void
        +paradaEm(int x, int y) String
        +provinciaEm(int x, int y) String
        +localKhanEm(int x, int y) int
        +espacoMelhoriaEm(int x, int y) int
        +salvar(Path arquivo) void
        +carregar(Path arquivo)$ Jogo
        +add(ObservadorIF observador) void
        +remove(ObservadorIF observador) void
        +get(Chave~T~ chave) T
        -notificar(Evento evento, int autor) void
    }

    class Turno {
        -Jogador jogador
        -int coluna
        -int movimentos
        -int punhos
        -boolean khan
        -List~Parada~ visitadas
        -Map~Cidade,Integer~ ataques
        -Map~Tributo,Integer~ virtuais
        -Map~Tesouro,Integer~ tesourosVirtuais
        -int melhoriasGratis
        -List~Tesouro~ bau
        -boolean habilidadeUsada
        ~andar(Parada p) void
        ~disponivel(Tributo t) int
        ~pagar(Tributo t, int n) void
        ~usarTesouroVirtual(Tesouro t) void
        ~abrirBau(List~Tesouro~ tesouros) void
    }

    class Jogador {
        -Herdeiro herdeiro
        -int moedas
        -int espadas
        -int yurts
        -EnumMap~Tesouro,Integer~ tesouros
        -int votos
        -List~Parada~ posicoes
        -int peaoAtivo
        -int bloqueadas
        -List~List~Melhoria~~ melhorias
        -int linhasPremiadas
        -List~Bonus~ bonus
        -Set~Bonus~ bonusUsados
        -List~CartaSecreta~ opcoesSecretas
        -CartaSecreta cartaSecreta
        ~getPosicao() Parada
        ~adicionarMelhoria(int coluna, Melhoria m) void
        ~linhasCompletas() int
    }

    class Melhorias {
        -Melhoria[] area
        -List~Melhoria~ pilha
        ~comprar(Jogador j, Turno turno, int espaco) void
        ~erroDaCompra(Jogador j, Turno turno, int espaco) String
        ~escolherGratis(Jogador j, Turno turno, int espaco) void
        ~erroDaEscolhaGratis(Jogador j, Turno turno, int espaco) String
        ~receberKhan() List~Tributo~
    }

    class Melhoria {
        -Tipo tipo
        -Cor cor
        -Tesouro tesouro
        ~getId() String
        ~porId(String id)$ Melhoria
    }

    class PecasBonus {
        -Map~Conselheiro,Bonus~ naMesa
        ~receber(Jogador j, Conselheiro c) Bonus
        ~usar(Jogador j, Turno turno, Bonus b, List~Jogador~ jogadores) void
        ~erroAoUsar(Jogador j, Turno turno, Bonus b)$ String
        ~escolherTesouroBau(Jogador j, Turno turno, Tesouro t) void
    }

    class CartasSecretas {
        ~sortear(List~Jogador~ jogadores, Random aleatorio) void
        ~escolhendo(List~Jogador~ jogadores)$ int
        ~escolher(Jogador j, CartaSecreta c) void
        ~medida(Jogador j, CartaSecreta c) int
        ~votos(Jogador j) int
    }

    class Habilidades {
        ~preparar(List~Jogador~ jogadores) void
        ~selecionarPeao(Jogador j, Turno turno, int peao)$ void
        ~erroDaTrocaDePeao(Jogador j, Turno turno)$ String
        ~trocarCavaloPorEspada(Jogador j, Turno turno, List~Jogador~ jogadores) void
        ~erroDaTrocaDeCavalo(Jogador j, Turno turno, List~Jogador~ jogadores) String
        ~tomarTesouroComMoeda(Jogador j, Turno turno, Cidade c, Tesouro t) void
        ~erroDoTesouroComMoeda(Jogador j, Turno turno, Cidade c)$ String
    }

    class Pontuacao {
        ~influencia(Jogador j, Cor cor, List~Jogador~ jogadores) int
        ~total(Jogador j, List~Jogador~ jogadores) int
        ~distanciaKarakorum(Jogador j) int
        ~vencedores(List~Jogador~ jogadores) List~Integer~
    }

    class Arquivo {
        ~salvar(Path arquivo, List~Jogador~ jogadores, int atual, Turno turno, int selecionada) void
        ~carregar(Jogo jogo, Path arquivo) void
    }

    class Mapa {
        -Map~String,Parada~ paradas
        -List~Cidade~ cidades
        -Map~String,Provincia~ provincias
        -List~LocalKhan~ locaisKhan
        -List~Conselheiro~ conselheiros
        ~distancia(Parada a, Parada b) int
        ~paradaEm(int x, int y) Parada
        ~provinciaEm(int x, int y) Provincia
        ~localKhanEm(int x, int y) int
    }

    class Parada {
        -String id
        -Regiao regiao
        -int[] pontos
        -int raio
        -List~Parada~ vizinhas
        -List~Jogador~ yurts
    }

    class Provincia {
        -String id
        -Tributo tipo
        -List~Parada~ adjacentes
        -int pecas
    }

    class LocalKhan {
        -int x
        -int y
        -List~Provincia~ provincias
    }

    class Cidade {
        -String nome
        -Regiao regiao
        -List~Parada~ adjacentes
        -List~Tesouro~ tesouros
        -boolean revelada
        -Jogador dono
    }

    class Conselheiro {
        -Parada parada
        -int pedido
        -List~Tesouro~ tipos
        -int votos
    }

    class Movimento {
        <<static>>
        ~alcance(Jogador j)$ Set~Parada~
        ~temSaida(Parada p, int m, Jogador j, List~Jogador~ jogadores)$ boolean
        ~lotada(Parada p, Jogador quem, List~Jogador~ jogadores)$ boolean
        ~destinos(Turno turno, List~Jogador~ jogadores)$ List~Parada~
    }

    class Tributos {
        -int khan
        ~estoque(Tributo t, List~Jogador~ jogadores) int
        ~pegar(Jogador j, Turno turno, Provincia p) void
        ~moverKhan(Jogador j, Turno turno, int local, List~Jogador~ jogadores) void
        ~alcancaveis(Turno turno) List~Provincia~
    }

    class Cidades {
        -List~Cidade~ pilha
        -List~Tesouro~ saco
        ~atacar(Jogador j, Turno turno, Cidade c, Tesouro t) boolean
        ~atacaveis(Turno turno) List~Cidade~
        ~tirarDoSaco(int n) List~Tesouro~
    }

    class Yurts {
        ~construir(Jogador j, Turno turno, Parada p) void
        ~restantes(Jogador j) int
        ~paradasParaYurt(Turno turno) List~Parada~
        ~noMapa(Jogador j, Regiao regiao) int
    }

    class Kurultai {
        -List~CartaAberta~ cartas
        -Map~CartaAberta,Jogador~ donosCartas
        -boolean fimDisparado
        -int turnosRestantes
        -boolean terminada
        ~entregar(Jogador j, Turno turno, Conselheiro c, int atual) void
        ~verificarCartas(int atual, Turno turno) List~CartaAberta~
        ~verificarFim(int atual) void
        ~encerrarTurno() boolean
    }

    Jogo "1" *-- "2..5" Jogador
    Jogo "1" *-- "0..1" Turno
    Jogo "1" *-- "1" Mapa
    Jogo "1" *-- "1" Tributos
    Jogo "1" *-- "1" Cidades
    Jogo "1" *-- "1" Yurts
    Jogo "1" *-- "1" Kurultai
    Jogo "1" *-- "1" Melhorias
    Jogo "1" *-- "1" PecasBonus
    Jogo "1" *-- "1" CartasSecretas
    Jogo "1" *-- "1" Habilidades
    Jogo "1" *-- "1" Pontuacao
    Jogo ..> Arquivo : salvar, carregar
    Jogo ..> Movimento
    Tributos --> Mapa
    Tributos --> Melhorias : Khan na área
    Cidades --> Mapa
    Yurts --> Mapa
    Kurultai --> Yurts
    Kurultai --> Cidades
    Kurultai --> PecasBonus : primeiro a entregar
    PecasBonus --> Tributos
    PecasBonus --> Cidades : baú
    CartasSecretas --> Yurts
    Pontuacao --> CartasSecretas
    Habilidades --> Tributos
    Habilidades --> Melhorias : Jochi
    Melhorias "1" *-- "44" Melhoria
    Turno --> Jogador
    Turno --> "*" Parada : visitadas
    Jogador --> "1..2" Parada : posicoes
    Jogador --> "*" Melhoria
    Mapa "1" *-- "34" Parada
    Mapa "1" *-- "12" Provincia
    Mapa "1" *-- "4" LocalKhan
    Mapa "1" *-- "9" Cidade
    Mapa "1" *-- "7" Conselheiro
    Parada --> "*" Parada : vizinhas
    Parada --> "*" Jogador : yurts
    Provincia --> "*" Parada : adjacentes
    LocalKhan --> "3" Provincia
    Cidade --> "3" Parada : adjacentes
    Cidade --> "0..1" Jogador : dono
    Conselheiro --> Parada
```

## Enums

The rules give a fixed list of each of these things, so each one is an `enum`.

```mermaid
classDiagram
    class Regiao {
        <<enumeration>>
        RUSSIA
        CHINA
        PERSIA
    }
    class Cor {
        <<enumeration>>
        VERMELHA
        VERDE
        AMARELA
        BRANCA
        -String nome
        -Regiao regiao
        ~coluna() int
        ~daColuna(int coluna)$ Cor
    }
    class Tributo {
        <<enumeration>>
        YURT
        ESPADA
        MOEDA
        -String nome
        -int total
    }
    class Tesouro {
        <<enumeration>>
        BOI
        SEDA
        FERRO
        PELE
        ESPECIARIA
        -String nome
        +porNome(String nome)$ Tesouro
    }
    class Herdeiro {
        <<enumeration>>
        ALTANI
        JOCHI
        CHAGATAI
        TOLUI
        OGEDEI
        -String nome
        -int[] cavalos
        -int[] punhos
        -int colunaKhan
        -int colunaMoeda
        +cavalos(int coluna) int
        +punhos(int coluna) int
        +khan(int coluna) boolean
        +moeda(int coluna) boolean
        +porNome(String nome)$ Herdeiro
    }
    class Tipo {
        <<enumeration>>
        MOEDA
        ESPADA
        YURT
        TESOURO
        PUNHO
        CAVALOS
    }
    class Bonus {
        <<enumeration>>
        TRONO
        ESPADAS
        MOEDAS
        YURTS
        CAVALOS_PUNHO
        PUNHOS
        BAU
        -String id
        -String descricao
    }
    class CartaAberta {
        <<enumeration>>
        ESPADAS4
        YURTS6
        YURTS10
        TESOUROS4
        DIFERENTES5
        REGIOES1
        REGIOES2
        CIDADES2
        CIDADES3
        BONUS2
        BONUS3
        LINHA
        COLUNAS2
        COLUNA4
        -String id
        -String descricao
    }
    class CartaSecreta {
        <<enumeration>>
        VIAJANTE
        SOLDADO
        COLECIONADOR
        SACERDOTE
        GOVERNADOR
        BAJULADOR
        PREFEITO
        CIDADAO
        CAVALEIRO
        MENSAGEIRO
        -String id
        -String nome
        -String objetivo
        -int[] faixas
    }
    class Evento {
        <<enumeration>>
        PARTIDA
        SECRETA
        PEAO
        SELECAO
        COLUNA
        MOVIMENTO
        TRIBUTO
        CAVALO_POR_ESPADA
        KHAN
        CIDADE
        TESOURO_COM_MOEDA
        CONQUISTA
        YURT
        CONSELHEIRO
        BONUS_GANHO
        BONUS_USADO
        BAU
        MELHORIA
        MELHORIA_GRATIS
        CARTA
        FIM_DISPARADO
        TURNO
        FIM
    }
```

`Evento` is nested in `ObservadoIF`, and `Tipo` (the kind of a melhoria) is nested in `Melhoria`. All other enums are package-private in `model`.

## Design decisions

### MVC and the Observer

- The view has no access to the model. It reads the game only with `get`, and it imports only `ObservadoIF` and `ObservadorIF` from `model`. `Jogo` is `public` only because the controller is in another package.
- The keys are typed constants (`Chave<T>`) and the events are an `enum`. Chapter 17 uses `int` for both. With `int`, a key and an event can have the same number, and a mix-up compiles.
- There is one event for each action. Thus, a view and the terminal know what happened only from the event.
- The terminal is an observer (`Terminal`). The controller writes only the errors and the messages of save and load, because these do not change the game.
- The tests also read the game only with `get`. `Jogo` has no public getters.
- The group does not use `java.util.Observer` (deprecated since Java 9) or `java.beans.PropertyChangeSupport` (it puts a dependency on the GUI module in the model).

### One rule, one method

A view does not calculate a rule. When a window must show what the player can do now (a button, a ring, a border), it reads a key such as `CIDADES_ATACAVEIS`, `ESPACOS_COMPRAVEIS` or `PODE_TROCAR_PEAO`. For the rules of iteration 4, the check is a method that returns the reason why the action is not possible, or `null` (for example `Melhorias.erroDaCompra`). The command and the key use the same method, so the rule is in one place. The list is in `interacoes/iteracao4.md`.

### Façade and smaller classes

`Jogo` checks whose turn it is and sends the rule to one class:

- `Movimento`: where the pawn can go with 1 move (§7.1), and if the pawn can leave an occupied stop. It has only static methods.
- `Tributos`: the provinces, the supply and the Khan (§7.2, §7.3). The fifth Khan place is the melhoria area.
- `Cidades`: the pile of city tiles, the bag of treasures, the attack and the conquest (§8.1).
- `Yurts`: build yurts, and count them by player and by region (§8.2).
- `Kurultai`: the counselors, the open counselor cards, the votes and the end of the game (§8.4 to §8.6, §14).
- `Melhorias`: the 44 pieces, the area, the pile, the purchase and the free melhoria of a complete row.
- `PecasBonus`: the bonus piece on each counselor, and the effect of each piece.
- `CartasSecretas`: the choice of the secret card and the measure of each goal.
- `Habilidades`: the abilities of the heirs.
- `Pontuacao`: the final score, the influence in the regions and the tie-break. It only calculates.
- `Arquivo`: save and load the complete game.

### Map: a graph with adjacency lists

- The map is a graph. Each `Parada` is a node, and each route is an edge in the list `vizinhas`. There is no graph library. Adjacency lists are enough for a map of this size.
- A double stop is one `Parada` with two medallions in `pontos`. Thus, going through it costs 1 move (§7.1), and it holds 2 yurts.
- Karakorum is a `Parada` with `regiao = null`. It holds all pawns and no yurts (except the yurt of Tolui).
- `Mapa` keeps the objects in maps and lists in the order of the board. The GUI and the tests use the ids.
- `distancia` is a breadth-first search on the graph.

The map data is in the constructor of `Mapa`, in Java code. The rulebook shows the map only as a picture, so the group copied the data from the photo of the main board (`src/main/resources/imagens/tabuleiro.jpg`, 2905 x 2919 pixels).

- 34 stops: 11 in each region and Karakorum. The ids are `r1` to `r11` (Russia), `p1` to `p11` (Persia), `c1` to `c11` (China) and `karakorum`.
- 54 routes.
- 9 cities, each with the 3 stops around it.
- 12 provinces, `t1` to `t12`, each with its tribute type and the stops on its border.
- 4 Khan places, each with the 3 provinces that get tribute, and the melhoria area.
- 7 counselors: the 6 double stops and Karakorum.
- The positions are pixels of the photo. The GUI changes a click to these pixels, and the controller calls `paradaEm`, `provinciaEm`, `localKhanEm` or `espacoMelhoriaEm`.
- Run the game with `-Dmapa.debug=true` to draw the stops, the routes, the provinces and the Khan places over the board.

### Game pieces: counters, not objects

- Tribute pieces are interchangeable, so `Jogador` has an `int` for each type and `Provincia.pecas` is an `int`. Each province has one printed tribute type.
- The supply is not stored. `Tributos.estoque` calculates it: `Tributo.total` minus the pieces in the provinces and the pieces of the players.
- Treasures are an `EnumMap<Tesouro, Integer>` in `Jogador`, and lists of `Tesouro` in `Cidade` and in the bag of `Cidades`.
- Yurts on the map are references to the owner (`Parada.yurts`). A conquered city has an owner (`Cidade.dono`), and that owner uses 1 of their 12 yurts for it.
- Melhorias are objects (`Melhoria`), because each one has a kind and a color. The id is `"<tipo>-<cor>"`, for example `"seda-verde"`.

### Turn state in `Turno`

Many rules depend on what happened in the current turn. `Jogo` makes a new `Turno` in `ativarColuna` and removes it in `encerrarTurno`.

- `movimentos`, `punhos` and `khan` come from the activated column (§7) and from its melhorias.
- `virtuais` and `tesourosVirtuais`: the virtual pieces of the column. They are spent before the real pieces, because they end with the turn (`disponivel`, `pagar`).
- `visitadas`: "take tribute" (§7.2), "attack a city" (§8.1), "build yurts" (§8.2) and "give treasures" (§8.4) check this list. A stop skipped with a yurt is not in the list.
- `ataques`: the first treasure of a city in a turn costs 1 sword. The next treasures of the same city cost 2 (§8.1).
- `melhoriasGratis` and `bau`: choices that the player must make before the end of the turn.
- `encerrarTurno` fails if the column has the Khan and the player did not move it (§7.3), or if a choice is still open.

### Heirs and columns

`Herdeiro` is an enum with the horses and fists of the red, green and yellow columns, the column that moves the Khan, and the column with the printed virtual coin. The white column (`Herdeiro.BRANCA`) is the same for all heirs: 2 horses, the Khan, and it releases the other columns. A used column stays blocked until the player activates the white column. `Jogador.bloqueadas` is a bit mask of the blocked columns. The color of each column (`Cor`) gives the region of its melhorias.

### End of the game

- Each vote moves the token of the player and the neutral token (§8.6). The neutral token is the sum of the votes of all players.
- The end starts when the neutral token reaches the limit (10, 14, 16 or 17 votes for 2, 3, 4 or 5 players), or when all cities are conquered (§14). The current round ends, and each player plays one more turn. `Kurultai.turnosRestantes` counts these turns.
- The final score is the track, plus the influence in the 3 regions, plus the secret card. In a tie, the player with more treasures wins, then the player nearer to Karakorum. If the tie continues, all tied players win.

## Items to confirm with the group

- The color of each melhoria (`Melhorias.todas`): the rules give only the number of each kind.
- Where the delivered treasures go. Now they leave the game (`Jogador.gastarTesouro`).
- The rule questions of iteration 4: at most 1 virtual treasure in each delivery, conquest without yurts, where the 2 other treasures of the chest go (`interacoes/iteracao4.md`).
- The optional Karakorum piece is not in the game, because there is no image of it.
