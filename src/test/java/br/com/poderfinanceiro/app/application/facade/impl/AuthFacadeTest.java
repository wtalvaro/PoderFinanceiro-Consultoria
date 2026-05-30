package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para AuthFacadeImpl.
 * Valida a orquestração de autenticação e cadastro de usuários.
 */
class AuthFacadeTest {

    private AuthFacadeImpl facade;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        facade = new AuthFacadeImpl(authService);
    }

    @Test
    @DisplayName("Deve realizar login com sucesso quando as credenciais forem válidas")
    void deveRealizarLoginComSucesso() {
        // GIVEN
        String user = "admin";
        String pass = "123456";
        when(authService.login(user, pass)).thenReturn(true);

        // WHEN
        boolean resultado = facade.realizarLogin(user, pass);

        // THEN
        assertTrue(resultado, "O login deveria retornar verdadeiro.");
        verify(authService, times(1)).login(user, pass);
    }

    @Test
    @DisplayName("Deve falhar no login quando as credenciais forem inválidas")
    void deveFalharNoLogin() {
        // GIVEN
        when(authService.login(anyString(), anyString())).thenReturn(false);

        // WHEN
        boolean resultado = facade.realizarLogin("usuario_errado", "senha_errada");

        // THEN
        assertFalse(resultado, "O login deveria retornar falso.");
    }

    @Test
    @DisplayName("Deve cadastrar um novo usuário e retornar o modelo com ID")
    void deveCadastrarUsuarioComSucesso() {
        // GIVEN
        UsuarioModel mockUsuario = new UsuarioModel();
        mockUsuario.setId(1L);
        mockUsuario.setUsername("consultor1");

        when(authService.cadastrar(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockUsuario);

        // WHEN
        UsuarioModel resultado = facade.cadastrarUsuario(
                "Consultor Teste", "consultor1", "teste@poder.com", "senha123", "api-key-ia");

        // THEN
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("consultor1", resultado.getUsername());
        verify(authService, times(1)).cadastrar(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve realizar logout delegando para o serviço")
    void deveRealizarLogout() {
        // WHEN
        facade.realizarLogout();

        // THEN
        verify(authService, times(1)).logout();
    }

    @Test
    @DisplayName("Deve verificar se o usuário está logado")
    void deveVerificarStatusLogado() {
        // GIVEN
        when(authService.estaLogado()).thenReturn(true);

        // WHEN & THEN
        assertTrue(facade.isUsuarioLogado());
        verify(authService, times(1)).estaLogado();
    }

    @Test
    @DisplayName("Deve retornar o usuário logado atualmente")
    void deveRetornarUsuarioLogado() {
        // GIVEN
        UsuarioModel logado = new UsuarioModel();
        logado.setUsername("logado_agora");
        when(authService.getUsuarioLogado()).thenReturn(logado);

        // WHEN
        UsuarioModel resultado = facade.getUsuarioLogado();

        // THEN
        assertNotNull(resultado);
        assertEquals("logado_agora", resultado.getUsername());
    }
}
