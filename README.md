# Herdeiros do Khan

Implementação em Java do jogo de tabuleiro **Herdeiros do Khan**, desenvolvida como trabalho da disciplina de **Programação Orientada a Objetos** (POO) da PUC.

## Sobre o jogo

Gengis Khan está velho e os mongóis precisam escolher o próximo líder. Cada jogador é um dos herdeiros dele e disputa votos no conselho Kurultai. Para conseguir votos, o jogador:

- cavalga pela Ásia e coleta tributos;
- ataca cidades e conquista tesouros;
- entrega os tesouros aos conselheiros;
- constrói yurts ao longo do caminho.

Quando o conselho Kurultai fica cheio, vence o jogador com mais votos e ele se torna o novo Khan.

- Jogadores: 2 a 5
- Duração: 60 a 90 minutos

As regras completas estão em [`regras.pdf`](regras.pdf). Uma versão em inglês está em [`docs/rules.md`](docs/rules.md).

## Documentação

- [`docs/diagrama.md`](docs/diagrama.md): diagrama de classes do modelo (Mermaid).
- [`teste_herdeiros.md`](teste_herdeiros.md) e [`teste_herdeiros_2.md`](teste_herdeiros_2.md): requisitos e roteiros de teste da 1ª e da 2ª iteração.
- [`.usoia.md`](.usoia.md): registro do uso de IA no trabalho.

## Requisitos

- Java 25
- Maven

## Como executar o jogo

```bash
mvn compile
java -cp target/classes app.Main
```

Para ver as paradas e as rotas do mapa desenhadas sobre o tabuleiro:

```bash
java -Dmapa.debug=true -cp target/classes app.Main
```

## Como executar os testes

```bash
mvn test
```

## Autor

Trabalho desenvolvido por Lucas Manoel e João Pedro Manhães para a disciplina de Programação Orientada a Objetos.
