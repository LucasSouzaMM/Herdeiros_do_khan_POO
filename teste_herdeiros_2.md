Use uma abordagem em TDD para atingir os objetivos descritos aqui.

O objetivo da 2ª iteração é iniciar a interface gráfica do jogo Herdeiros de Khan, por meio da qual se possa jogar partidas.

RESTRIÇÕES TÉCNICAS
Java 25
JUnit4
Interface gráfica com Java Swing e Java2D

REQUISITOS FUNCIONAIS

RF01: O sistema deve gerar mensagens de erro contendo nome da classe e função. Na interface gráfica, um deslocamento inválido não exibe mensagem
RF02: O sistema deve exibir no terminal os retornos das funções e os erros
RF03: O sistema deve testar interação entre classes
RF04: O sistema deve ser capaz de salvar e carregar um arquivo .txt contendo o estado de uma partida, incluindo a posição de cada pião e os movimentos restantes do turno
RF05: A primeira janela deve permitir escolher entre começar uma nova partida ou continuar uma partida salva em arquivo. Se o arquivo for inválido, a janela exibe um aviso de erro no próprio desenho da janela e continua aberta
RF06: A janela de nova partida deve exibir as cartelas dos 5 herdeiros e permitir escolher de 2 a 5 herdeiros. A ordem de escolha define a ordem dos jogadores. O nome de cada jogador é o nome do seu herdeiro
RF07: A janela de nova partida deve permitir, em vez da escolha do RF06, escolher apenas o número de jogadores (2 a 5). Nesse caso, o sistema sorteia o herdeiro de cada jogador
RF08: A janela do tabuleiro deve permitir salvar a partida corrente em um arquivo
RF09: A janela do tabuleiro deve exibir o tabuleiro, os piões de todos os jogadores e a cartela do jogador da vez. Piões na mesma parada são exibidos lado a lado
RF10: A janela do tabuleiro deve indicar o jogador da vez pela sua cor, junto com o nome do herdeiro e os movimentos restantes
RF11: O jogador da vez deve ativar uma coluna clicando nela na sua cartela, uma vez por turno. O número de movimentos é o número de cavalos da coluna
RF12: Após ativar uma coluna, o jogador deve clicar em uma parada do tabuleiro para mover o seu pião. Se o deslocamento seguir as regras (§7.1), o pião vai para a parada e os movimentos gastos são descontados; o jogador pode clicar de novo enquanto tiver movimentos. Se não seguir, nada acontece: nenhuma mensagem é exibida e o jogador da vez não muda
RF13: O turno deve passar para o próximo jogador automaticamente quando os movimentos acabam, ou quando o jogador da vez encerra o turno por um botão. O botão fica desabilitado até o jogador ativar uma coluna
RF14: O mapa deve ser um grafo de paradas e rotas, com as coordenadas de cada parada na imagem do tabuleiro e as 9 cidades com nome e posição. As províncias ficam para uma iteração futura

REQUISITOS NÃO-FUNCIONAIS
RNF01: O sistema deve exibir mensagens de erro em português
RNF02: O sistema deve testar classes individualmente
RNF03: O sistema deve possuir aleatoriedade, permitindo embaralhar cidades, tesouros e cartas (iteração futura: cidades, tesouros e cartas não fazem parte desta iteração)
RNF04: O sistema deve seguir o diagrama de classes diagrama.md
RNF05: O pacote model deve expor apenas a classe Jogo como pública; as demais classes do modelo devem ser package-private
RNF06: O tabuleiro, os piões e as cartas devem ser exibidos somente por meio do método drawImage() da classe Graphics2D, nunca por componentes Swing (JLabel, JButton, JPanel etc.)
RNF07: As janelas do jogo devem ter no máximo 1400 pixels de largura e 1050 pixels de altura
RNF08: O tratamento de eventos de componentes gráficos não deve usar expressões lambda
RNF09: O sistema não deve usar caixas de diálogo (JDialog, JOptionPane etc.) para exibir mensagens simples
RNF10: O pacote model não deve importar javax.swing nem java.awt; a interface gráfica deve se comunicar apenas com a classe Jogo

