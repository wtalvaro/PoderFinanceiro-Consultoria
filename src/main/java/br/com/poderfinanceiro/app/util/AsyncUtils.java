package br.com.poderfinanceiro.app.util;
import javafx.concurrent.Task;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AsyncUtils {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

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
}