package model;

// Padrão Observer (cap. 17): quem quer saber das mudanças da partida implementa esta interface
// e se registra no observado (Jogo) por meio do controlador.
public interface ObservadorIF {
    /** Chamado a cada mudança. O parâmetro é o observado (a partida), não o observador. */
    void notify(ObservadoIF observado);
}
