package br.com.poderfinanceiro.app.presentation.event;

import br.com.poderfinanceiro.app.presentation.ui.navigation.AppRoute;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <h1>UIEvent</h1>
 * <p>
 * Eventos para comunicação desacoplada entre controladores.
 * </p>
 */
@Getter
public abstract class UIEvent extends ApplicationEvent {
    public UIEvent(Object source) {
        super(source);
    }

    @Getter
    public static class Loading extends UIEvent {
        private final String mensagem;
        private final boolean visivel;

        public Loading(Object source, String mensagem, boolean visivel) {
            super(source);
            this.mensagem = mensagem;
            this.visivel = visivel;
        }
    }

    @Getter
    public static class Notificacao extends UIEvent {
        public enum Tipo {
            SUCESSO, AVISO, ERRO
        }

        private final String titulo;
        private final String mensagem;
        private final Tipo tipo;

        public Notificacao(Object source, String titulo, String mensagem, Tipo tipo) {
            super(source);
            this.titulo = titulo;
            this.mensagem = mensagem;
            this.tipo = tipo;
        }
    }

    @Getter
    public static class Navegacao extends UIEvent {
        private final AppRoute rota;

        public Navegacao(Object source, AppRoute rota) {
            super(source);
            this.rota = rota;
        }
    }
}
