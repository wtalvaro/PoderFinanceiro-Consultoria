package br.com.poderfinanceiro.app;

import atlantafx.base.theme.PrimerLight;
import br.com.poderfinanceiro.app.infrastructure.ui.component.SplashScreenStage;
import br.com.poderfinanceiro.app.infrastructure.ui.stage.StageInitializer;
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
        log.debug("[JAVAFX_APP] init() chamado - preparando inicialização");
        super.init();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("[JAVAFX_APP] start() iniciando - aplicando tema e exibindo splash screen");

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        log.debug("[JAVAFX_APP] Tema PrimerLight aplicado");

        splashStage = new SplashScreenStage();
        splashStage.show();
        log.info("[JAVAFX_APP] SplashScreenStage exibida");

        Task<Void> progressTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                log.trace("[JAVAFX_APP] Task de progresso iniciada");
                for (int i = 0; i <= 90; i++) {
                    if (isCancelled()) {
                        log.trace("[JAVAFX_APP] Task de progresso cancelada");
                        break;
                    }

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
                log.debug("[JAVAFX_APP] Task de progresso concluída (loop finalizado)");
                return null;
            }
        };

        AsyncUtils.executarTask(progressTask, null, null);
        log.debug("[JAVAFX_APP] Task de progresso submetida ao AsyncUtils");

        AsyncUtils.executarTaskAsync(
                () -> {
                    log.debug("[JAVAFX_APP] Iniciando carregamento do Spring Boot em background");
                    Thread.currentThread().setContextClassLoader(JavafxApplication.class.getClassLoader());
                    return new SpringApplicationBuilder()
                            .sources(AppApplication.class)
                            .initializers(initialContext -> initialContext.getBeanFactory()
                                    .registerSingleton("hostServices", getHostServices()))
                            .run(getParameters().getRaw().toArray(new String[0]));
                },
                context -> {
                    log.info("[JAVAFX_APP] Spring Boot carregado com sucesso");
                    progressTask.cancel();
                    this.springContext = context;

                    splashStage.atualizarProgresso(1.0, "Sinais Vitais Ok. Preparando Mesa Cirúrgica...");
                    log.debug("[JAVAFX_APP] Splash screen atualizada para 100%");

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("[JAVAFX_APP] Pausa interrompida antes de carregar view principal");
                    }

                    try {
                        StageInitializer initializer = context.getBean(StageInitializer.class);
                        initializer.setPrimaryStage(primaryStage);
                        initializer.loadMainView();
                        splashStage.hide();
                        log.info("[JAVAFX_APP] View principal carregada e splash screen ocultada");
                    } catch (Exception e) {
                        log.error("[JAVAFX_APP] Erro ao montar o layout principal: {}", e.getMessage(), e);
                        splashStage.atualizarProgresso(-1, "Erro ao montar o layout principal.");
                    }
                },
                ex -> {
                    log.error("[JAVAFX_APP] Erro crítico no boot do Spring: {}", ex.getMessage(), ex);
                    progressTask.cancel();
                    splashStage.atualizarProgresso(-1, "Falha Crítica no Boot.");
                });
    }

    @Override
    public void stop() throws Exception {
        log.info("[JAVAFX_APP] stop() chamado - encerrando aplicação");
        if (springContext != null) {
            log.debug("[JAVAFX_APP] Fechando contexto Spring");
            springContext.close();
        }
        Platform.exit();
        System.exit(0);
        log.trace("[JAVAFX_APP] Platform.exit() e System.exit(0) executados");
    }
}