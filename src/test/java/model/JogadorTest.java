package model;

import static org.junit.Assert.*;

import org.junit.Test;

public class JogadorTest {

  @Test
  public void guardaNomeEMoedas() {
    Jogador j = new Jogador("Ana", 2);
    assertEquals("Ana", j.getNome());
    assertEquals(2, j.getMoedas());
  }

  @Test
  public void rejeitaNomeVazio() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Jogador(" ", 1));
    assertTrue(e.getMessage().contains("Jogador.Jogador"));
  }

  @Test
  public void rejeitaMoedasNegativas() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Jogador("Ana", -1));
    assertTrue(e.getMessage().contains("Jogador.Jogador"));
  }
}
