package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.event.PropostaPagaEvent;
import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.service.*;
import br.com.poderfinanceiro.app.util.BancoModelBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para PropostaFacadeImpl.
 * Valida a orquestração complexa de propostas, eventos e regras de negócio.
 */
class PropostaFacadeTest {

    private PropostaFacadeImpl facade;

    // Mocks das 7 dependências
    private PropostaService propostaService;
    private TabelaJurosService tabelaJurosService;
    private ProponenteService proponenteService;
    private DocumentoService documentoService;
    private AtendimentoContextService contextoService;
    private ApplicationEventPublisher eventPublisher;
    private AssistenteDocumentalService assistenteIA;

    @BeforeEach
    void setUp() {
        propostaService = mock(PropostaService.class);
        tabelaJurosService = mock(TabelaJurosService.class);
        proponenteService = mock(ProponenteService.class);
        documentoService = mock(DocumentoService.class);
        contextoService = mock(AtendimentoContextService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        assistenteIA = mock(AssistenteDocumentalService.class);

        facade = new PropostaFacadeImpl(
                propostaService, tabelaJurosService, proponenteService,
                documentoService, contextoService, eventPublisher, assistenteIA);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar proposta PAGA sem valor aprovado")
    void deveValidarPropostaPagaSemValor() {
        // GIVEN
        PropostaModel propostaInvalida = new PropostaModel();
        propostaInvalida.setStatus(StatusPropostaModel.PAGO);
        propostaInvalida.setValorAprovado(BigDecimal.ZERO);

        // WHEN & THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> facade.salvarProposta(propostaInvalida));
        assertTrue(ex.getMessage().contains("Valor Aprovado deve ser preenchido"));
        verifyNoInteractions(propostaService);
    }

    @Test
    @DisplayName("Deve disparar PropostaPagaEvent quando o status for PAGO")
    void deveDispararEventoAoPagar() {
        // GIVEN
        PropostaModel dadosUI = new PropostaModel();
        dadosUI.setId(1L);
        dadosUI.setStatus(StatusPropostaModel.PAGO);
        dadosUI.setValorAprovado(new BigDecimal("5000.00"));

        when(propostaService.carregarPropostaDetalhada(1L)).thenReturn(new PropostaModel());
        when(propostaService.salvarProposta(any())).thenReturn(dadosUI);

        // WHEN
        facade.salvarProposta(dadosUI);

        // THEN
        verify(eventPublisher, times(1)).publishEvent(any(PropostaPagaEvent.class));
    }

    @Test
    @DisplayName("Deve calcular comissão priorizando o valor aprovado sobre o solicitado")
    void deveCalcularComissaoComPrioridade() {
        // GIVEN
        PropostaModel proposta = new PropostaModel();
        proposta.setTabelaId(10L);
        proposta.setValorSolicitado(new BigDecimal("10000.00"));
        proposta.setValorAprovado(new BigDecimal("8000.00"));

        // WHEN
        facade.calcularComissao(proposta);

        // THEN
        // Deve usar 8000.00 como base
        verify(propostaService).calcularComissaoEstimada(eq(new BigDecimal("8000.00")), eq(10L));
    }

        @Test
    @DisplayName("Deve listar bancos únicos a partir das tabelas ativas")
    void deveListarBancosDasTabelas() {
        // GIVEN - Uso do Builder (Pilar 2)
        // Note como o código fica limpo e expressivo
        BancoModel b1 = BancoModelBuilder.umBanco().comId(1L).comNome("Banco A").comCodigo("001").build();
        BancoModel b2 = BancoModelBuilder.umBanco().comId(2L).comNome("Banco B").comCodigo("002").build();
        
        // Teste de Identidade (Pilar 1): Mesmo que b3 não tenha ID, se tiver o código "001", 
        // o distinct() saberá que ele é igual ao b1.
        BancoModel b3 = BancoModelBuilder.umBanco().comNome("Banco A").comCodigo("001").build();

        TabelaJurosModel t1 = new TabelaJurosModel(); t1.setBanco(b1);
        TabelaJurosModel t2 = new TabelaJurosModel(); t2.setBanco(b3); // Duplicado por Chave de Negócio
        TabelaJurosModel t3 = new TabelaJurosModel(); t3.setBanco(b2);

        when(tabelaJurosService.listarAtivas()).thenReturn(List.of(t1, t2, t3));

        // WHEN
        List<BancoModel> bancos = facade.listarBancosDasTabelasAtivas();

        // THEN
        assertNotNull(bancos);
        assertEquals(2, bancos.size(), "O distinct() deve reconhecer a igualdade pela Chave de Negócio (Código).");
        assertTrue(bancos.contains(b1));
        assertTrue(bancos.contains(b2));
    }


    @Test
    @DisplayName("Deve atualizar o contexto global de atendimento para a esteira")
    void deveAtualizarContexto() {
        // GIVEN
        PropostaModel proposta = new PropostaModel();
        proposta.setId(123L);

        // WHEN
        facade.atualizarContextoAtendimento(proposta);

        // THEN
        verify(contextoService).setPropostaAtiva(proposta);
        verify(contextoService).setTelaAtualFocada(AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS);
    }

    @Test
    @DisplayName("Deve delegar análise de IA para o AssistenteDocumentalService")
    void deveAnalisarDocumentoComIA() {
        // GIVEN
        DocumentoProponenteModel doc = new DocumentoProponenteModel();
        doc.setId(1L);
        ProponenteModel proponente = new ProponenteModel();
        String modelo = "gemini-1.5-flash";

        // WHEN
        facade.analisarDocumentoComIA(doc, proponente, modelo);

        // THEN
        verify(assistenteIA).analisarDocumento(doc, proponente, modelo);
    }
}
