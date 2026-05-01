package br.com.poderfinanceiro.app;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.controller.MainController;
import java.io.IOException;
import java.net.URL;

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
        // Na primeira vez, carregamos a cena e centralizamos
        showScene("/fxml/login.fxml", "Poder Financeiro - Acesso");
        stage.centerOnScreen(); // Centraliza APENAS na abertura inicial
    }

    public void showScene(String fxmlPath, String title) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();

            if (stage.getScene() == null) {
                // Se for a primeira cena (Login), criamos a Scene com o tamanho padrão
                stage.setScene(new Scene(root, 1024, 768));
            } else {
                // Para trocas (Login -> Cadastro ou Dashboard -> Login), apenas trocamos o
                // Root.
                // Isso mantém a janela na mesma posição X e Y atual
                stage.getScene().setRoot(root);
            }

            stage.setTitle(title);
            aplicarTransicaoSuave(root);
            stage.show();

            // REMOVIDO: stage.centerOnScreen();
            // Ao remover daqui, a janela não "pula" mais ao trocar de tela

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar a interface: " + fxmlPath, e);
        }
    }

    private void aplicarTransicaoSuave(Parent root) {
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(600), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * Método centralizado de saída do sistema.
     * Pode ser chamado por qualquer Controller.
     */
    public void logout() {
        // Em vez de Alert, buscamos o Controller da Main para mostrar o Overlay
        // Isso garante que NENHUMA nova janela seja criada no Fedora
        MainController mainController = applicationContext.getBean(MainController.class);
        mainController.mostrarOverlaySair();
    }
}