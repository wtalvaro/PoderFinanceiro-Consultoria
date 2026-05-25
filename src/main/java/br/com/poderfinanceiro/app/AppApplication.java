package br.com.poderfinanceiro.app;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class AppApplication {

	private static final Logger log = LoggerFactory.getLogger(AppApplication.class);

	public static void main(String[] args) {
		log.info("[APP_APPLICATION] Iniciando aplicação Poder Financeiro");
		log.debug("[APP_APPLICATION] Configurando propriedade XDG_CURRENT_DESKTOP=GNOME");
		System.setProperty("XDG_CURRENT_DESKTOP", "GNOME");
		log.debug("[APP_APPLICATION] Propriedade definida. Delegando inicialização para JavafxApplication");
		try {
			Application.launch(JavafxApplication.class, args);
		} catch (Exception e) {
			log.error("[APP_APPLICATION] Erro fatal durante execução do JavaFX: {}", e.getMessage(), e);
			throw e;
		}
		log.info("[APP_APPLICATION] Aplicação encerrada");
	}
}