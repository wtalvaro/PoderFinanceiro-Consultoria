package br.com.poderfinanceiro.app;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavafxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Inicializa o Spring Boot e integra com o JavaFX
        this.context = new SpringApplicationBuilder()
                .sources(AppApplication.class)
                .initializers(initialContext -> initialContext.getBeanFactory().registerSingleton("hostServices",
                        getHostServices()))
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        // Dispara o evento que o StageInitializer está ouvindo
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        context.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }
}