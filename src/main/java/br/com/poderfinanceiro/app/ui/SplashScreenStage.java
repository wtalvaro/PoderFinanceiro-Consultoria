package br.com.poderfinanceiro.app.ui;

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

/**
 * Splash Screen invisível. Exibe APENAS a imagem PNG flutuando na tela,
 * sem bordas do sistema operacional.
 */
public class SplashScreenStage extends Stage {

    private final ProgressBar progressBar;
    private final Label lblStatus;

    public SplashScreenStage() {
        // 1. O Segredo da Transparência: Remove até o fundo branco da janela do Fedora
        initStyle(StageStyle.TRANSPARENT);

        // 2. Carrega a Imagem
        ImageView imageView = new ImageView();
        try {
            var imageStream = getClass().getResourceAsStream("/images/splash.png");
            if (imageStream != null) {
                imageView.setImage(new Image(imageStream));
            } else {
                System.err.println("🚨 AVISO: /images/splash.png não encontrada!");
            }
        } catch (Exception e) {
            System.err.println("🚨 ERRO ao ler a imagem: " + e.getMessage());
        }

        imageView.setFitWidth(600);
        imageView.setPreserveRatio(true);

        // 3. Barra de Progresso Customizada (Verde, Estreita e Fina)
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(350); // Menor na largura
        progressBar.setPrefHeight(4); // Bem fina

        // CSS para deixar verde e remover o fundo grosso padrão do JavaFX
        progressBar.setStyle(
                "-fx-accent: #28a745; " + // Verde elegante (padrão de sucesso)
                        "-fx-control-inner-background: rgba(255, 255, 255, 0.2); " + // Fundo do trilho translúcido
                        "-fx-background-color: transparent;" // Sem bordas
        );

        // 4. Texto menor com sombreamento
        lblStatus = new Label("Iniciando Sinais Vitais...");
        lblStatus.setTextFill(Color.WHITE);
        // Letra menor e um "drop shadow" preto para garantir que dê para ler
        // mesmo se o fundo da sua imagem for claro nessa área
        lblStatus.setStyle(
                "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 1);");

        // 5. Agrupamento e Posicionamento
        VBox progressContainer = new VBox(5, progressBar, lblStatus); // 5px de espaço entre barra e texto
        progressContainer.setAlignment(Pos.CENTER);
        // O layout não deve capturar cliques
        progressContainer.setMouseTransparent(true);

        StackPane root = new StackPane(imageView, progressContainer);
        // Garante que o painel raiz não tenha nenhuma cor ou borda
        root.setStyle("-fx-background-color: transparent;");

        // Crava a barra na parte inferior, centro...
        StackPane.setAlignment(progressContainer, Pos.BOTTOM_CENTER);
        // ... e empurra exatos 60px para cima!
        StackPane.setMargin(progressContainer, new Insets(275, 0, 0, 0));

        // 6. Configuração da Cena Invisível
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); // Torna a cena "fantasma"
        setScene(scene);

        centerOnScreen();
    }

    public void atualizarProgresso(double progresso, String mensagem) {
        progressBar.setProgress(progresso);
        lblStatus.setText(mensagem);
    }
}