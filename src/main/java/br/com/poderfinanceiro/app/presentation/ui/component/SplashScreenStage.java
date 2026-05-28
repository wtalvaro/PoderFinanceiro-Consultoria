package br.com.poderfinanceiro.app.presentation.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splash Screen invisível. Exibe APENAS a imagem PNG flutuando na tela,
 * sem bordas do sistema operacional.
 */
public class SplashScreenStage extends Stage {

    private static final Logger log = LoggerFactory.getLogger(SplashScreenStage.class);

    private final ProgressBar progressBar;
    private final Label lblStatus;

    public SplashScreenStage() {
        log.debug("[SPLASH_SCREEN] Construtor: Inicializando SplashScreenStage");
        initStyle(StageStyle.TRANSPARENT);

        ImageView imageView = new ImageView();
        try {
            var imageStream = getClass().getResourceAsStream("/images/splash.png");
            if (imageStream != null) {
                imageView.setImage(new Image(imageStream));
                log.debug("[SPLASH_SCREEN] Imagem /images/splash.png carregada com sucesso");
            } else {
                log.error("[SPLASH_SCREEN] 🚨 AVISO: /images/splash.png não encontrada!");
            }
        } catch (Exception e) {
            log.error("[SPLASH_SCREEN] 🚨 ERRO ao ler a imagem: {}", e.getMessage(), e);
        }

        imageView.setFitWidth(600);
        imageView.setPreserveRatio(true);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(350);
        progressBar.setPrefHeight(4);
        progressBar.setStyle(
                "-fx-accent: #28a745; " +
                        "-fx-control-inner-background: rgba(255, 255, 255, 0.2); " +
                        "-fx-background-color: transparent;");

        lblStatus = new Label("Iniciando Sinais Vitais...");
        lblStatus.setTextFill(Color.WHITE);
        lblStatus.setStyle(
                "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 1);");

        VBox progressContainer = new VBox(5, progressBar, lblStatus);
        progressContainer.setAlignment(Pos.CENTER);
        progressContainer.setMouseTransparent(true);

        StackPane root = new StackPane(imageView, progressContainer);
        root.setStyle("-fx-background-color: transparent;");
        StackPane.setAlignment(progressContainer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(progressContainer, new Insets(275, 0, 0, 0));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);

        centerOnScreen();
        log.info("[SPLASH_SCREEN] SplashScreenStage configurada e centralizada");
    }

    public void atualizarProgresso(double progresso, String mensagem) {
        log.debug("[SPLASH_SCREEN] atualizarProgresso: progresso={}, mensagem='{}'", progresso, mensagem);
        progressBar.setProgress(progresso);
        lblStatus.setText(mensagem);
    }
}