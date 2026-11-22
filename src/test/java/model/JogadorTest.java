package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class JogadorTest {

  @Test(timeout = 1000)
  public void guardaHerdeiroEMoedas() {
    Jogador j = new Jogador(Herdeiro.TOLUI, 2);
    assertEquals("o herdeiro do jogador", Herdeiro.TOLUI, j.getHerdeiro());
    assertEquals("o nome é o do herdeiro", "Tolui", j.getNome());
    assertEquals("as moedas iniciais", 2, j.getMoedas());
  }

  @Test(timeout = 1000)
  public void rejeitaHerdeiroNulo() {
    IllegalArgumentException e = assertThrows("herdeiro nulo é recusado",
            IllegalArgumentException.class, () -> new Jogador(null, 1));
    assertTrue(e.getMessage(), e.getMessage().contains("Jogador.Jogador"));
  }

  @Test(timeout = 1000)
  public void rejeitaMoedasNegativas() {
    IllegalArgumentException e = assertThrows("moedas negativas são recusadas",
            IllegalArgumentException.class, () -> new Jogador(Herdeiro.TOLUI, -1));
    assertTrue(e.getMessage(), e.getMessage().contains("Jogador.Jogador"));
  }

  @Test(timeout = 1000)
  public void guardaPecasDeTributo() {
    Jogador j = new Jogador(Herdeiro.TOLUI, 1);
    j.ganhar(Tributo.MOEDA);
    j.ganhar(Tributo.ESPADA);
    j.ganhar(Tributo.ESPADA);
    assertEquals("1 moeda inicial e 1 ganha", 2, j.getMoedas());
    assertEquals("getTributos conta as mesmas moedas", 2, j.getTributos(Tributo.MOEDA));
    assertEquals("ganhou 2 espadas", 2, j.getTributos(Tributo.ESPADA));
    assertEquals("não ganhou yurt", 0, j.getTributos(Tributo.YURT));
  }
}
