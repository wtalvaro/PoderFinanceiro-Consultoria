package br.com.poderfinanceiro.app.infrastructure.ui.stage;

import br.com.poderfinanceiro.app.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer refinado. Não escuta mais eventos automáticos do Spring.
 * Agora é comandado ativamente pelo JavafxApplication após o carregamento
 * assíncrono.
 */
@Component
public class StageInitializer {

    private static final Logger log = LoggerFactory.getLogger(StageInitializer.class);

    private final ApplicationContext applicationContext;
    private Stage primaryStage;

    public StageInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        log.debug("[STAGE_INITIALIZER] Construtor: Inicializado com ApplicationContext");
    }

    public void setPrimaryStage(Stage primaryStage) {
        log.debug("[STAGE_INITIALIZER] setPrimaryStage: Recebendo Stage principal");
        this.primaryStage = primaryStage;
    }

    /**
     * Método público que realiza o carregamento final do FXML Wide
     * após a Splash Screen finalizar.
     */
    public void loadMainView() {
        log.info("[STAGE_INITIALIZER] loadMainView: Iniciando carregamento da view principal");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            log.debug("[STAGE_INITIALIZER] FXML /fxml/main.fxml carregado com sucesso");

            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double width = Math.min(1280, bounds.getWidth() * 0.9);
            double height = Math.min(720, bounds.getHeight() * 0.9);
            log.trace("[STAGE_INITIALIZER] Dimensões da cena: width={}, height={}", width, height);

            Scene scene = new Scene(root, width, height);
            primaryStage.setScene(scene);

            try {
                var iconStream = getClass().getResourceAsStream("/icons/app.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
                    log.debug("[STAGE_INITIALIZER] Ícone da aplicação carregado");
                } else {
                    log.warn("[STAGE_INITIALIZER] Ícone /icons/app.png não encontrado");
                }
            } catch (Exception e) {
                log.error("[STAGE_INITIALIZER] Não foi possível carregar o ícone: {}", e.getMessage(), e);
            }

            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(700);
            primaryStage.setTitle("Poder Financeiro ERP");
            primaryStage.setMaximized(true);
            primaryStage.show();
            primaryStage.centerOnScreen();
            log.info("[STAGE_INITIALIZER] Janela principal exibida e maximizada");

            MainController mainController = applicationContext.getBean(MainController.class);
            mainController.navegarPara("/fxml/login.fxml", false);
            log.debug("[STAGE_INITIALIZER] Navegação para tela de login solicitada");

        } catch (IOException e) {
            log.error("[STAGE_INITIALIZER] Falha ao inicializar o layout mestre após o boot", e);
            throw new RuntimeException("Falha ao inicializar o layout mestre após o boot.", e);
        }
    }

    public void logout() {
        log.info("[STAGE_INITIALIZER] logout: Usuário solicitou logout");
        MainController mainController = applicationContext.getBean(MainController.class);
        mainController.mostrarOverlaySair();
    }
}