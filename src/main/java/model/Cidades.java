package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Cidades e tesouros (§5; regras, p. 10): a pilha de peças de cidade, o saco de tesouros, o ataque com espadas e
// a conquista.
class Cidades {
    private final Mapa mapa;
    private final Yurts yurts; // C2: conquistar põe um yurt no meio da cidade
    private final List<Cidade> pilha = new ArrayList<>(); // peças de cidade ainda não reveladas, na ordem em que saem
    private final List<Tesouro> saco = new ArrayList<>();  // peças de tesouro ainda não colocadas em cidades

    Cidades(Mapa mapa, Yurts yurts) {
        this.mapa = mapa;
        this.yurts = yurts;
    }

    // §5: as 9 peças de cidade e as 40 de tesouro são embaralhadas; 3 cidades são reveladas com 4 tesouros cada.
    void sortear(Random aleatorio) {
        List<String> ordem = new ArrayList<>();
        for (Cidade c : mapa.getCidades()) ordem.add(c.nome());
        Collections.shuffle(ordem, aleatorio);
        List<Tesouro> tesouros = new ArrayList<>();
        for (Tesouro t : Tesouro.values())
            for (int i = 0; i < Tesouro.POR_TIPO; i++) tesouros.add(t);
        Collections.shuffle(tesouros, aleatorio);
        preparar(ordem, tesouros);
    }

    // Usado também pelos testes, para começar com cidades e tesouros conhecidos: as cidades saem na ordem dada,
    // e cada cidade revelada recebe os próximos 4 tesouros do saco.
    void preparar(List<String> ordem, List<Tesouro> tesouros) {
        for (Cidade c : mapa.getCidades()) c.reiniciar();
        pilha.clear();
        for (String nome : ordem) pilha.add(mapa.getCidade(nome));
        saco.clear();
        saco.addAll(tesouros);
        for (int i = 0; i < 3; i++) revelarProxima();
    }

    // Revela a próxima cidade da pilha com até 4 tesouros do saco. Sem cidades na pilha, nada acontece.
    private void revelarProxima() {
        if (pilha.isEmpty()) return;
        List<Tesouro> novos = new ArrayList<>();
        while (novos.size() < Cidade.TESOUROS && !saco.isEmpty()) novos.add(saco.remove(0));
        pilha.remove(0).revelar(novos);
    }

    /**
     * Toma o tesouro da cidade com espadas. A cidade precisa estar revelada e ser adjacente a uma parada onde o
     * pião esteve neste turno. O 1º tesouro de uma cidade no turno custa 1 espada, e cada tesouro seguinte da
     * mesma cidade custa 2. Quem toma o último tesouro conquista a cidade, e a próxima cidade da pilha é
     * revelada. Retorna true se a cidade foi conquistada.
     * C2 (vídeo 1): a conquista põe um yurt do jogador no meio da cidade, então quem já usou os seus 12 yurts não
     * pode tomar o último tesouro.
     */
    boolean atacar(Jogador j, Turno turno, Cidade c, Tesouro t) {
        if (!c.isRevelada() || c.getDono() != null)
            throw new IllegalStateException("Erro em Cidades.atacar: " + c.nome() + " não pode ser atacada agora"
                    + (c.getDono() != null ? " (já foi conquistada)." : " (a cidade ainda não foi revelada)."));
        if (!c.getTesouros().contains(t))
            throw new IllegalArgumentException("Erro em Cidades.atacar: " + c.nome() + " não tem o tesouro "
                    + t.getNome() + ".");
        if (!adjacenteAoCaminho(c, turno))
            throw new IllegalArgumentException("Erro em Cidades.atacar: " + c.nome() + " não é adjacente a nenhuma"
                    + " parada onde " + j.getNome() + " esteve neste turno.");
        if (!podeConquistar(j, c))
            throw new IllegalStateException("Erro em Cidades.atacar: tomar o último tesouro conquista " + c.nome()
                    + ", e " + j.getNome() + " já usou os seus " + Jogador.YURTS + " yurts.");
        int custo = custo(c, turno);
        if (turno.disponivel(Tributo.ESPADA) < custo)
            throw new IllegalStateException("Erro em Cidades.atacar: tomar este tesouro de " + c.nome() + " custa "
                    + custo + " espada(s), e " + j.getNome() + " tem " + turno.disponivel(Tributo.ESPADA) + ".");
        turno.pagar(Tributo.ESPADA, custo); // as espadas virtuais primeiro
        c.retirar(t);
        j.ganharTesouro(t);
        turno.registrarAtaque(c);
        if (!c.getTesouros().isEmpty()) return false;
        c.conquistar(j);
        revelarProxima();
        return true;
    }

    // Tomar o tesouro não conquista a cidade, ou o jogador ainda tem um yurt para pôr nela.
    private boolean podeConquistar(Jogador j, Cidade c) {
        return c.getTesouros().size() > 1 || yurts.restantes(j) > 0;
    }

    private static int custo(Cidade c, Turno turno) {
        return turno.getAtaques(c) == 0 ? 1 : 2;
    }

    private static boolean adjacenteAoCaminho(Cidade c, Turno turno) {
        for (Parada v : turno.getVisitadas())
            if (c.isAdjacente(v)) return true;
        return false;
    }

    /**
     * Cidades que o jogador do turno pode atacar agora: reveladas, adjacentes ao caminho, com espadas suficientes e,
     * se só restar 1 tesouro, com um yurt para a conquista.
     */
    List<Cidade> atacaveis(Turno turno) {
        List<Cidade> cidades = new ArrayList<>();
        if (turno == null) return cidades;
        int espadas = turno.disponivel(Tributo.ESPADA); // reais e virtuais
        for (Cidade c : mapa.getCidades())
            if (c.isRevelada() && c.getDono() == null && !c.getTesouros().isEmpty() && adjacenteAoCaminho(c, turno)
                    && espadas >= custo(c, turno) && podeConquistar(turno.getJogador(), c))
                cidades.add(c);
        return cidades;
    }

    boolean todasConquistadas() {
        for (Cidade c : mapa.getCidades())
            if (c.getDono() == null) return false;
        return true;
    }

    int conquistadas(Jogador j) {
        int n = 0;
        for (Cidade c : mapa.getCidades())
            if (c.getDono() == j) n++;
        return n;
    }

    /** Tira até n tesouros do saco, os primeiros (o saco já está embaralhado). Usado pelo baú (peça de bônus). */
    List<Tesouro> tirarDoSaco(int n) {
        List<Tesouro> tirados = new ArrayList<>();
        while (tirados.size() < n && !saco.isEmpty()) tirados.add(saco.remove(0));
        return tirados;
    }

    List<Cidade> getPilha() {
        return Collections.unmodifiableList(pilha);
    }

    List<Tesouro> getSaco() {
        return Collections.unmodifiableList(saco);
    }

    // Ao carregar uma partida salva: a pilha de cidades e o saco de tesouros, na ordem gravada.
    void restaurar(List<Cidade> pilha, List<Tesouro> saco) {
        this.pilha.clear();
        this.pilha.addAll(pilha);
        this.saco.clear();
        this.saco.addAll(saco);
    }

    /** Cidades ainda na pilha, sem revelar. */
    int naPilha() {
        return pilha.size();
    }
}
