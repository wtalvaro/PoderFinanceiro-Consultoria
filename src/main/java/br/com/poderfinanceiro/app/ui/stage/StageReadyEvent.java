package br.com.poderfinanceiro.app.ui.stage;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StageReadyEvent extends ApplicationEvent {

    private static final Logger log = LoggerFactory.getLogger(StageReadyEvent.class);

    public StageReadyEvent(Stage stage) {
        super(stage);
        log.info("[STAGE_READY_EVENT] Evento criado para Stage: {}", stage);
    }

    public Stage getStage() {
        return (Stage) getSource();
    }
}