package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.ResultadoSimulacaoDTO;
import br.com.poderfinanceiro.app.application.dto.SimulacaoRascunhoDTO;
import br.com.poderfinanceiro.app.domain.event.PropostaCriadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaExcluidaEvent;
import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import br.com.poderfinanceiro.app.domain.repository.DocumentoProponenteRepository;
import br.com.poderfinanceiro.app.domain.repository.PropostaRepository;
import br.com.poderfinanceiro.app.domain.repository.TabelaJurosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[Domínio] Teste de Unidade - PropostaService")
class PropostaServiceTest {

    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private TabelaJurosRepository tabelaJurosRepository;
    @Mock
    private ComissaoRepository comissaoRepository;
    @Mock
    private DocumentoProponenteRepository documentoRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AuthService authService;

    @InjectMocks
    private PropostaService propostaService;

    private UsuarioModel consultorMock;
    private TabelaJurosModel tabelaMock;
    private PropostaModel propostaMock;

    @BeforeEach
    void setUp() {
        consultorMock = new UsuarioModel();
        consultorMock.setId(1L);
        consultorMock.setNome("Consultor Master");

        tabelaMock = new TabelaJurosModel();
        tabelaMock.setId(10L);
        tabelaMock.setNomeTabela("INSS - NOVO - 1.66%");
        tabelaMock.setComissaoPercentual(new BigDecimal("5.00"));
        tabelaMock.setTipoConvenio(TipoConvenioModel.INSS_CONSIGNADO);

        propostaMock = new PropostaModel();
        propostaMock.setValorSolicitado(new BigDecimal("10000.00"));
        propostaMock.setTabelaId(10L);
        propostaMock.setStatus(StatusPropostaModel.DIGITADA);
    }

    @Test
    @DisplayName("Deve calcular comissão estimada corretamente com arredondamento")
    void deveCalcularComissaoEstimada() {
        when(tabelaJurosRepository.findById(10L)).thenReturn(Optional.of(tabelaMock));
        BigDecimal comissao = propostaService.calcularComissaoEstimada(new BigDecimal("10000.00"), 10L);
        assertThat(comissao).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Deve salvar nova proposta, sincronizar tabela e disparar evento")
    void deveSalvarNovaPropostaComSincronizacao() {
        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(tabelaJurosRepository.findById(10L)).thenReturn(Optional.of(tabelaMock));
        when(propostaRepository.save(any(PropostaModel.class))).thenAnswer(i -> {
            PropostaModel p = i.getArgument(0);
            p.setId(500L);
            return p;
        });

        PropostaModel salva = propostaService.salvarProposta(propostaMock);

        assertThat(salva.getUsuario()).isEqualTo(consultorMock);
        assertThat(salva.getConvenioOrgao()).isEqualTo(TipoConvenioModel.INSS_CONSIGNADO);
        verify(eventPublisher).publishEvent(any(PropostaCriadaEvent.class));
    }

    @Test
    @DisplayName("Deve processar ciclo financeiro quando status for PAGO")
    void deveProcessarCicloFinanceiroAoPagar() {
        propostaMock.setStatus(StatusPropostaModel.PAGO);
        propostaMock.setValorAprovado(new BigDecimal("10000.00"));

        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(tabelaJurosRepository.findById(10L)).thenReturn(Optional.of(tabelaMock));
        when(propostaRepository.save(any(PropostaModel.class))).thenReturn(propostaMock);
        when(comissaoRepository.findByPropostaId(any())).thenReturn(List.of());

        propostaService.salvarProposta(propostaMock);

        verify(comissaoRepository).save(any(ComissaoModel.class));
    }

    @Test
    @DisplayName("Deve converter rascunho de simulação para PropostaModel corretamente")
    void deveConverterRascunhoParaProposta() {
        // Ajustado para o construtor de 6 campos conforme definição do Record
        SimulacaoRascunhoDTO rascunho = new SimulacaoRascunhoDTO(
                65, // idade
                new BigDecimal("3500.00"), // rendaMensal
                "INSS_CONSIGNADO", // tipoConvenio
                new BigDecimal("5000.00"), // valorDesejado
                84, // prazoDesejado
                new BigDecimal("450.00") // margemDisponivel
        );

        BancoModel banco = new BancoModel();
        banco.setNome("Banco Itaú");

        TabelaJurosModel tabelaSimulada = new TabelaJurosModel();
        tabelaSimulada.setId(99L);
        tabelaSimulada.setBanco(banco);

        // Ajustado para o construtor de 3 campos conforme definição do Record
        ResultadoSimulacaoDTO resultado = new ResultadoSimulacaoDTO(
                tabelaSimulada,
                new BigDecimal("150.00"),
                new BigDecimal("120.00"));

        ProponenteModel cliente = new ProponenteModel();
        cliente.setNomeCompleto("CLIENTE TESTE");

        when(authService.getUsuarioLogado()).thenReturn(consultorMock);

        PropostaModel convertida = propostaService.converterRascunhoParaProposta(rascunho, resultado, cliente);

        assertThat(convertida.getProponente()).isEqualTo(cliente);
        assertThat(convertida.getValorSolicitado()).isEqualByComparingTo("5000.00");
        assertThat(convertida.getPrazoDesejado()).isEqualTo(84);
        assertThat(convertida.getConvenioOrgao()).isEqualTo(TipoConvenioModel.INSS_CONSIGNADO);
    }

    @Test
    @DisplayName("Deve excluir proposta e disparar evento de exclusão")
    void deveExcluirPropostaComSucesso() {
        Long idExclusao = 77L;
        propostaService.excluirProposta(idExclusao);
        verify(propostaRepository).deleteById(idExclusao);

        ArgumentCaptor<PropostaExcluidaEvent> eventCaptor = ArgumentCaptor.forClass(PropostaExcluidaEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().idProposta()).isEqualTo(idExclusao);
    }
}
