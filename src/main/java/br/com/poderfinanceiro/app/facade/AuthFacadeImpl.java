package br.com.poderfinanceiro.app.facade;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthFacadeImpl implements IAuthFacade {

    private static final Logger log = LoggerFactory.getLogger(AuthFacadeImpl.class);
    private static final String LOG_PREFIX = "[AuthFacade]";

    private final AuthService authService;

    public AuthFacadeImpl(AuthService authService) {
        this.authService = authService;
        log.debug("{} [SISTEMA] Facade de Autenticação instanciada.", LOG_PREFIX);
    }

    @Override public boolean realizarLogin(String username, String senha) {
        log.info("{} [TELEMETRIA] Solicitando login para o usuário: {}", LOG_PREFIX, username);
        return authService.login(username, senha);
    }

    @Override public UsuarioModel cadastrarUsuario(String nome, String username, String email, String senha, String geminiApiKey) {
        log.info("{} [TELEMETRIA] Iniciando processo de cadastro para o username: {}", LOG_PREFIX, username);

        // A validação de negócio (se o usuário já existe) já é feita dentro do
        // AuthService,
        // então a Facade apenas repassa a chamada e loga o resultado.
        UsuarioModel novoUsuario = authService.cadastrar(nome, username, email, senha, geminiApiKey);

        log.info("{} [AUDITORIA] Usuário cadastrado com sucesso. ID: {}", LOG_PREFIX, novoUsuario.getId());
        return novoUsuario;
    }

    @Override public void realizarLogout() {
        log.info("{} [TELEMETRIA] Solicitando encerramento de sessão.", LOG_PREFIX);
        authService.logout();
    }

    @Override public boolean isUsuarioLogado() {
        return authService.estaLogado();
    }

    @Override public UsuarioModel getUsuarioLogado() {
        return authService.getUsuarioLogado();
    }
}
