package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.model.enums.CategoriaLinkModel;
import br.com.poderfinanceiro.app.domain.service.LinkUtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para LinkUtilFacadeImpl.
 * Valida a gestão de links úteis e o motor de busca textual.
 */
class LinkUtilFacadeTest {

    private LinkUtilFacadeImpl facade;
    private LinkUtilService linkUtilService;

    @BeforeEach
    void setUp() {
        linkUtilService = mock(LinkUtilService.class);
        facade = new LinkUtilFacadeImpl(linkUtilService);
    }

    @Test
    @DisplayName("Deve listar todos os links delegando ao serviço de domínio")
    void deveListarTodosOsLinks() {
        // GIVEN
        when(linkUtilService.listarTodos()).thenReturn(List.of(new LinkUtilModel()));

        // WHEN
        List<LinkUtilModel> resultado = facade.listarTodosOsLinks();

        // THEN
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(linkUtilService, times(1)).listarTodos();
    }

    @Test
    @DisplayName("Deve salvar um link com sucesso quando os dados obrigatórios estiverem presentes")
    void deveSalvarLinkComSucesso() {
        // GIVEN
        LinkUtilModel link = new LinkUtilModel();
        link.setTitulo("Portal do Consultor");
        link.setUrl("https://portal.exemplo.com");

        when(linkUtilService.salvar(any(LinkUtilModel.class))).thenAnswer(invocation -> {
            LinkUtilModel l = invocation.getArgument(0);
            l.setId(1L);
            return l;
        });

        // WHEN
        LinkUtilModel salvo = facade.salvarLink(link);

        // THEN
        assertNotNull(salvo.getId());
        assertEquals("Portal do Consultor", salvo.getTitulo());
        verify(linkUtilService, times(1)).salvar(link);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar link sem título ou URL")
    void deveFalharAoSalvarLinkInvalido() {
        // GIVEN
        LinkUtilModel linkSemTitulo = new LinkUtilModel();
        linkSemTitulo.setUrl("https://url.com");

        LinkUtilModel linkSemUrl = new LinkUtilModel();
        linkSemUrl.setTitulo("Titulo");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> facade.salvarLink(linkSemTitulo));
        assertThrows(IllegalArgumentException.class, () -> facade.salvarLink(linkSemUrl));
        verifyNoInteractions(linkUtilService);
    }

    @Test
    @DisplayName("Deve filtrar links por título, descrição, categoria ou tags")
    void deveFiltrarLinksCorretamente() {
        // GIVEN
        LinkUtilModel l1 = new LinkUtilModel();
        l1.setTitulo("Calculadora INSS");
        l1.setTags("previdencia, calculo");
        // CORREÇÃO: Usando constante real CONSULTA ("Consultas (CPF/FGTS)")
        l1.setCategoria(CategoriaLinkModel.CONSULTA);

        LinkUtilModel l2 = new LinkUtilModel();
        l2.setTitulo("Portal Gov.br");
        l2.setDescricao("Acesso ao sistema do governo");
        // CORREÇÃO: Usando constante real GOVERNO ("Portais Governamentais")
        l2.setCategoria(CategoriaLinkModel.GOVERNO);

        when(linkUtilService.listarTodos()).thenReturn(List.of(l1, l2));

        // WHEN & THEN
        // 1. Busca por título
        assertEquals(1, facade.filtrarLinks("inss").size());

        // 2. Busca por tag
        assertEquals(1, facade.filtrarLinks("previdencia").size());

        // 3. Busca por descrição
        assertEquals(1, facade.filtrarLinks("governo").size());

        // 4. Busca pelo Label da Categoria (Testando a integração com getLabel())
        assertEquals(1, facade.filtrarLinks("governamentais").size(), "Deve achar pelo label da categoria");

        // 5. Busca vazia
        assertEquals(2, facade.filtrarLinks("").size());
    }

    @Test
    @DisplayName("Deve delegar a exclusão de link para o serviço")
    void deveExcluirLink() {
        // WHEN
        facade.excluirLink(10L);

        // THEN
        verify(linkUtilService, times(1)).excluir(10L);
    }
}
