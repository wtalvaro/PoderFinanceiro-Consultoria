package br.com.poderfinanceiro.app.util;

import javafx.concurrent.Task;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AsyncUtils {

    // 🚀 ATUALIZAÇÃO PARA VIRTUAL THREADS (Project Loom)
    // Não usamos mais pool de threads fixas ou cache.
    // Cada tarefa agora é executada em uma Virtual Thread dedicada,
    // oferecendo performance superior e custo de memória irrisório.
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Executa uma Task do JavaFX já preconfigurada.
     */
    public static <T> void executarTask(Task<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailed) {
        task.setOnSucceeded(e -> {
            if (onSuccess != null)
                onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            if (onFailed != null)
                onFailed.accept(task.getException());
        });
        executor.submit(task);
    }

    /**
     * Novo método de conveniência (Boilerplate-free).
     * Cria a Task internamente usando um Callable.
     */
    public static <T> void executarTaskAsync(Callable<T> acao, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return acao.call();
            }
        };
        executarTask(task, onSuccess, onError);
    }
}