package br.com.poderfinanceiro.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import br.com.poderfinanceiro.app.controller.MainController;
import java.io.IOException;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ApplicationContext applicationContext;
    private Stage stage;

    public StageInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        this.stage = event.getStage();

        try {
            // 1. Carrega o CONTAINER MESTRE apenas uma vez na vida do App
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // 2. Trava a geometria da janela para o KDE/Wayland ficar estável (1280x800)
            stage.setScene(new Scene(root, 1280, 800));
            stage.setTitle("Poder Financeiro - Consultoria");
            stage.show();
            stage.centerOnScreen();

            // 3. Pede ao MainController para injetar o Login e Ocultar a Sidebar
            MainController mainController = applicationContext.getBean(MainController.class);
            mainController.navegarPara("/fxml/login.fxml", false);

        } catch (IOException e) {
            throw new RuntimeException("Falha catastrófica ao inicializar o layout mestre.", e);
        }
    }

    public void logout() {
        MainController mainController = applicationContext.getBean(MainController.class);
        mainController.mostrarOverlaySair();
    }
}