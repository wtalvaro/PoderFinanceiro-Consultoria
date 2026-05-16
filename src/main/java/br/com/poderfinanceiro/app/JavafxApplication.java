package br.com.poderfinanceiro.app;

import atlantafx.base.theme.PrimerLight;
import br.com.poderfinanceiro.app.ui.SplashScreenStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CompletableFuture;

/**
 * Ponto de entrada JavaFX centralizado.
 * Gerencia o ciclo de vida da Splash Screen e do contexto Spring Boot v4.0.6.
 */
public class JavafxApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private SplashScreenStage splashStage;

    @Override
    public void init() throws Exception {
        // O init fica vazio. O JavaFX já inicializou o toolkit neste ponto.
        // O carregamento pesado do Spring foi movido para o start() de forma
        // assíncrona.
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Aplica o tema AtlantaFX ANTES de exibir qualquer interface
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // 1. Mostra a Splash Screen IMEDIATAMENTE
        splashStage = new SplashScreenStage();
        splashStage.show();

        // 2. O MARCAPASSO: Cria uma animação fluida que estima o tempo de boot
        javafx.concurrent.Task<Void> progressTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                // Vai de 0% a 90% suavemente
                for (int i = 0; i <= 90; i++) {
                    if (isCancelled())
                        break; // Se o Spring carregar rápido, paramos o loop

                    String msg = "Carregando módulos internos...";
                    if (i < 20)
                        msg = "Iniciando Spring Boot v4.0.6...";
                    else if (i < 50)
                        msg = "Conectando ao PostgreSQL (Poder Financeiro)...";
                    else if (i < 80)
                        msg = "Mapeando Entidades (Hibernate ORM)...";

                    final double progressoAtual = i / 100.0;
                    final String mensagemFinal = msg;

                    // Atualiza a UI na thread correta
                    Platform.runLater(() -> splashStage.atualizarProgresso(progressoAtual, mensagemFinal));

                    // Pausa de 120ms (120ms * 90 = ~10.8 segundos totais)
                    Thread.sleep(120);
                }
                return null;
            }
        };

        // Inicia a barra de progresso
        Thread animacaoThread = new Thread(progressTask);
        animacaoThread.setDaemon(true);
        animacaoThread.start();

        // 3. Lança o Spring Boot de verdade no background
        CompletableFuture.supplyAsync(() -> {
            // 🚀 A VACINA DO FAT JAR:
            // Injeta o ClassLoader do Spring Boot na thread genérica de background
            // para que ela consiga ler as bibliotecas dentro do executável (.exe)
            Thread.currentThread().setContextClassLoader(JavafxApplication.class.getClassLoader());

            return new SpringApplicationBuilder()
                    .sources(AppApplication.class)
                    .initializers(initialContext -> {
                        initialContext.getBeanFactory().registerSingleton("hostServices", getHostServices());
                    })
                    .run(getParameters().getRaw().toArray(new String[0]));

        }).thenAcceptAsync(context -> {
            // 4. O Spring terminou! Paramos a animação falsa e cravamos em 100%
            progressTask.cancel();
            this.springContext = context;

            Platform.runLater(() -> {
                splashStage.atualizarProgresso(1.0, "Sinais Vitais Ok. Preparando Mesa Cirúrgica...");

                try {
                    // Dá um tempinho extra (meio segundo) só para o usuário ver o "100%"
                    Thread.sleep(500);

                    StageInitializer initializer = context.getBean(StageInitializer.class);
                    initializer.setPrimaryStage(primaryStage);
                    initializer.loadMainView();

                    splashStage.hide();
                } catch (Exception e) {
                    e.printStackTrace();
                    splashStage.atualizarProgresso(-1, "Erro ao montar o layout principal.");
                }
            });

        }, Platform::runLater).exceptionally(ex -> {
            progressTask.cancel();
            ex.printStackTrace();
            Platform.runLater(() -> splashStage.atualizarProgresso(-1, "Falha Crítica no Boot."));
            return null;
        });
    }
    
    @Override
    public void stop() throws Exception {
        // Encerra o Spring e libera as conexões JPA com o PostgreSQL
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
        System.exit(0);
    }
}