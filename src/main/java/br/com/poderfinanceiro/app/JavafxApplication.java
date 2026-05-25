package br.com.poderfinanceiro.app;

import atlantafx.base.theme.PrimerLight;
import br.com.poderfinanceiro.app.controller.AjudaChatController;
import br.com.poderfinanceiro.app.ui.component.SplashScreenStage;
import br.com.poderfinanceiro.app.ui.stage.StageInitializer;
import br.com.poderfinanceiro.app.util.AsyncUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada JavaFX centralizado.
 * Gerencia o ciclo de vida da Splash Screen e do contexto Spring Boot v4.0.6.
 */
public class JavafxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(JavafxApplication.class);

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

        // 2. Animação de progresso (marco passo) usando AsyncUtils
        Task<Void> progressTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i <= 90; i++) {
                    if (isCancelled())
                        break;

                    String msg = "Carregando módulos internos...";
                    if (i < 20)
                        msg = "Iniciando Spring Boot v4.0.6...";
                    else if (i < 50)
                        msg = "Conectando ao PostgreSQL (Poder Financeiro)...";
                    else if (i < 80)
                        msg = "Mapeando Entidades (Hibernate ORM)...";

                    final double progressoAtual = i / 100.0;
                    final String mensagemFinal = msg;

                    Platform.runLater(() -> splashStage.atualizarProgresso(progressoAtual, mensagemFinal));

                    Thread.sleep(120);
                }
                return null;
            }
        };

        // Dispara a animação em background (sem callbacks de sucesso/falha, pois não
        // precisa)
        AsyncUtils.executarTask(progressTask, null, null);

        // 3. Lança o Spring Boot em background com AsyncUtils
        AsyncUtils.executarTaskAsync(
                () -> {
                    // Injeta o ClassLoader do Spring Boot na thread de background
                    Thread.currentThread().setContextClassLoader(JavafxApplication.class.getClassLoader());
                    return new SpringApplicationBuilder()
                            .sources(AppApplication.class)
                            .initializers(initialContext -> initialContext.getBeanFactory()
                                    .registerSingleton("hostServices", getHostServices()))
                            .run(getParameters().getRaw().toArray(new String[0]));
                },
                context -> {
                    // Sucesso: Spring carregado → cancela a animação e crava 100%
                    progressTask.cancel();
                    this.springContext = context;

                    splashStage.atualizarProgresso(1.0, "Sinais Vitais Ok. Preparando Mesa Cirúrgica...");

                    // Pequena pausa para o usuário ver o 100% (ATENÇÃO: bloqueia a UI thread)
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    try {
                        StageInitializer initializer = context.getBean(StageInitializer.class);
                        initializer.setPrimaryStage(primaryStage);
                        initializer.loadMainView();
                        splashStage.hide();
                    } catch (Exception e) {
                        log.error("[JAVAFXAPPLICATION][START] Erro: {}", e.getMessage(), e);
                        splashStage.atualizarProgresso(-1, "Erro ao montar o layout principal.");
                    }
                },
                ex -> {
                    // Falha crítica no boot
                    progressTask.cancel();
                    log.error("[JAVAFXAPPLICATION][START] Erro crítico no boot: {}", ex.getMessage(), ex);
                    splashStage.atualizarProgresso(-1, "Falha Crítica no Boot.");
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