package br.com.poderfinanceiro.app.common.util;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Utilitário de Orquestração Assíncrona.
 * Utiliza Virtual Threads (Project Loom) para execução de tarefas de I/O e IA
 * sem bloquear a JavaFX Application Thread.
 */
public class AsyncUtils {

    private static final Logger log = LoggerFactory.getLogger(AsyncUtils.class);
    private static final String LOG_PREFIX = "[AsyncUtils]";

    // 🚀 Executor baseado em Virtual Threads: Performance superior com custo de
    // memória irrisório.
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    static {
        log.info("{} [SISTEMA] Inicializado com Virtual Thread Executor (Project Loom).", LOG_PREFIX);
    }

    /**
     * Executa uma Task do JavaFX preconfigurada.
     * Garante que os callbacks onSuccess e onFailed rodem na UI Thread.
     */
    public static <T> void executarTask(Task<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailed) {
        log.debug("{} [TELEMETRIA] Iniciando execução de Task: {}", LOG_PREFIX, task.getClass().getSimpleName());

        task.setOnSucceeded(e -> {
            log.trace("{} [TELEMETRIA] Task finalizada com sucesso. Disparando callback.", LOG_PREFIX);
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("{} [SISTEMA] Falha na execução da Task: {}", LOG_PREFIX,
                    ex != null ? ex.getMessage() : "Erro desconhecido");
            if (onFailed != null) {
                onFailed.accept(ex);
            }
        });

        executor.submit(task);
    }

    /**
     * Método de conveniência para executar um Callable de forma assíncrona.
     * Cria a Task internamente, reduzindo o boilerplate nos Controllers.
     */
    public static <T> void executarTaskAsync(Callable<T> acao, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        log.debug("{} [TELEMETRIA] Criando Task assíncrona a partir de Callable.", LOG_PREFIX);

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return acao.call();
            }
        };

        executarTask(task, onSuccess, onError);
    }
}
