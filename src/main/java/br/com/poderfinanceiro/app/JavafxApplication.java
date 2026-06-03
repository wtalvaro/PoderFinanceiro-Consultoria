package br.com.poderfinanceiro.app;

import atlantafx.base.theme.PrimerLight;
import br.com.poderfinanceiro.app.common.util.AsyncUtils;
import br.com.poderfinanceiro.app.presentation.ui.component.SplashScreenStage;
import br.com.poderfinanceiro.app.presentation.ui.stage.StageInitializer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <h1>JavafxApplication</h1>
 * <p>
 * Orquestrador do ciclo de vida JavaFX integrado ao Spring Boot v4.0.6.
 * Gerencia a transição entre a Splash Screen e a interface principal,
 * garantindo que o contexto do Spring seja carregado em background via Virtual
 * Threads.
 * </p>
 */
public class JavafxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(JavafxApplication.class);
    private static final String LOG_PREFIX = "[JavafxApplication]";

    private ConfigurableApplicationContext springContext;
    private SplashScreenStage splashStage;

    @Override
    public void init() throws Exception {
        log.info("{} [SISTEMA] Preparando ambiente JavaFX para inicialização.", LOG_PREFIX);
        super.init();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("{} [SISTEMA] Iniciando ciclo de vida da interface gráfica.", LOG_PREFIX);

        // Aplicação do tema moderno AtlantaFX
        log.debug("{} [SISTEMA] Aplicando tema visual PrimerLight.", LOG_PREFIX);
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Inicialização da Splash Screen
        this.splashStage = new SplashScreenStage();
        this.splashStage.show();
        log.info("{} [TELEMETRIA] Splash Screen exibida com sucesso.", LOG_PREFIX);

        // Task de simulação de progresso visual para feedback ao usuário
        Task<Void> progressTask = criarTaskProgressoVisual();
        AsyncUtils.executarTask(progressTask, null, null);

        // Orquestração do Boot do Spring em Virtual Thread
        log.info("{} [TELEMETRIA] Iniciando carregamento do contexto Spring Boot em background.", LOG_PREFIX);
        AsyncUtils.executarTaskAsync(
                () -> {
                    // Garante que o ClassLoader do contexto seja o da aplicação JavaFX
                    Thread.currentThread().setContextClassLoader(JavafxApplication.class.getClassLoader());

                    return new SpringApplicationBuilder()
                            .sources(AppApplication.class)
                            .initializers(initialContext -> initialContext.getBeanFactory()
                                    .registerSingleton("hostServices", getHostServices()))
                            .run(getParameters().getRaw().toArray(new String[0]));
                },
                context -> {
                    log.info("{} [SISTEMA] Spring Boot carregado com sucesso.", LOG_PREFIX);
                    progressTask.cancel();
                    this.springContext = context;

                    // Transição final da Splash
                    Platform.runLater(() -> splashStage.atualizarProgresso(1.0,
                            "Sinais Vitais Ok. Preparando Mesa Cirúrgica..."));

                    try {
                        // Pequena pausa para o usuário ler a última mensagem da splash
                        Thread.sleep(500);

                        log.info("{} [TELEMETRIA] Invocando StageInitializer para montar a View principal.",
                                LOG_PREFIX);
                        StageInitializer initializer = context.getBean(StageInitializer.class);
                        initializer.setPrimaryStage(primaryStage);
                        initializer.loadMainView();

                        splashStage.hide();
                        log.info("{} [AUDITORIA] Aplicação pronta para uso. Splash Screen ocultada.", LOG_PREFIX);

                    } catch (Exception e) {
                        log.error("{} [SISTEMA] Erro fatal ao montar o layout principal: {}", LOG_PREFIX,
                                e.getMessage(), e);
                        Platform.runLater(
                                () -> splashStage.atualizarProgresso(-1, "Erro ao montar o layout principal."));
                    }
                },
                ex -> {
                    log.error("{} [SISTEMA] Falha crítica no bootstrap do Spring: {}", LOG_PREFIX, ex.getMessage(), ex);
                    progressTask.cancel();
                    Platform.runLater(
                            () -> splashStage.atualizarProgresso(-1, "Falha Crítica no Boot. Verifique os logs."));
                });
    }

    /**
     * Cria uma Task para gerenciar as mensagens de progresso na Splash Screen.
     */
    private Task<Void> criarTaskProgressoVisual() {
        return new Task<>() {
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
    }

    @Override
    public void stop() throws Exception {
        log.info("{} [SISTEMA] Encerrando aplicação e liberando recursos.", LOG_PREFIX);

        if (springContext != null) {
            log.debug("{} [SISTEMA] Fechando ApplicationContext do Spring.", LOG_PREFIX);
            springContext.close();
        }

        Platform.exit();
        log.info("{} [AUDITORIA] Ciclo de vida encerrado de forma limpa.", LOG_PREFIX);

        // Garante que o processo do SO seja finalizado
        System.exit(0);
    }
}
