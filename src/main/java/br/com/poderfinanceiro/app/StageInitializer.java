package br.com.poderfinanceiro.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // 🚀 A CURA: Captura os limites reais do monitor (descontando barras de
            // tarefas/painéis)
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();

            // Define o tamanho como 90% da tela ou um valor seguro para 768p
            double width = Math.min(1280, bounds.getWidth() * 0.9);
            double height = Math.min(720, bounds.getHeight() * 0.9);

            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);

            // 🎨 CONFIGURAÇÃO DO ÍCONE
            // O getIcons() aceita uma lista, então você pode adicionar várias resoluções se
            // quiser
            try {
                var iconStream = getClass().getResourceAsStream("/icons/app.png");
                if (iconStream != null) {
                    stage.getIcons().add(new javafx.scene.image.Image(iconStream));
                }
            } catch (Exception e) {
                System.err.println("Não foi possível carregar o ícone: " + e.getMessage());
            }

            // 🛡️ Blindagem de Geometria
            stage.setMinWidth(1024);
            stage.setMinHeight(700);
            stage.setTitle("Poder Financeiro - Consultoria");

            stage.show();
            stage.centerOnScreen();

            MainController mainController = applicationContext.getBean(MainController.class);
            mainController.navegarPara("/fxml/login.fxml", false);

        } catch (IOException e) {
            throw new RuntimeException("Falha ao inicializar o layout mestre.", e);
        }
    }

    public void logout() {
        MainController mainController = applicationContext.getBean(MainController.class);
        mainController.mostrarOverlaySair();
    }
}