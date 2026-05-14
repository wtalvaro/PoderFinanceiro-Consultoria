package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.UsuarioModel;
import br.com.poderfinanceiro.app.repository.UsuarioRepository;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder; // Injetado via Spring Security

    @Getter
    private UsuarioModel usuarioLogado; // Centraliza a sessão global do programa

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean login(String username, String senha) {
        // Busca pelo nome de usuário agora
        Optional<UsuarioModel> usuarioOpt = usuarioRepository.findByUsernameAndAtivoTrue(username.toLowerCase());

        if (usuarioOpt.isPresent()) {
            UsuarioModel u = usuarioOpt.get();
            if (passwordEncoder.matches(senha, u.getSenhaHash())) {
                u.setUltimoAcesso(LocalDateTime.now());
                usuarioRepository.save(u);
                this.usuarioLogado = u;
                return true;
            }
        }
        return false;
    }

    /**
     * Cria um novo consultor, aplica BCrypt e já o define como usuário logado na
     * sessão.
     */
    @Transactional
    public UsuarioModel cadastrar(String nome, String username, String email, String senha) {
        // 1. Verificação de unicidade de Username e E-mail
        if (usuarioRepository.findByUsernameAndAtivoTrue(username.toLowerCase()).isPresent()) {
            throw new RuntimeException("Este nome de usuário já está sendo usado.");
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso no Poder Financeiro.");
        }

        // 2. Mapeamento
        UsuarioModel novo = new UsuarioModel();
        novo.setNome(nome);
        novo.setUsername(username.toLowerCase().trim()); // Normalização para o login
        novo.setEmail(email.toLowerCase().trim());
        novo.setSenhaHash(passwordEncoder.encode(senha));
        novo.setPapel("CONSULTOR");
        novo.setAtivo(true);

        UsuarioModel salvo = usuarioRepository.save(novo);
        this.usuarioLogado = salvo;

        return salvo;
    }

    public void logout() {
        this.usuarioLogado = null;
    }

    public boolean estaLogado() {
        return this.usuarioLogado != null;
    }
}