package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.service.BancoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para BancoFacadeImpl.
 * Valida a gestão de instituições bancárias e utilitários de interface.
 */
class BancoFacadeTest {

    private BancoFacadeImpl facade;
    private BancoService bancoService;

    @BeforeEach
    void setUp() {
        bancoService = mock(BancoService.class);
        facade = new BancoFacadeImpl(bancoService);
    }

    @Test
    @DisplayName("Deve salvar um banco com sucesso quando os dados forem válidos")
    void deveSalvarBancoComSucesso() {
        // GIVEN
        BancoModel banco = new BancoModel();
        banco.setNome("Banco do Brasil");
        banco.setCodigo("001");

        when(bancoService.salvar(any(BancoModel.class))).thenAnswer(i -> {
            BancoModel b = i.getArgument(0);
            b.setId(1L);
            return b;
        });

        // WHEN
        BancoModel salvo = facade.salvarBanco(banco);

        // THEN
        assertNotNull(salvo.getId());
        assertEquals("Banco do Brasil", salvo.getNome());
        verify(bancoService, times(1)).salvar(banco);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar banco sem nome")
    void deveFalharAoSalvarBancoSemNome() {
        // GIVEN
        BancoModel bancoInvalido = new BancoModel();
        bancoInvalido.setNome("");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> facade.salvarBanco(bancoInvalido));
        verifyNoInteractions(bancoService);
    }

    @Test
    @DisplayName("Deve filtrar bancos por nome ou código corretamente")
    void deveFiltrarBancos() {
        // GIVEN
        BancoModel b1 = new BancoModel();
        b1.setNome("Itaú");
        b1.setCodigo("341");
        BancoModel b2 = new BancoModel();
        b2.setNome("Santander");
        b2.setCodigo("033");

        when(bancoService.listarTodos()).thenReturn(List.of(b1, b2));

        // WHEN
        List<BancoModel> resultadoNome = facade.filtrarBancos("ita");
        List<BancoModel> resultadoCodigo = facade.filtrarBancos("033");

        // THEN
        assertEquals(1, resultadoNome.size());
        assertEquals("Itaú", resultadoNome.get(0).getNome());
        assertEquals(1, resultadoCodigo.size());
        assertEquals("Santander", resultadoCodigo.get(0).getNome());
    }

    @Test
    @DisplayName("Deve bloquear exclusão se o banco possuir tabelas vinculadas")
    void deveVerificarBloqueioDeExclusao() {
        // GIVEN
        BancoModel bancoComTabelas = new BancoModel();
        bancoComTabelas.setTabelas(List.of(new br.com.poderfinanceiro.app.domain.model.TabelaJurosModel()));

        BancoModel bancoVazio = new BancoModel();
        bancoVazio.setTabelas(new ArrayList<>());

        // WHEN & THEN
        assertTrue(facade.isExclusaoBloqueada(bancoComTabelas), "Deve bloquear se houver tabelas.");
        assertFalse(facade.isExclusaoBloqueada(bancoVazio), "Não deve bloquear se estiver vazio.");
    }

    @Test
    @DisplayName("Deve formatar URL do portal adicionando HTTPS se necessário")
    void deveFormatarUrlPortal() {
        assertEquals("https://portal.banco.com", facade.formatarUrlPortal("portal.banco.com"));
        assertEquals("https://portal.banco.com", facade.formatarUrlPortal("https://portal.banco.com"));
        assertEquals("http://portal.banco.com", facade.formatarUrlPortal("http://portal.banco.com"));
        assertNull(facade.formatarUrlPortal("   "));
    }

    @Test
    @DisplayName("Deve formatar link do WhatsApp com prefixo internacional")
    void deveFormatarLinkWhatsApp() {
        // GIVEN
        String telefone = "11999998888";

        // WHEN
        String link = facade.formatarLinkWhatsApp(telefone);

        // THEN
        assertEquals("https://wa.me/5511999998888", link);
    }

    @Test
    @DisplayName("Deve delegar exclusão para o serviço")
    void deveExcluirBanco() {
        // WHEN
        facade.excluirBanco(10L);

        // THEN
        verify(bancoService, times(1)).excluir(10L);
    }
}