ITENS DO ENUNCIADO QUE NÃO SE APLICAM AO KHAN
O enunciado da 2ª iteração foi escrito para o jogo Clue. Estes itens não têm equivalente nas regras de Herdeiros de Khan:
- Lançamento de dados: no Khan, o número de movimentos é o número de cavalos da coluna ativada (regras §7.1). Por isso não há dados nem definição dos valores dos dados pelo testador.
- Passagens secretas: o mapa do Khan não tem passagens secretas.

ITENS FUTUROS
- Habilidades especiais dos herdeiros (por exemplo, o segundo pião de Altani).
- Peças de ativação: bloquear a coluna usada até a coluna branca devolver as peças. Nesta iteração, qualquer coluna pode ser ativada em qualquer turno.
- Ações das colunas além dos cavalos: punhos (tributos), Khan e moedas.
- Padrão Observer, quando houver mais de uma janela exibindo o mesmo estado da partida. Nesta iteração, a janela é redesenhada após cada ação.
- Modo desenvolvedor, com acesso a trapaças (cheats) para testar situações específicas. Até lá, o arquivo de estado da partida (RF04 da 1ª iteração) permite criar e editar estados para teste.

ROTEIRO DE TESTES
Execute cada teste e marque um resultado: Passou ou Falhou. Nos casos de falha, exibe o que foi feito.

Testes automáticos (JUnit)
Os testes T1 a T9 da 1ª iteração continuam valendo, adaptados para criar a partida com herdeiros.

J1
Passos: Criar uma partida com Tolui como primeiro jogador e ativar a coluna verde.
Resultado: O jogador tem 2 movimentos, que é o número de cavalos dessa coluna na cartela de Tolui.

J2
Passos: Encerrar o turno sem ativar uma coluna.
Resultado: Exibe uma mensagem de erro e o turno não passa.

J3
Passos: Ativar uma coluna e gastar todos os movimentos.
Resultado: O turno passa automaticamente para o próximo jogador.

J4
Passos: Salvar uma partida e carregar o arquivo salvo.
Resultado: Os herdeiros, a ordem dos jogadores, as posições dos piões e os movimentos restantes são os mesmos.

J5
Passos: Carregar um arquivo de partida com um herdeiro que não existe.
Resultado: Exibe uma mensagem de erro.

J6
Passos: Criar uma partida com um herdeiro repetido.
Resultado: Exibe uma mensagem de erro.

J7
Passos: Criar 20 partidas com sorteio de herdeiros para N jogadores.
Resultado: Cada partida tem N herdeiros diferentes, e pelo menos 2 partidas têm herdeiros diferentes para algum jogador.

J8
Passos: Com 1 movimento, mover o pião para uma parada vizinha.
Resultado: O pião vai para a parada, os movimentos chegam a 0 e o turno passa.

J9
Passos: Com 1 movimento, mover o pião para uma parada a 2 passos.
Resultado: Exibe uma mensagem de erro e o pião não sai do lugar.

J10
Passos: Com 2 movimentos, mover o pião 1 passo e depois mais 1 passo.
Resultado: Os dois deslocamentos são feitos.

J11
Passos: Mover o pião através de uma parada ocupada até uma parada livre.
Resultado: O pião vai para a parada livre.

J12
Passos: Mover o pião para uma parada simples ocupada por outro pião.
Resultado: Exibe uma mensagem de erro e o pião não sai do lugar.

J13
Passos: Mover um pião para uma parada dupla com 1 pião e depois outro pião para a mesma parada dupla.
Resultado: O primeiro deslocamento é feito. O segundo exibe uma mensagem de erro.

J14
Passos: Mover piões para Karakorum com outros piões já em Karakorum.
Resultado: Os deslocamentos são feitos.

J15
Passos: Mover o pião antes de ativar uma coluna.
Resultado: Exibe uma mensagem de erro.

J16
Passos: Verificar o mapa.
Resultado: Toda rota liga paradas existentes e toda parada é alcançável a partir de Karakorum.

J17
Passos: Consultar a parada em pontos dentro e fora do raio de uma parada, incluindo os dois medalhões de uma parada dupla.
Resultado: Dentro do raio, devolve a parada. Fora, não devolve nenhuma parada.

Testes manuais da interface gráfica

G1
Passos: Na primeira janela, clicar em Novo jogo.
Resultado: A primeira janela fecha e a janela de escolha dos herdeiros abre.

