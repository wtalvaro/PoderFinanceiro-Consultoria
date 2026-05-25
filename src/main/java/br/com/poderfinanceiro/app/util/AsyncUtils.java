package br.com.poderfinanceiro.app.util;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AsyncUtils {

    private static final Logger log = LoggerFactory.getLogger(AsyncUtils.class);

    // 🚀 ATUALIZAÇÃO PARA VIRTUAL THREADS (Project Loom)
    // Não usamos mais pool de threads fixas ou cache.
    // Cada tarefa agora é executada em uma Virtual Thread dedicada,
    // oferecendo performance superior e custo de memória irrisório.
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    static {
        log.info("[ASYNC_UTILS] Inicializado com Virtual Thread Executor (Project Loom)");
    }

    /**
     * Executa uma Task do JavaFX já preconfigurada.
     */
    public static <T> void executarTask(Task<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailed) {
        log.debug("[ASYNC_UTILS] executarTask: Iniciando task (classe={})", task.getClass().getSimpleName());
        task.setOnSucceeded(e -> {
            log.debug("[ASYNC_UTILS] Task finalizada com sucesso, chamando callback onSuccess");
            if (onSuccess != null)
                onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("[ASYNC_UTILS] Task falhou: {}", ex != null ? ex.getMessage() : "exceção desconhecida", ex);
            if (onFailed != null)
                onFailed.accept(ex);
        });
        executor.submit(task);
    }

    /**
     * Novo método de conveniência (Boilerplate-free).
     * Cria a Task internamente usando um Callable.
     */
    public static <T> void executarTaskAsync(Callable<T> acao, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        log.debug("[ASYNC_UTILS] executarTaskAsync: Criando nova task a partir de Callable");
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                log.trace("[ASYNC_UTILS] Task.call() iniciando execução");
                return acao.call();
            }
        };
        executarTask(task, onSuccess, onError);
    }
}