# Diagrama de classes

This diagram follows the rules in [rules.md](rules.md). Section numbers such as "§8.1" point to that file.

All classes are in the `model` package. `Jogo` is the only `public` class. The other classes are package-private. The GUI and the tests call only `Jogo`, and they send ids (`String`) and indexes (`int`), not model objects.

## Status after iteration 2

The diagram shows the full design. The code has only part of it.

| Class | In the code now | Later |
|---|---|---|
| `Jogo` | Constructor, `sortear`, `ativarColuna`, `mover`, `encerrarTurno`, `paradaEm`, `salvar`, `carregar`, and the getters that the GUI uses (`getPosicao`, `getMovimentos`, `getPontos`, `getVizinhas` and others). | All other fields and methods. |
| `Turno` | `jogador`, `movimentos`, and the column as an `int` (0 to 3). | `Coluna` object and all other fields. |
| `Jogador` | `herdeiro`, `posicao`, and `moedas` as an `int`. | `tributos` (it replaces `moedas`) and all other fields. |
| `Herdeiro` | All constants, `nome`, `cavalos`, `porNome`. | Heir abilities. |
| `Mapa`, `Parada` | Stops, routes, coordinates, `distancia`, `paradaEm`, `contem`. | Provinces, yurts, counselors. |
| `Cidade` | `nome`, `regiao`, `x`, `y`. | Treasures and conquest. |
| `Regiao` | All constants. | |
| Other classes and enums | | All. |

The GUI is in the `view` package: `JanelaInicial`, `JanelaHerdeiros`, `JanelaTabuleiro` and `Imagens`. It calls only `Jogo` and draws each game image with `Graphics2D.drawImage`.

## Classes

```mermaid
classDiagram
    direction LR

    class Jogo {
        <<public>>
        -List~Jogador~ jogadores
        -int jogadorAtual
        -int jogadorInicial
        -Turno turno
        -Mapa mapa
        -AreaMelhorias areaMelhorias
        -Deque~Cidade~ pilhaCidades
        -Deque~TipoTesouro~ pilhaTesouros
        -List~CartaAberta~ cartasAbertas
        -Provincia khanProvincia
        -boolean khanNaAreaMelhorias
        -int votosNeutra
        -int limiteVotos
        -int turnosRestantes
        +Jogo(List~String~ herdeiros)
        +sortear(int n)$ Jogo
        +escolherCartaSecreta(int jogador, int indice) void
        +ativarColuna(int jogador, int coluna) void
        +mover(int jogador, String paradaId) void
        +paradaEm(int x, int y) String
        +pegarTributo(String provinciaId) void
        +usarKhan(String destinoId) void
        +atacarCidade(String cidadeId) void
        +construirYurt(String paradaId) void
        +comprarMelhoria(int posicao, int coluna) void
        +entregarTesouros(List~TipoTesouro~ tesouros) void
        +usarBonus(Bonus bonus) void
        +encerrarTurno(int jogador) void
        +salvar(Path arquivo) void
        +carregar(Path arquivo)$ Jogo
        +isFimDeJogo() boolean
        +getVencedores() List~String~
        -darVoto(Jogador j, int votos) void
        -conferirCartasAbertas(Jogador j) void
        -contarInfluencia() void
    }

    class Turno {
        -Jogador jogador
        -Coluna coluna
        -int movimentos
        -int punhos
        -boolean khanPendente
        -Set~Parada~ paradasVisitadas
        -Set~Cidade~ cidadesAtacadas
        -int moedasVirtuais
        -int espadasVirtuais
        -int yurtsVirtuais
        -List~TipoTesouro~ tesourosVirtuais
        -List~Melhoria~ compradasNesteTurno
    }

    class Jogador {
        -Herdeiro herdeiro
        -Parada posicao
        -Parada posicaoExtra
        -int yurtsNaMao
        -EnumMap tributos
        -List~TipoTesouro~ tesouros
        -CartaSecreta cartaSecreta
        -List~CartaSecreta~ cartasParaEscolher
        -List~Bonus~ bonusNaoUsados
        -int bonusObtidos
        -int cidadesConquistadas
        -int votos
        -TabuleiroJogador tabuleiro
        +getTributo(TipoTributo t) int
        +gastarTributo(TipoTributo t, int n) void
    }

    class TabuleiroJogador {
        -Coluna[] colunas
        -int ativacoesNaMao
        +ativar(int coluna) Coluna
        +recuperarAtivacoes() void
        +adicionarMelhoria(Melhoria m, int coluna) boolean
        +linhasCompletas() int
    }

    class Coluna {
        -Regiao regiao
        -int cavalosBase
        -int punhosBase
        -boolean khanBase
        -boolean recupera
        -List~Melhoria~ melhorias
        -boolean ativada
        +cavalos() int
        +punhos() int
    }

    class Melhoria {
        -TipoMelhoria tipo
        -Regiao cor
        -TipoTesouro tesouro
    }

    class AreaMelhorias {
        -Melhoria[] espacos
        -Deque~Melhoria~ pilha
        +comprar(int posicao) Melhoria
        +comprarDaPilha() Melhoria
        +reporComKhan() List~TipoTributo~
    }

    class Mapa {
        -Map paradas
        -Map provincias
        -Map cidades
        -Parada karakorum
        +getParada(String id) Parada
        +distancia(Parada a, Parada b) int
        +paradaEm(int x, int y) Parada
        +yurtsNaRegiao(Jogador j, Regiao r) int
        +yurtsConectados(Jogador j) int
    }

    class Parada {
        -String id
        -Regiao regiao
        -int[] pontos
        -int raio
        -List~Parada~ vizinhas
        -List~Provincia~ provincias
        -List~Cidade~ cidades
        -List~Jogador~ yurts
        -List~Jogador~ peoes
        -Conselheiro conselheiro
        +isDupla() boolean
        +contem(int x, int y) boolean
        +podeTerminarAqui(Jogador j) boolean
        +podeConstruir() boolean
    }

    class Provincia {
        -String id
        -TipoTributo tipo
        -int quantidade
        -boolean doKhan
        -List~Provincia~ alvosDoKhan
        +retirar() void
        +adicionar() void
    }

    class Cidade {
        -String nome
        -Regiao regiao
        -int x
        -int y
        -List~TipoTesouro~ tesouros
        -boolean aberta
        -Jogador yurtCentral
        +retirarTesouro() TipoTesouro
    }

    class Conselheiro {
        -List~TipoTesouro~ pedido
        -int qualquerTipo
        -int votos
        -Bonus bonus
        +aceita(List~TipoTesouro~ entrega) boolean
    }

    Jogo "1" *-- "2..5" Jogador
    Jogo "1" *-- "1" Mapa
    Jogo "1" *-- "1" AreaMelhorias
    Jogo "1" *-- "0..1" Turno
    Jogo --> Cidade : pilhaCidades
    Turno --> Jogador
    Turno --> Coluna
    Jogador "1" *-- "1" TabuleiroJogador
    Jogador --> Parada : posicao
    TabuleiroJogador "1" *-- "4" Coluna
    Coluna "1" o-- "0..*" Melhoria
    AreaMelhorias "1" o-- "0..*" Melhoria
    Mapa "1" *-- "*" Parada
    Mapa "1" *-- "*" Provincia
    Mapa "1" *-- "9" Cidade
    Parada --> "*" Parada : vizinhas
    Parada --> "*" Provincia
    Parada --> "*" Cidade
    Parada "1" *-- "0..1" Conselheiro
    Provincia --> "2" Provincia : alvosDoKhan
```

