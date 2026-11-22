package model;

import java.util.Objects;

// Uma peça de melhoria (regras, p. 11, 12, 16 e 17). Ela vai para a coluna da sua cor na cartela e, a partir do
// turno seguinte ao da compra, soma uma ação à coluna sempre que a coluna for ativada.
// O id tem a forma "<tipo>-<cor>", por exemplo "moeda-vermelha" ou "cavalos-verde". No tesouro virtual, o tipo é o
// nome do tesouro: "seda-amarela".
final class Melhoria {
    enum Tipo {
        MOEDA("moeda"),     // moeda virtual: vale 1 moeda no turno
        ESPADA("espada"),   // espada virtual: vale 1 espada no turno
        YURT("yurt"),       // yurt virtual: vale 1 peça de tributo de yurt no turno
        TESOURO("tesouro"), // tesouro virtual: vale 1 tesouro do tipo no turno
        PUNHO("punho"),     // 1 punho a mais
        CAVALOS("cavalos"); // 2 movimentos a mais

        private final String nome;

        Tipo(String nome) {
            this.nome = nome;
        }

        String getNome() {
            return nome;
        }
    }

    private final Tipo tipo;
    private final Cor cor;
    private final Tesouro tesouro; // só no tesouro virtual

    Melhoria(Tipo tipo, Cor cor, Tesouro tesouro) {
        if (tipo == null || cor == null || cor == Cor.BRANCA)
            throw new IllegalArgumentException("Erro em Melhoria.Melhoria: a melhoria precisa de um tipo e de uma cor"
                    + " (vermelha, verde ou amarela).");
        if ((tipo == Tipo.TESOURO) != (tesouro != null))
            throw new IllegalArgumentException("Erro em Melhoria.Melhoria: só a melhoria de tesouro tem um tesouro.");
        this.tipo = tipo;
        this.cor = cor;
        this.tesouro = tesouro;
    }

    Melhoria(Tipo tipo, Cor cor) {
        this(tipo, cor, null);
    }

    Tipo getTipo() {
        return tipo;
    }

    Cor getCor() {
        return cor;
    }

    /** O tesouro do tesouro virtual, ou null. */
    Tesouro getTesouro() {
        return tesouro;
    }

    String getId() {
        return (tipo == Tipo.TESOURO ? tesouro.getNome() : tipo.getNome()) + "-" + cor.getNome();
    }

    static Melhoria porId(String id) {
        int traco = id == null ? -1 : id.lastIndexOf('-');
        if (traco <= 0)
            throw new IllegalArgumentException("Erro em Melhoria.porId: a melhoria " + id + " não existe.");
        String nome = id.substring(0, traco);
        Cor cor = Cor.porNome(id.substring(traco + 1));
        for (Tipo t : Tipo.values())
            if (t != Tipo.TESOURO && t.getNome().equals(nome)) return new Melhoria(t, cor);
        return new Melhoria(Tipo.TESOURO, cor, Tesouro.porNome(nome));
    }

    // Duas peças do mesmo tipo e da mesma cor são iguais (o jogo tem várias cópias de cada uma).
    @Override
    public boolean equals(Object o) {
        return o instanceof Melhoria m && m.tipo == tipo && m.cor == cor && m.tesouro == tesouro;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, cor, tesouro);
    }

    @Override
    public String toString() {
        return getId();
    }
}
