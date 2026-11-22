package model;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

// §7.1: dá para passar por uma parada ocupada, mas não terminar nela. Encontrado nas partidas simuladas: um pião
// podia entrar de passagem numa parada ocupada sem ter como sair dali (todas as vizinhas ocupadas) e ficava preso,
// sem poder encerrar o turno. Agora só se entra numa parada ocupada se ainda der para chegar a uma parada livre.
public class MovimentoTest {
    private final Mapa mapa = new Mapa();
    private final Parada p5 = mapa.getParada("p5"), p6 = mapa.getParada("p6"), p7 = mapa.getParada("p7"),
            p8 = mapa.getParada("p8");
    private final Jogador jochi = new Jogador(Herdeiro.JOCHI, 1), tolui = new Jogador(Herdeiro.TOLUI, 1),
            altani = new Jogador(Herdeiro.ALTANI, 1);
    private final List<Jogador> jogadores = List.of(jochi, tolui, altani);

    // p7 só tem as vizinhas p5 e p6, e p6 é vizinha de p8. Altani ocupa p5 e p6 (2 piões), Tolui ocupa p7.
    public MovimentoTest() {
        assertEquals("p7 só tem as vizinhas p5 e p6", Set.of(p5, p6), new HashSet<>(p7.getVizinhas()));
        assertTrue("p8 é vizinha de p6", p8.getVizinhas().contains(p6));
        altani.posicionarPeoes(p5);
        altani.setPosicao(1, p6);
        tolui.posicionarPeoes(p7);
        jochi.posicionarPeoes(p8);
    }

    @Test(timeout = 1000)
    public void paradaOcupadaSoComSaida() {
        assertTrue("p8 está livre", Movimento.temSaida(p8, 0, jochi, jogadores));
        assertFalse("p6 está ocupada e não sobra movimento", Movimento.temSaida(p6, 0, jochi, jogadores));
        assertTrue("de p6 dá para voltar a p8", Movimento.temSaida(p6, 2, jochi, jogadores));
        assertFalse("de p7 não há saída: p5 e p6 estão ocupadas", Movimento.temSaida(p7, 1, jochi, jogadores));
    }

    @Test(timeout = 1000)
    public void destinosNaoLevamAUmaArmadilha() {
        Turno t = new Turno(jochi, 2); // 2 cavalos
        t.ganharMovimentos(1);         // 3 movimentos
        assertTrue("com 3 movimentos dá para entrar em p6 de passagem", Movimento.destinos(t, jogadores).contains(p6));
        jochi.setPosicao(p6);
        t.andar(p6); // 2 movimentos sobrando
        List<Parada> destinos = Movimento.destinos(t, jogadores);
        assertFalse("entraria em p7 e ficaria preso lá", destinos.contains(p7));
        assertTrue("de p6 dá para voltar a p8", destinos.contains(p8));
    }
}