## Enums

The rules give a fixed list of each of these things, so each one is an `enum`. The cards and the heirs have different rules, so each constant overrides one method.

```mermaid
classDiagram
    class Regiao {
        <<enumeration>>
        RUSSIA
        CHINA
        PERSIA
    }
    class TipoTributo {
        <<enumeration>>
        ESPADA
        YURT
        MOEDA
    }
    class TipoTesouro {
        <<enumeration>>
        FERRO
        PELE
        TIPO_3
        TIPO_4
        TIPO_5
    }
    class TipoMelhoria {
        <<enumeration>>
        MOEDA
        TESOURO
        ESPADA
        YURT
        PUNHO
        CAVALOS
    }
    class Bonus {
        <<enumeration>>
        VOTO
        DUAS_ESPADAS
        DUAS_MOEDAS
        DOIS_YURTS
        CAVALOS_E_PUNHO
        DOIS_PUNHOS
        TESOUROS
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
        +cavalos(int coluna) int
        +porNome(String nome)$ Herdeiro
    }
    class CartaAberta {
        <<enumeration>>
        QUATRO_ESPADAS
        SEIS_YURTS
        DEZ_YURTS
        QUATRO_TESOUROS
        CINCO_TESOUROS_DIFERENTES
        UM_YURT_POR_REGIAO
        DOIS_YURTS_POR_REGIAO
        DOIS_BONUS
        TRES_BONUS
        DUAS_CIDADES
        TRES_CIDADES
        LINHA_DE_MELHORIAS
        DUAS_COLUNAS_COM_DUAS
        QUATRO_NUMA_COLUNA
        +cumprida(Jogador j, Mapa m) boolean
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
        -int[] limites
        +votos(Jogador j, Mapa m) int
        #medir(Jogador j, Mapa m) int
    }
```

