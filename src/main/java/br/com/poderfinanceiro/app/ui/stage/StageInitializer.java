package br.com.poderfinanceiro.app.ui.stage;

import br.com.poderfinanceiro.app.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Initializer refinado. Não escuta mais eventos automáticos do Spring.
 * Agora é comandado ativamente pelo JavafxApplication após o carregamento
 * assíncrono.
 */
@Component
public class StageInitializer {

    private final ApplicationContext applicationContext;
    private Stage primaryStage;

    // Injetamos o ApplicationContext para garantir que o FXMLLoader
    // consiga criar todos os Controllers como Beans do Spring
    public StageInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Método público que realiza o carregamento final do FXML Wide
     * após a Splash Screen finalizar.
     */
    public void loadMainView() {
        try {
            // 1. Carrega o layout mestre usando o Spring para instanciar o MainController
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // 2. 🚀 A CURA: Captura os limites reais do monitor (descontando barras de
            // tarefas)
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double width = Math.min(1280, bounds.getWidth() * 0.9);
            double height = Math.min(720, bounds.getHeight() * 0.9);

            Scene scene = new Scene(root, width, height);
            primaryStage.setScene(scene);

            // 3. 🎨 CONFIGURAÇÃO DO ÍCONE
            try {
                var iconStream = getClass().getResourceAsStream("/icons/app.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
                }
            } catch (Exception e) {
                System.err.println("Não foi possível carregar o ícone: " + e.getMessage());
            }

            // 4. 🛡️ Blindagem de Geometria e Configuração Final
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(700);
            primaryStage.setTitle("Poder Financeiro ERP");

            // Força a janela a abrir maximizada, aproveitando o novo layout Wide
            primaryStage.setMaximized(true);

            primaryStage.show();
            primaryStage.centerOnScreen();

            // 5. O Fluxo Oficial: Direciona para o Login inicialmente
            MainController mainController = applicationContext.getBean(MainController.class);
            mainController.navegarPara("/fxml/login.fxml", false);

        } catch (IOException e) {
            throw new RuntimeException("Falha ao inicializar o layout mestre após o boot.", e);
        }
    }

    public void logout() {
        MainController mainController = applicationContext.getBean(MainController.class);
        mainController.mostrarOverlaySair();
    }
}