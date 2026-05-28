package br.com.poderfinanceiro.app.controller;

import br.com.poderfinanceiro.app.facade.IAuthFacade;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <h1>StatusBarController</h1>
 * <p>
 * Controlador de Interface (UI) responsável pela barra de status inferior. Atua
 * como um <b>Humble Object</b>, delegando a consulta de sessão para a
 * {@link IAuthFacade}.
 * </p>
 */
@Component
public class StatusBarController {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(StatusBarController.class);
    private static final String LOG_PREFIX = "[StatusBarController]";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS (DIP)
    // ==========================================================================================
    private final IAuthFacade authFacade;

    @Value("${app.version:v1.0.0}") private String appVersion;

    // ==========================================================================================
    // MÓDULO 3: COMPONENTES VISUAIS (FXML)
    // ==========================================================================================
    @FXML private Label lblUsuario;
    @FXML private Label lblVersao;

    public StatusBarController(IAuthFacade authFacade) {
        this.authFacade = authFacade;
        log.debug("{} [SISTEMA] Controlador instanciado via Spring.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 4: INICIALIZAÇÃO E CICLO DE VIDA
    // ==========================================================================================
    @FXML public void initialize() {
        log.info("{} [TELEMETRIA] Inicializando barra de status...", LOG_PREFIX);

        atualizarStatusUsuario();

        if (lblVersao != null) {
            lblVersao.setText("Poder Financeiro " + appVersion);
            log.info("{} [SISTEMA] Versão da aplicação exibida: {}", LOG_PREFIX, appVersion);
        } else {
            log.warn("{} [UI] Label de versão (lblVersao) não encontrado no FXML.", LOG_PREFIX);
        }

        log.debug("{} [LIFECYCLE] Inicialização concluída.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 5: ATUALIZAÇÃO DE ESTADO
    // ==========================================================================================
    /**
     * Atualiza o texto da barra de status com o nome do consultor logado. Deve
     * ser chamado sempre que houver uma mudança de sessão (ex: após o login).
     */
    public void atualizarStatusUsuario() {
        log.trace("{} [UI] Verificando estado da autenticação para atualizar a barra.", LOG_PREFIX);

        if (lblUsuario == null) {
            log.error("{} [SISTEMA] Label de usuário (lblUsuario) está nulo. Impossível atualizar.", LOG_PREFIX);
            return;
        }

        if (authFacade.isUsuarioLogado()) {
            String nomeConsultor = authFacade.getUsuarioLogado().getNome();
            lblUsuario.setText("👤 Consultor: " + nomeConsultor);
            log.info("{} [TELEMETRIA] Barra de status atualizada para o usuário: '{}'", LOG_PREFIX, nomeConsultor);
        } else {
            lblUsuario.setText("👤 Usuário não identificado");
            log.warn("{} [NEGOCIO] Nenhum usuário logado. Barra de status exibindo estado anônimo.", LOG_PREFIX);
        }
    }
}