## Design decisions

### Map: a graph with adjacency lists

- The map is a graph. Each `Parada` is a node, and each route is an edge in the list `vizinhas`.
- A double stop is one `Parada` with two medallions in `pontos`. Thus, going through it costs 1 move (§7.1).
- Karakorum is a `Parada` with `regiao = null`. It holds all pawns and no yurts.
- Each `Parada` also keeps lists of its adjacent `Provincia` and `Cidade` objects. The game uses them for "take tribute" (§7.2), "attack a city" (§8.1), and the secret cards Prefeito, Cidadão, and Bajulador.
- `Mapa` keeps each kind of object in a `Map<String, T>`, with the id as the key. The GUI and the tests use these ids.
- `distancia` and `yurtsConectados` are breadth-first searches on the graph. `distancia` gives the cost of a move (§7.1), the tie-break rule (§14.3) and the Viajante card.
- There is no graph library. Adjacency lists are enough for a map of this size.

The map data is in the constructor of `Mapa`, in Java code. The rulebook shows the map only as a picture, so the group copied the data from the photo of the main board (`src/main/resources/imagens/tabuleiro.jpg`, 2905 x 2919 pixels).

- 34 stops: 11 in each region and Karakorum. The ids are `r1` to `r11` (Russia), `p1` to `p11` (Persia), `c1` to `c11` (China) and `karakorum`.
- 54 routes.
- `pontos` has the center of each medallion in pixels of the photo. `raio` is the click area: 60 for a medallion, 220 for Karakorum. The GUI converts a click to these pixels and calls `Jogo.paradaEm`.
- The 9 cities have a name and a position. Provinces are not in the map yet.
- Run the game with `-Dmapa.debug=true` to draw the stops and the routes over the board.

### Game pieces: counters, not objects

- Tribute pieces are interchangeable, so `Jogador.tributos` is an `EnumMap<TipoTributo, Integer>`. `Provincia.quantidade` is an `int`, because each province has one printed tribute type.
- Treasures are lists of `TipoTesouro`, because the deliveries need the type of each treasure.
- Yurts on the map are references to the owner (`Parada.yurts`, `Cidade.yurtCentral`). `Jogador.yurtsNaMao` counts the yurts that the player did not build.

### Turn state in `Turno`

Many rules depend on what happened in the current turn. `Jogo` makes a new `Turno` in `ativarColuna`. It removes the `Turno` in `encerrarTurno`.

- `paradasVisitadas`: "take tribute" (§7.2), "attack a city" (§8.1), and "build yurts" (§8.2) check this set. A stop skipped with a yurt is not in the set.
- `cidadesAtacadas`: the first treasure costs 1 sword. The next treasures from the same city cost 2 (§8.1).
- `moedasVirtuais`, `espadasVirtuais`, `yurtsVirtuais`, `tesourosVirtuais`: the upgrades of the activated column give these for this turn only (§9).
- `compradasNesteTurno`: an upgrade does not work in the turn that you buy it (§8.3).
- `khanPendente`: `encerrarTurno` fails if the column has a Khan and the player did not use it (§7.3).

### Heirs: one `enum`, checks in the rules

`Herdeiro` is an enum. Each rule method in `Jogo` or `Parada` checks the heir where the ability changes the rule. For example, `construirYurt` accepts an adjacent stop for `TOLUI`. There are 5 heirs, and each one changes one or two rules. Thus, one class for each heir is not necessary.

`Jogador.posicaoExtra` is the second pawn of Altani. It is `null` for all other heirs.

### End of the game

- `darVoto` moves the player token and the neutral token (§8.6). When `votosNeutra` reaches `limiteVotos` (10, 14, 16, or 17), `Jogo` sets `turnosRestantes` (§14).
- `contarInfluencia` uses the table in §14.1. It reads `Mapa.yurtsNaRegiao` and the number of upgrades in the `Coluna` of each region. The white column has `regiao = null`, so it never counts.
- `CartaSecreta.votos` compares the value from `medir` with the 3 numbers in `limites` (§13). For example, `SOLDADO` has `limites = {4, 7, 10}`.

## Items to confirm with the group

These items come from the **[unclear]** marks in `rules.md`. Each one can change the diagram.

- The names of treasure types 3 to 5 (`TipoTesouro`).
- The base actions and the region color of each column (`Coluna` fields). The number of horses of each column is known: `Herdeiro.cavalos` has it for each heir.
- The full ability of each heir (`Herdeiro`).
- Where delivered treasures go. This diagram assumes that `entregarTesouros` puts them back in `pilhaTesouros`.
- The positions of the coin and sword icons in the upgrade area (`AreaMelhorias.reporComKhan`).
