package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <h1>AuthServiceTest</h1>
 * <p>
 * Testes de Unidade para o serviço de Autenticação e Identidade.
 * Valida fluxos de login, cadastro, segurança de senhas e gestão de sessão.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioModel usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = new UsuarioModel();
        usuarioMock.setId(1L);
        usuarioMock.setUsername("consultor.teste");
        usuarioMock.setEmail("teste@poderfinanceiro.com.br");
        usuarioMock.setSenhaHash("hash_seguro");
        usuarioMock.setAtivo(true);
    }

    @Test
    @DisplayName("Deve autenticar usuário com sucesso quando credenciais estiverem corretas")
    void deveAutenticarComSucesso() {
        String username = "Consultor.Teste"; // Testando normalização (case insensitive)
        String senha = "senha123";

        when(usuarioRepository.findByUsernameAndAtivoTrue("consultor.teste")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches(senha, "hash_seguro")).thenReturn(true);

        boolean resultado = authService.login(username, senha);

        assertThat(resultado).isTrue();
        assertThat(authService.getUsuarioLogado()).isEqualTo(usuarioMock);
        assertThat(authService.estaLogado()).isTrue();

        // Verifica se o rastro de último acesso foi atualizado
        verify(usuarioRepository).save(usuarioMock);
        assertThat(usuarioMock.getUltimoAcesso()).isNotNull();
    }

    @Test
    @DisplayName("Deve falhar autenticação quando a senha estiver incorreta")
    void deveFalharComSenhaIncorreta() {
        when(usuarioRepository.findByUsernameAndAtivoTrue(anyString())).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        boolean resultado = authService.login("consultor.teste", "errada");

        assertThat(resultado).isFalse();
        assertThat(authService.getUsuarioLogado()).isNull();
        assertThat(authService.estaLogado()).isFalse();
    }

    @Test
    @DisplayName("Deve cadastrar novo usuário normalizando strings e criptografando senha")
    void deveCadastrarUsuarioComSucesso() {
        String nome = " Wagner Alvaro ";
        String username = " Wagner.Alvaro ";
        String email = " Wagner@Poder.com ";
        String senha = "minha_senha_secreta";

        when(usuarioRepository.findByUsernameAndAtivoTrue(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(senha)).thenReturn("hash_gerado");
        when(usuarioRepository.save(any(UsuarioModel.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioModel salvo = authService.cadastrar(nome, username, email, senha, "api_key_123");

        assertThat(salvo.getNome()).isEqualTo("Wagner Alvaro");
        assertThat(salvo.getUsername()).isEqualTo("wagner.alvaro");
        assertThat(salvo.getEmail()).isEqualTo("wagner@poder.com");
        assertThat(salvo.getSenhaHash()).isEqualTo("hash_gerado");
        assertThat(authService.getUsuarioLogado()).isEqualTo(salvo);
    }

    @Test
    @DisplayName("Deve impedir cadastro de usuário com username já existente")
    void deveImpedirUsernameDuplicado() {
        when(usuarioRepository.findByUsernameAndAtivoTrue(anyString())).thenReturn(Optional.of(usuarioMock));

        assertThatThrownBy(() -> authService.cadastrar("Nome", "consultor.teste", "outro@email.com", "123", "key"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nome de usuário já está sendo usado");
    }

    @Test
    @DisplayName("Deve atualizar a Gemini API Key apenas se houver usuário logado")
    void deveAtualizarApiKeyComSucesso() {
        // Simula login prévio
        deveAutenticarComSucesso();

        authService.atualizarGeminiApiKey("nova_chave_ia");

        assertThat(usuarioMock.getGeminiApiKey()).isEqualTo("nova_chave_ia");
        verify(usuarioRepository, times(2)).save(usuarioMock); // Uma no login, outra no update
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar API Key sem sessão ativa")
    void deveFalharAoAtualizarApiKeySemLogin() {
        assertThatThrownBy(() -> authService.atualizarGeminiApiKey("chave"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sessão inválida ou expirada");
    }

    @Test
    @DisplayName("Deve limpar a sessão ao realizar logout")
    void deveRealizarLogout() {
        deveAutenticarComSucesso();
        assertThat(authService.estaLogado()).isTrue();

        authService.logout();

        assertThat(authService.getUsuarioLogado()).isNull();
        assertThat(authService.estaLogado()).isFalse();
    }
}
