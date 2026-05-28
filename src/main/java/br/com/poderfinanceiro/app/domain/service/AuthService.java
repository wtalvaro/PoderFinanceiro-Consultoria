package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.infrastructure.repository.UsuarioRepository;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Serviço de Autenticação e Gestão de Identidade. Responsável pelo ciclo de
 * vida da sessão do consultor e segurança de credenciais.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String LOG_PREFIX = "[AuthService]";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Getter private UsuarioModel usuarioLogado;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        log.info("{} [SISTEMA] Serviço de autenticação inicializado com PasswordEncoder BCrypt.", LOG_PREFIX);
    }

    /**
     * Realiza a autenticação do consultor.
     * 
     * @param username Nome de usuário
     * @param senha Senha em texto plano
     * @return boolean True se autenticado com sucesso
     */
    @Transactional public boolean login(String username, String senha) {
        log.info("{} [TELEMETRIA] Tentativa de login iniciada para o usuário: {}", LOG_PREFIX, username);

        if (username == null || senha == null || username.isBlank() || senha.isBlank()) {
            log.warn("{} [NEGOCIO] Falha no login: Credenciais nulas ou vazias.", LOG_PREFIX);
            return false;
        }

        String usernameNormalizado = username.toLowerCase().trim();
        Optional<UsuarioModel> usuarioOpt = usuarioRepository.findByUsernameAndAtivoTrue(usernameNormalizado);

        if (usuarioOpt.isPresent()) {
            UsuarioModel usuario = usuarioOpt.get();

            if (passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                // Atualiza rastro de acesso
                usuario.setUltimoAcesso(LocalDateTime.now());
                usuarioRepository.save(usuario);

                this.usuarioLogado = usuario;

                log.info("{} [AUDITORIA] Usuário '{}' autenticado com sucesso. ID: {}", LOG_PREFIX, usernameNormalizado,
                        usuario.getId());
                return true;
            } else {
                log.warn("{} [AUDITORIA] Falha de login: Senha incorreta para o usuário '{}'.", LOG_PREFIX,
                        usernameNormalizado);
            }
        } else {
            log.warn("{} [NEGOCIO] Falha de login: Usuário '{}' não encontrado ou inativo.", LOG_PREFIX,
                    usernameNormalizado);
        }

        return false;
    }

    /**
     * Registra um novo consultor no sistema.
     * 
     * @param nome Nome completo
     * @param username Username único
     * @param email E-mail único
     * @param senha Senha
     * @param geminiApiKey Chave de API do Google Gemini
     * @return UsuarioModel persistido
     */
    @Transactional public UsuarioModel cadastrar(String nome, String username, String email, String senha,
            String geminiApiKey) {
        log.info("{} [TELEMETRIA] Iniciando processo de cadastro de novo consultor: {}", LOG_PREFIX, username);

        // 1. Normalização e Validação
        String usernameNormalizado = username.toLowerCase().trim();
        String emailNormalizado = email.toLowerCase().trim();

        if (usuarioRepository.findByUsernameAndAtivoTrue(usernameNormalizado).isPresent()) {
            log.warn("{} [NEGOCIO] Cadastro negado: Username '{}' já existe.", LOG_PREFIX, usernameNormalizado);
            throw new RuntimeException("Este nome de usuário já está sendo usado.");
        }

        if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
            log.warn("{} [NEGOCIO] Cadastro negado: E-mail '{}' já cadastrado.", LOG_PREFIX, emailNormalizado);
            throw new RuntimeException("Este e-mail já está em uso no Poder Financeiro.");
        }

        try {
            // 2. Construção do Modelo
            UsuarioModel novo = new UsuarioModel();
            novo.setNome(nome.trim());
            novo.setUsername(usernameNormalizado);
            novo.setEmail(emailNormalizado);
            novo.setSenhaHash(passwordEncoder.encode(senha));
            novo.setGeminiApiKey(geminiApiKey != null ? geminiApiKey.trim() : null);
            novo.setPapel("CONSULTOR");
            novo.setAtivo(true);
            novo.setUltimoAcesso(LocalDateTime.now());

            // 3. Persistência
            UsuarioModel salvo = usuarioRepository.save(novo);
            this.usuarioLogado = salvo; // Login automático após cadastro

            log.info("{} [AUDITORIA] Novo consultor cadastrado e logado. ID: {}, Username: {}", LOG_PREFIX,
                    salvo.getId(), salvo.getUsername());

            return salvo;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro fatal ao cadastrar usuário: {}", LOG_PREFIX, e.getMessage());
            throw new RuntimeException("Falha técnica ao realizar cadastro. Tente novamente.");
        }
    }

    /**
     * Atualiza a chave de API do Gemini para o usuário atualmente logado.
     * 
     * @param novaChave Nova chave de API
     */
    @Transactional public void atualizarGeminiApiKey(String novaChave) {
        log.info("{} [TELEMETRIA] Solicitada atualização de Gemini API Key.", LOG_PREFIX);

        if (this.usuarioLogado == null) {
            log.error("{} [NEGOCIO] Tentativa de atualizar chave sem sessão ativa.", LOG_PREFIX);
            throw new IllegalStateException("Sessão inválida ou expirada. Efetue o login novamente.");
        }

        try {
            this.usuarioLogado.setGeminiApiKey(novaChave != null ? novaChave.trim() : null);
            usuarioRepository.save(this.usuarioLogado);

            log.info("{} [AUDITORIA] Chave API Gemini atualizada para o usuário ID: {}", LOG_PREFIX,
                    usuarioLogado.getId());
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao persistir nova chave API: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    /**
     * Encerra a sessão do usuário atual.
     */
    public void logout() {
        if (this.usuarioLogado != null) {
            log.info("{} [AUDITORIA] Logout realizado para o usuário: {}", LOG_PREFIX, usuarioLogado.getUsername());
            this.usuarioLogado = null;
        } else {
            log.debug("{} [NEGOCIO] Logout chamado sem usuário logado.", LOG_PREFIX);
        }
    }

    /**
     * Verifica se existe uma sessão ativa.
     * 
     * @return boolean
     */
    public boolean estaLogado() {
        boolean status = this.usuarioLogado != null;
        log.trace("{} [TELEMETRIA] Verificação de status de login: {}", LOG_PREFIX, status);
        return status;
    }
}
