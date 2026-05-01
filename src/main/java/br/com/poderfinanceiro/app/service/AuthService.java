package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.Usuario;
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
    private Usuario usuarioLogado; // Centraliza a sessão global do programa

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Tenta autenticar o consultor comparando o hash BCrypt e registra o acesso.
     */
    @Transactional
    public boolean login(String email, String senha) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndAtivoTrue(email);

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();

            // O BCrypt.matches descriptografa o salt do hash e valida a senha com segurança
            if (passwordEncoder.matches(senha, u.getSenhaHash())) {
                u.setUltimoAcesso(LocalDateTime.now());
                usuarioRepository.save(u); // Atualiza o timestamp no Postgres[cite: 1]
                this.usuarioLogado = u;
                return true;
            }
        }
        return false;
    }

    /**
     * Cria um novo consultor aplicando o Hash BCrypt na senha antes de salvar.[cite: 1]
     */
    @Transactional
    public Usuario cadastrar(String nome, String email, String senha) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso no Poder Financeiro.");
        }

        Usuario novo = new Usuario();
        novo.setNome(nome);
        novo.setEmail(email);
        
        // Aplica o algoritmo de Hash BCrypt com Salting automático
        novo.setSenhaHash(passwordEncoder.encode(senha)); 
        
        novo.setPapel("CONSULTOR"); // Padrão conforme seu script SQL[cite: 1]
        novo.setAtivo(true);

        return usuarioRepository.save(novo);
    }

    public void logout() {
        this.usuarioLogado = null;
    }

    public boolean estaLogado() {
        return this.usuarioLogado != null;
    }
}