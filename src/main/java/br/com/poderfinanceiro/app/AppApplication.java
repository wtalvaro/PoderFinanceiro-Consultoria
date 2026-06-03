package br.com.poderfinanceiro.app;

import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <h1>AppApplication</h1>
 * <p>
 * Ponto de entrada principal (Entry Point) do ERP Poder Financeiro.
 * Esta classe orquestra o bootstrap do Spring Boot e delega o ciclo de vida
 * da interface gráfica para a {@link JavafxApplication}.
 * </p>
 */
@SpringBootApplication
public class AppApplication {

	private static final Logger log = LoggerFactory.getLogger(AppApplication.class);
	private static final String LOG_PREFIX = "[AppApplication]";

	/**
	 * Método principal que inicia a execução da JVM.
	 * 
	 * @param args Argumentos de linha de comando.
	 */
	public static void main(String[] args) {
		log.info("{} [SISTEMA] Iniciando sequência de boot do ERP Poder Financeiro.", LOG_PREFIX);

		try {
			// Configuração de compatibilidade para ambientes Linux (Fedora/GNOME)
			log.debug("{} [SISTEMA] Configurando variáveis de ambiente para o Toolkit gráfico.", LOG_PREFIX);
			System.setProperty("XDG_CURRENT_DESKTOP", "GNOME");

			log.info("{} [TELEMETRIA] Delegando inicialização para o ciclo de vida JavaFX.", LOG_PREFIX);

			// Lançamento da aplicação JavaFX integrada ao Spring
			Application.launch(JavafxApplication.class, args);

		} catch (Exception e) {
			log.error("{} [SISTEMA] Falha crítica detectada durante a inicialização: {}", LOG_PREFIX, e.getMessage(),
					e);
			// Garante o encerramento do processo em caso de erro fatal no Toolkit
			System.exit(1);
		}

		log.info("{} [SISTEMA] Processo de aplicação encerrado.", LOG_PREFIX);
	}
}
