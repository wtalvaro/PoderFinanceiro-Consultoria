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

        // Deixa o JavaFX decidir o tamanho inicial com base no login.fxml
        showScene("/fxml/login.fxml", "Poder Financeiro - Acesso");

        // Centraliza apenas na primeira abertura
        stage.centerOnScreen();
    }

    public void showScene(String fxmlPath, String title) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();

            if (stage.getScene() == null) {
                // O JavaFX lerá o prefWidth/Height do FXML automaticamente
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }

            stage.setTitle(title);

            // DELEGAÇÃO TOTAL: O Java pergunta ao sistema gráfico o espaço necessário
            stage.sizeToScene();

            // Mantém a janela estável no centro após o redimensionamento automático
            stage.centerOnScreen();

            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar a interface: " + fxmlPath, e);
        }
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