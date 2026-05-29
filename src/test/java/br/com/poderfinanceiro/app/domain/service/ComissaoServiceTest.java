package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.ComissaoAtualizadaEvent;
import br.com.poderfinanceiro.app.domain.event.ComissaoCriadaEvent;
import br.com.poderfinanceiro.app.domain.event.PropostaPagaEvent;
import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.PropostaModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.ComissaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h1>ComissaoServiceTest</h1>
 * <p>
 * Testes de Unidade para a gestão de Comissões e Conciliação.
 * Valida a imutabilidade de ciclos pagos e a automação de auditoria por prazo.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ComissaoServiceTest {

    @InjectMocks
    private ComissaoService service;

    @Mock
    private ComissaoRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PropostaModel propostaMock;
    private ComissaoModel comissaoMock;

    @BeforeEach
    void setUp() {
        propostaMock = new PropostaModel();
        propostaMock.setId(100L);
        propostaMock.setComissaoEstimada(new BigDecimal("500.00"));
        propostaMock.setUsuario(new UsuarioModel());

        comissaoMock = new ComissaoModel();
        comissaoMock.setId(1L);
        comissaoMock.setProposta(propostaMock);
        comissaoMock.setStatusPagamento("Pendente");
    }

    @Test
    @DisplayName("Deve reagir ao evento de Proposta Paga e gerar uma nova comissão")
    void deveGerarComissaoAoCapturarEvento() {
        // CORREÇÃO: Removido o parâmetro 'this' (source), pois o Record espera apenas a
        // Proposta
        PropostaPagaEvent event = new PropostaPagaEvent(propostaMock);

        when(repository.findByPropostaId(100L)).thenReturn(List.of());
        when(repository.save(any(ComissaoModel.class))).thenAnswer(i -> i.getArgument(0));

        service.onPropostaPaga(event);

        verify(repository).save(any(ComissaoModel.class));
        verify(eventPublisher).publishEvent(any(ComissaoCriadaEvent.class));
    }

    @Test
    @DisplayName("Deve atualizar comissão existente se a proposta for paga novamente com novo valor")
    void deveAtualizarComissaoExistente() {
        when(repository.findByPropostaId(100L)).thenReturn(List.of(comissaoMock));
        when(repository.save(any(ComissaoModel.class))).thenReturn(comissaoMock);

        service.gerarOuAtualizarComissaoParaProposta(propostaMock);

        assertThat(comissaoMock.getValorBrutoComissao()).isEqualByComparingTo("500.00");
        verify(eventPublisher).publishEvent(any(ComissaoAtualizadaEvent.class));
    }

    @Test
    @DisplayName("Regra de Ouro: Deve impedir alteração em comissão com status Pago ou Liquidado")
    void deveImpedirAlteracaoEmComissaoLiquidada() {
        comissaoMock.setStatusPagamento("Pago");

        assertThatThrownBy(() -> service.salvarConciliacao(comissaoMock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi liquidado");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aplicar conferência automática quando o prazo de contestação expirar")
    void deveApplyConferenciaAutomaticaPorPrazo() {
        // Cenário: Prazo expirou ontem
        comissaoMock.setDataLimiteContestacao(LocalDateTime.now().minusDays(1));
        comissaoMock.setVerificadoConsultor(false);

        when(repository.save(any(ComissaoModel.class))).thenAnswer(i -> i.getArgument(0));

        ComissaoModel resultado = service.salvarConciliacao(comissaoMock);

        assertThat(resultado.isVerificadoConsultor()).isTrue();
        assertThat(resultado.getObservacaoAjuste()).contains("Conferência automática aplicada");
        verify(eventPublisher).publishEvent(any(ComissaoAtualizadaEvent.class));
    }

    @Test
    @DisplayName("Deve manter status manual se o prazo de contestação ainda não expirou")
    void deveManterStatusSeDentroDoPrazo() {
        // Cenário: Prazo expira amanhã
        comissaoMock.setDataLimiteContestacao(LocalDateTime.now().plusDays(1));
        comissaoMock.setVerificadoConsultor(false);

        when(repository.save(any(ComissaoModel.class))).thenAnswer(i -> i.getArgument(0));

        ComissaoModel resultado = service.salvarConciliacao(comissaoMock);

        assertThat(resultado.isVerificadoConsultor()).isFalse();
        assertThat(resultado.getObservacaoAjuste()).isNull();
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar gerar comissão nula")
    void deveFalharAoGerarComissaoNula() {
        assertThatThrownBy(() -> service.gerarNovaComissao(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
