package br.com.poderfinanceiro.app.presentation.ui.stage;

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

/**
 * <h1>StageReadyEvent</h1>
 * <p>
 * Evento customizado do Spring que sinaliza a prontidão do Stage principal do
 * JavaFX.
 * Utilizado para desacoplar a inicialização da interface gráfica do bootstrap
 * do Spring Boot.
 * </p>
 */
public class StageReadyEvent extends ApplicationEvent {

    private static final Logger log = LoggerFactory.getLogger(StageReadyEvent.class);
    private static final String LOG_PREFIX = "[StageReadyEvent]";

    /**
     * Construtor do evento.
     * 
     * @param stage O Stage do JavaFX que foi inicializado e está pronto para
     *              receber a Scene.
     */
    public StageReadyEvent(Stage stage) {
        super(stage);
        log.info("{} [SISTEMA] Evento de Stage pronto instanciado para o objeto: {}", LOG_PREFIX, stage);
    }

    /**
     * Recupera o Stage encapsulado no evento.
     * 
     * @return O Stage principal da aplicação.
     */
    public Stage getStage() {
        log.trace("{} [SISTEMA] Recuperando referência do Stage via evento.", LOG_PREFIX);
        return (Stage) getSource();
    }
}
