package br.com.poderfinanceiro.app.application.facade.impl;

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
 * Valida a orquestração de salvamento e gestão de contexto de atendimento.
 */
class AtendimentoFacadeTest {

    private AtendimentoFacadeImpl facade;
    private ProponenteService proponenteService;
    private AtendimentoContextService contextoService;

    @BeforeEach
    void setUp() {
        proponenteService = mock(ProponenteService.class);
        contextoService = mock(AtendimentoContextService.class);
        facade = new AtendimentoFacadeImpl(proponenteService, contextoService);
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
            p.setId(1L); // Simula ID gerado pelo banco
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
    @DisplayName("Deve formatar link do WhatsApp corretamente com e sem prefixo")
    void deveFormatarLinkWhatsApp() {
        // Cenário 1: Sem prefixo
        String link1 = facade.formatarLinkWhatsApp("11999998888");
        assertEquals("https://wa.me/5511999998888", link1);

        // Cenário 2: Com prefixo já existente
        String link2 = facade.formatarLinkWhatsApp("5511999998888");
        assertEquals("https://wa.me/5511999998888", link2);

        // Cenário 3: Com caracteres especiais
        String link3 = facade.formatarLinkWhatsApp("(11) 99999-8888");
        assertEquals("https://wa.me/5511999998888", link3);
    }

    @Test
    @DisplayName("Deve retornar null ao formatar link de WhatsApp com telefone inválido")
    void deveRetornarNullParaTelefoneInvalido() {
        assertNull(facade.formatarLinkWhatsApp(null));
        assertNull(facade.formatarLinkWhatsApp("   "));
    }

    @Test
    @DisplayName("Deve delegar a limpeza de contexto para o AtendimentoContextService")
    void deveLimparContexto() {
        // WHEN
        facade.limparContextoAtendimento();

        // THEN
        verify(contextoService, times(1)).limparContexto();
    }

    @Test
    @DisplayName("Deve definir o lead ativo no contexto global")
    void deveDefinirLeadAtivo() {
        // GIVEN
        ProponenteModel lead = new ProponenteModel();
        lead.setId(100L);

        // WHEN
        facade.definirLeadAtivo(lead);

        // THEN
        verify(contextoService, times(1)).setLeadAtivo(lead);
    }

    @Test
    @DisplayName("Deve gerar resumo contextual (JSON) para o lead")
    void deveGerarResumoParaCopia() {
        // GIVEN
        ProponenteModel lead = new ProponenteModel();
        lead.setNomeCompleto("Maria Oliveira");

        // WHEN
        String resumo = facade.gerarResumoParaCopia(lead, "R$ 5.000,00");

        // THEN
        assertNotNull(resumo);
        assertTrue(resumo.contains("Maria Oliveira"), "O resumo deve conter os dados do lead.");
        // Nota: O método na Facade ignora a rendaFormatada e usa o
        // SummaryGeneratorUtils
    }
}
