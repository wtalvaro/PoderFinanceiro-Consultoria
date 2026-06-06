package br.com.poderfinanceiro.app.presentation.ui.stage;

import br.com.poderfinanceiro.app.presentation.controller.layout.MainController;
import br.com.poderfinanceiro.app.presentation.ui.navigation.AppRoute;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * <h1>StageInitializer</h1>
 * <p>
 * Responsável pela configuração e exibição do Stage principal da aplicação.
 * Realiza a transição da Splash Screen para o layout mestre (MainView) e
 * orquestra a navegação inicial para o fluxo de autenticação via Registry.
 * </p>
 */
@Component
public class StageInitializer {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(StageInitializer.class);
    private static final String LOG_PREFIX = "[StageInitializer]";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final ApplicationContext applicationContext;
    private Stage primaryStage;

    public StageInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        log.info("{} [SISTEMA] Inicializador de Stage instanciado com sucesso.", LOG_PREFIX);
    }

    /**
     * Define o Stage principal fornecido pelo ciclo de vida do JavaFX.
     * 
     * @param primaryStage O Stage raiz da aplicação.
     */
    public void setPrimaryStage(Stage primaryStage) {
        log.debug("{} [SISTEMA] Stage principal vinculado ao inicializador.", LOG_PREFIX);
        this.primaryStage = primaryStage;
    }

    /**
     * Realiza o carregamento do FXML mestre, configura a cena principal
     * e dispara a navegação inicial utilizando o AppRoute Registry.
     */
    public void loadMainView() {
        log.info("{} [TELEMETRIA] Iniciando montagem da View principal (MainView).", LOG_PREFIX);

        try {
            // Configuração do Loader integrado ao Spring
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            log.debug("{} [SISTEMA] FXML mestre carregado e controladores injetados.", LOG_PREFIX);

            // Cálculo de dimensões responsivas baseadas no monitor principal
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            double width = Math.min(1280, bounds.getWidth() * 0.9);
            double height = Math.min(720, bounds.getHeight() * 0.9);

            Scene scene = new Scene(root, width, height);
            primaryStage.setScene(scene);

            // Configuração de Identidade Visual (Ícone)
            configurarIconeAplicacao();

            // Propriedades da Janela
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(700);
            primaryStage.setTitle("Poder Financeiro ERP");
            primaryStage.setMaximized(true);

            log.info("{} [SISTEMA] Exibindo janela principal maximizada.", LOG_PREFIX);
            primaryStage.show();
            primaryStage.centerOnScreen();

            // Disparo da navegação inicial para o Login via Registry
            log.info("{} [TELEMETRIA] Redirecionando para o fluxo de autenticação via Registry.", LOG_PREFIX);
            MainController mainController = applicationContext.getBean(MainController.class);

            // CORREÇÃO: Utilizando AppRoute em vez de String/Boolean
            mainController.navegarPara(AppRoute.LOGIN);

        } catch (IOException e) {
            log.error("{} [SISTEMA] Erro fatal ao inicializar o layout mestre: {}", LOG_PREFIX, e.getMessage(), e);
            throw new RuntimeException("Falha ao inicializar o layout mestre após o boot.", e);
        }
    }

    /**
     * Tenta carregar o ícone oficial da aplicação para o Stage.
     */
    private void configurarIconeAplicacao() {
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/app.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
                log.debug("{} [SISTEMA] Ícone da aplicação carregado com sucesso.", LOG_PREFIX);
            } else {
                log.warn("{} [SISTEMA] Recurso de ícone '/icons/app.png' não localizado.", LOG_PREFIX);
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao processar o ícone da aplicação: {}", LOG_PREFIX, e.getMessage());
        }
    }

    /**
     * Inicia o fluxo visual de encerramento de sessão.
     */
    public void logout() {
        log.info("{} [TELEMETRIA] Solicitação de logout detectada. Invocando overlay de saída.", LOG_PREFIX);
        MainController mainController = applicationContext.getBean(MainController.class);
        mainController.mostrarOverlaySair();
    }
}
