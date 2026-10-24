package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class JogadorTest {

  @Test
  public void guardaHerdeiroEMoedas() {
    Jogador j = new Jogador(Herdeiro.TOLUI, 2);
    assertEquals(Herdeiro.TOLUI, j.getHerdeiro());
    assertEquals("Tolui", j.getNome());
    assertEquals(2, j.getMoedas());
  }

  @Test
  public void rejeitaHerdeiroNulo() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Jogador(null, 1));
    assertTrue(e.getMessage().contains("Jogador.Jogador"));
  }

  @Test
  public void rejeitaMoedasNegativas() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Jogador(Herdeiro.TOLUI, -1));
    assertTrue(e.getMessage().contains("Jogador.Jogador"));
  }
}
