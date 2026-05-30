package br.com.poderfinanceiro.app.common.util;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Common] Teste de Unidade - AsyncUtils")
class AsyncUtilsTest {

    @BeforeAll
    static void initJFX() {
        try {
            // Inicializa o Toolkit do JavaFX de forma nativa (sem Swing)
            // O try-catch evita erro se o toolkit já estiver rodando
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // Toolkit já inicializado, podemos prosseguir
        }
    }

    @Test
    @DisplayName("Deve executar tarefa assíncrona e retornar sucesso")
    void deveExecutarComSucesso() throws InterruptedException {
        // GIVEN
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resultado = new AtomicReference<>();

        // WHEN
        AsyncUtils.executarTaskAsync(() -> {
            // Simulando uma operação em Virtual Thread
            return "Sucesso Loom";
        }, sucesso -> {
            resultado.set(sucesso);
            latch.countDown();
        }, erro -> latch.countDown());

        // THEN
        boolean finalizou = latch.await(5, TimeUnit.SECONDS);
        assertThat(finalizou).isTrue();
        assertThat(resultado.get()).isEqualTo("Sucesso Loom");
    }

    @Test
    @DisplayName("Deve capturar erro em tarefa assíncrona e disparar callback de erro")
    void deveCapturarErro() throws InterruptedException {
        // GIVEN
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> erroCapturado = new AtomicReference<>();

        // WHEN
        AsyncUtils.executarTaskAsync(() -> {
            throw new RuntimeException("Falha na IA");
        }, sucesso -> latch.countDown(),
                erro -> {
                    erroCapturado.set(erro);
                    latch.countDown();
                });

        // THEN
        latch.await(5, TimeUnit.SECONDS);
        assertThat(erroCapturado.get()).isNotNull();
        assertThat(erroCapturado.get().getMessage()).isEqualTo("Falha na IA");
    }

    @Test
    @DisplayName("Deve garantir que a tarefa está rodando em uma Virtual Thread")
    void deveRodarEmVirtualThread() throws InterruptedException {
        // GIVEN
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> isVirtual = new AtomicReference<>(false);

        // WHEN
        AsyncUtils.executarTaskAsync(() -> {
            isVirtual.set(Thread.currentThread().isVirtual());
            return null;
        }, sucesso -> latch.countDown(), erro -> latch.countDown());

        // THEN
        latch.await(5, TimeUnit.SECONDS);
        assertThat(isVirtual.get()).isTrue();
    }

    @Test
    @DisplayName("Deve garantir que o callback de sucesso roda na JavaFX Thread")
    void deveRodarCallbackNaUIThread() throws InterruptedException {
        // GIVEN
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> isUIThread = new AtomicReference<>(false);

        // WHEN
        AsyncUtils.executarTaskAsync(() -> "OK",
                sucesso -> {
                    isUIThread.set(Platform.isFxApplicationThread());
                    latch.countDown();
                },
                erro -> latch.countDown());

        // THEN
        latch.await(5, TimeUnit.SECONDS);
        assertThat(isUIThread.get()).isTrue();
    }
}
