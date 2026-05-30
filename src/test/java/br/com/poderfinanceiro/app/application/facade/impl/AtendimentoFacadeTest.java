package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.common.util.SummaryGeneratorUtils;
import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService;
import br.com.poderfinanceiro.app.domain.service.ProponenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para AtendimentoFacadeImpl.
 * Sincronizado com a injeção de dependência do SummaryGeneratorUtils (v2.1.4).
 */
class AtendimentoFacadeTest {

    private AtendimentoFacadeImpl facade;
    private ProponenteService proponenteService;
    private AtendimentoContextService contextoService;
    private SummaryGeneratorUtils summaryUtils; // Novo mock necessário

    @BeforeEach
    void setUp() {
        proponenteService = mock(ProponenteService.class);
        contextoService = mock(AtendimentoContextService.class);
        summaryUtils = mock(SummaryGeneratorUtils.class); // Inicialização do mock

        // CORREÇÃO: Instanciação com os 3 parâmetros sincronizados
        facade = new AtendimentoFacadeImpl(proponenteService, contextoService, summaryUtils);
    }

    @Test
    @DisplayName("Deve vincular endereço ao proponente e salvar atendimento completo")
    void deveSalvarAtendimentoComSucesso() {
        // GIVEN
        ProponenteModel lead = new ProponenteModel();
        lead.setNomeCompleto("João Silva");

        EnderecoProponenteModel endereco = new EnderecoProponenteModel();
        endereco.setLogradouro("Rua das Flores");

        when(proponenteService.salvarProponente(any(ProponenteModel.class))).thenAnswer(invocation -> {
            ProponenteModel p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // WHEN
        ProponenteModel salvo = facade.salvarAtendimentoCompleto(lead, endereco);

        // THEN
        assertNotNull(salvo.getId());
        assertEquals(1, salvo.getEnderecos().size());
        assertEquals(salvo, salvo.getEnderecos().get(0).getProponente(),
                "O vínculo bidirecional deve ser estabelecido.");
        verify(proponenteService, times(1)).salvarProponente(lead);
    }

    @Test
    @DisplayName("Deve formatar link do WhatsApp corretamente")
    void deveFormatarLinkWhatsApp() {
        String link = facade.formatarLinkWhatsApp("11999998888");
        assertEquals("https://wa.me/5511999998888", link);
    }

    @Test
    @DisplayName("Deve delegar a geração de resumo para o SummaryUtils injetado")
    void deveGerarResumoParaCopia() {
        // GIVEN
        ProponenteModel lead = new ProponenteModel();
        when(summaryUtils.gerarJsonContextualParaIA(any(), anyBoolean())).thenReturn("{ \"json\": \"mock\" }");

        // WHEN
        String resumo = facade.gerarResumoParaCopia(lead, "5.000,00");

        // THEN
        assertNotNull(resumo);
        verify(summaryUtils, times(1)).gerarJsonContextualParaIA(lead, true);
    }

    @Test
    @DisplayName("Deve delegar a limpeza de contexto para o AtendimentoContextService")
    void deveLimparContexto() {
        facade.limparContextoAtendimento();
        verify(contextoService, times(1)).limparContexto();
    }

    @Test
    @DisplayName("Deve definir o lead ativo no contexto global")
    void deveDefinirLeadAtivo() {
        ProponenteModel lead = new ProponenteModel();
        lead.setId(100L);

        facade.definirLeadAtivo(lead);

        verify(contextoService, times(1)).setLeadAtivo(lead);
    }
}