G2
Passos: Na primeira janela, clicar em Continuar e escolher um arquivo de partida válido.
Resultado: A janela do tabuleiro abre no estado salvo.

G3
Passos: Na primeira janela, clicar em Continuar e escolher um arquivo inválido.
Resultado: O erro é exibido no terminal, a janela exibe um aviso de arquivo inválido e continua aberta.

G4
Passos: Na janela de herdeiros, marcar 1 herdeiro e clicar em Jogar.
Resultado: Nada acontece.

G5
Passos: Na janela de herdeiros, marcar 5 herdeiros e clicar em Jogar.
Resultado: A janela do tabuleiro abre com 5 piões em Karakorum.

G6
Passos: Na janela de herdeiros, marcar Tolui e depois Altani, e clicar em Jogar.
Resultado: Tolui é o primeiro jogador.

G7
Passos: Escolher o sorteio com 3 jogadores e criar duas partidas.
Resultado: Cada partida tem 3 herdeiros diferentes, e as duas partidas podem ter herdeiros diferentes para cada jogador.

G8
Passos: Abrir cada janela do jogo.
Resultado: Nenhuma janela passa de 1400 x 1050 pixels.

G9
Passos: Abrir a janela do tabuleiro em uma partida nova.
Resultado: Exibe o tabuleiro, todos os piões em Karakorum e a cartela do 1º jogador, com o painel na cor dele.

G10
Passos: Clicar em uma coluna da cartela que tem N cavalos.
Resultado: Os movimentos restantes passam a ser N.

G11
Passos: Depois de ativar uma coluna, clicar em outra coluna no mesmo turno.
Resultado: Nada acontece.

G12
Passos: Clicar em uma parada antes de ativar uma coluna.
Resultado: Nada acontece.

G13
Passos: Ativar uma coluna e clicar em uma parada válida.
Resultado: O pião vai para a parada e os movimentos gastos são descontados.

G14
Passos: Ativar uma coluna e clicar em uma parada mais distante que os movimentos restantes.
Resultado: Nada acontece, nenhuma mensagem é exibida e o jogador da vez não muda.

G15
Passos: Gastar todos os movimentos do turno.
Resultado: O turno passa automaticamente, e o painel exibe a cor e a cartela do próximo jogador.

G16
Passos: Verificar o botão Próximo antes e depois de ativar uma coluna.
Resultado: Antes, o botão está desabilitado. Depois, clicar nele passa o turno para o próximo jogador.

G17
Passos: Passar o turno do último jogador.
Resultado: O turno volta para o 1º jogador.

G18
Passos: Salvar a partida e depois continuar a partida com o arquivo salvo.
Resultado: As posições dos piões, o jogador da vez e os movimentos restantes são os mesmos.

G19
Passos: Ativar uma coluna e clicar em uma parada simples ocupada por outro pião.
Resultado: Nada acontece.

G20
Passos: Mover dois piões para a mesma parada dupla.
Resultado: Os dois piões aparecem lado a lado na parada.

G21
Passos: Executar o jogo com a opção -Dmapa.debug=true.
Resultado: Cada parada aparece com um círculo numerado e cada rota com uma linha, alinhados com o tabuleiro.

CRITÉRIOS DE ACEITAÇÃO
CA01: Dado que o usuário queira criar um jogo com menos de 2 jogadores, quando ele for criar a partida, então o sistema exibe uma mensagem de erro
CA02: Dado que o usuário queira criar um jogo com mais de 5 jogadores, quando ele for criar a partida, então o sistema exibe uma mensagem de erro
CA03: Dado que o usuário queira realizar uma ação fora do seu turno, quando ele for realizar uma ação, o sistema o alerta com um erro
CA04: Dado que o jogador clique em uma parada para a qual o deslocamento não é permitido, quando ele clicar, então nada acontece e nenhuma mensagem é exibida
CA05: Dado que o jogador gaste todos os seus movimentos, quando o último movimento terminar, então o turno passa automaticamente para o próximo jogador
CA06: Dado que o usuário escolha um arquivo inválido para continuar uma partida, quando ele confirmar a escolha, então a primeira janela exibe um aviso e continua aberta
