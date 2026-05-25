package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.UsuarioRepository;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Getter
    private UsuarioModel usuarioLogado;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        log.debug("[AUTH_SERVICE] Construtor: Serviço de autenticação instanciado");
    }

    @Transactional
    public boolean login(String username, String senha) {
        log.debug("[AUTH_SERVICE] login: Tentativa de login para username='{}'", username);
        Optional<UsuarioModel> usuarioOpt = usuarioRepository.findByUsernameAndAtivoTrue(username.toLowerCase());

        if (usuarioOpt.isPresent()) {
            UsuarioModel u = usuarioOpt.get();
            if (passwordEncoder.matches(senha, u.getSenhaHash())) {
                u.setUltimoAcesso(LocalDateTime.now());
                usuarioRepository.save(u);
                this.usuarioLogado = u;
                log.info("[AUTH_SERVICE] login: Usuário '{}' autenticado com sucesso (ID={})", username, u.getId());
                return true;
            } else {
                log.warn("[AUTH_SERVICE] login: Falha na autenticação - senha inválida para username='{}'", username);
            }
        } else {
            log.warn("[AUTH_SERVICE] login: Usuário '{}' não encontrado ou inativo", username);
        }
        return false;
    }

    @Transactional
    public UsuarioModel cadastrar(String nome, String username, String email, String senha, String geminiApiKey) {
        log.debug("[AUTH_SERVICE] cadastrar: Iniciando cadastro para username='{}', email='{}'", username, email);

        if (usuarioRepository.findByUsernameAndAtivoTrue(username.toLowerCase()).isPresent()) {
            log.warn("[AUTH_SERVICE] cadastrar: Nome de usuário '{}' já está em uso", username);
            throw new RuntimeException("Este nome de usuário já está sendo usado.");
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            log.warn("[AUTH_SERVICE] cadastrar: E-mail '{}' já está em uso", email);
            throw new RuntimeException("Este e-mail já está em uso no Poder Financeiro.");
        }

        UsuarioModel novo = new UsuarioModel();
        novo.setNome(nome);
        novo.setUsername(username.toLowerCase().trim());
        novo.setEmail(email.toLowerCase().trim());
        novo.setSenhaHash(passwordEncoder.encode(senha));
        novo.setGeminiApiKey(geminiApiKey);
        novo.setPapel("CONSULTOR");
        novo.setAtivo(true);

        UsuarioModel salvo = usuarioRepository.save(novo);
        this.usuarioLogado = salvo;
        log.info("[AUTH_SERVICE] cadastrar: Novo usuário criado com sucesso - ID={}, username='{}'", salvo.getId(),
                salvo.getUsername());
        return salvo;
    }

    @org.springframework.transaction.annotation.Transactional
    public void atualizarGeminiApiKey(String novaChave) {
        log.debug("[AUTH_SERVICE] atualizarGeminiApiKey: Atualizando chave API Gemini");
        if (usuarioLogado == null) {
            log.error("[AUTH_SERVICE] atualizarGeminiApiKey: Tentativa de atualização sem sessão ativa");
            throw new RuntimeException("Sessão inválida ou expirada. Efetue o login novamente.");
        }

        usuarioLogado.setGeminiApiKey(novaChave);
        usuarioRepository.save(usuarioLogado);
        log.info("[AUTH_SERVICE] atualizarGeminiApiKey: Chave API atualizada para o usuário ID={}",
                usuarioLogado.getId());
    }

    public void logout() {
        if (usuarioLogado != null) {
            log.info("[AUTH_SERVICE] logout: Usuário '{}' (ID={}) realizou logout", usuarioLogado.getUsername(),
                    usuarioLogado.getId());
        } else {
            log.debug("[AUTH_SERVICE] logout: Chamado sem usuário logado (nada a fazer)");
        }
        this.usuarioLogado = null;
    }

    public boolean estaLogado() {
        boolean logado = this.usuarioLogado != null;
        log.trace("[AUTH_SERVICE] estaLogado: {}", logado);
        return logado;
    }
}