Use uma abordagem em TDD para atingir os objetivos descritos aqui.

O objetivo do sistema é implementar o jogo Herdeiros de Khan em Java.

RESTRIÇÕES TÉCNICAS
Java 25.0.2
JUnit4
Interface via terminal

REQUISITOS FUNCIONAIS

RF01: O sistema deve exibir mensagens de erro contendo nome da classe e função
RF02: O sistema deve exibir opções de funções para serem testadas individualmente
RF03: O sistema deve testar interação entre classes
RF04: O sistema deve ser capaz de ler e carregar em um arquivo .txt contendo o estado de uma partida.

REQUISITOS NÃO-FUNCIONAIS
RNF01: O sistema deve exibir mensagens de erro em português
RNF02: O sistema deve testar classes individualmente
RNF03: O sistema deve possuir aleatoriedade, permitindo embaralhar cidades, tesoura e cartas
RNF04: O sistema deve seguir o diagrama de classes diagrama.md
RNF05: O sistema deve possuir apenas as classes “Jogadores”, “Jogo” e “Turno”

ROTEIRO DE TESTES
Execute cada teste e marque um resultado: Passou ou Falhou. Nos casos de falha, exibe o que foi feito.

T1
Passos: Criar uma partida com 2 jogadores.
Resultado: Partida com 2 jogadores é criada

T2
Passos: Criar uma partida com -1 jogadores.
Resultado: Exibe uma mensagem de erro, informa o número de jogadores máximos e mínimos e então pede para tentar novamente

T3
Passos: Jogador 1 passa um turno.
Resultado: O turno do jogador 1 termina e do 2 começa.

T4
Passos: Jogador 1 passa um turno e tenta realizar uma ação.
Resultado: Mensagem de erro e impedimento de realizar ação.

T5
Passos: Jogador 1 realiza uma ação
Resultado: Ação realizada com sucesso

T6
Passos: Criar uma partida com 6 jogadores
Resultado: Exibe uma mensagem de erro, informa o número de jogadores máximos e mínimos e então pede para tentar novamente

T7
Passos: Jogador 1 tenta passar turno do jogador 2
Resultado: Exibe mensagem de erro e não permite a passagem de turno

T8
Passos: Criar uma partida com 3 jogadores e consultar as moedas iniciais de cada um.
Resultado: Jogador 1 e jogador 2 começam com 1 moeda cada, e o jogador 3 começa com 2 moedas.

T9
Passos: Criar uma partida com 5 jogadores e consultar as moedas iniciais de cada um.
Resultado: Jogadores 1 e 2 começam com 1 moeda cada, e os jogadores 3, 4 e 5 começam com 2 moedas cada.

CRITÉRIOS DE ACEITAÇÃO
CA01: Dado que o usuário queira criar um jogo com menos de 2 jogadores, quando ele for criar a partida, então o sistema exibe uma mensagem de erro
CA02: Dado que o usuário queira criar um jogo com mais de 4 jogadores, quando ele for criar a partida, então o sistema exibe uma mensagem de erro
CA03: Dado que o usuário queira realizar uma ação fora do seu turno, quando ele for realizar uma ação, o sistema o alerta com um erro
