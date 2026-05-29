package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.ComissaoModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService.ContextoAlteradoEvent;
import br.com.poderfinanceiro.app.domain.service.AtendimentoContextService.TipoTelaFocada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h1>AtendimentoContextServiceTest</h1>
 * <p>
 * Testes de Unidade para o Gestor de Contexto Reativo.
 * Valida a integridade do estado compartilhado e a emissão de eventos de UI.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AtendimentoContextServiceTest {

    @InjectMocks
    private AtendimentoContextService service;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProponenteModel proponenteMock;

    @BeforeEach
    void setUp() {
        proponenteMock = new ProponenteModel();
        proponenteMock.setId(100L);
        proponenteMock.setNomeCompleto("CLIENTE TESTE");
    }

    @Test
    @DisplayName("Deve iniciar com o estado padrão (Dashboard e sem Lead)")
    void deveIniciarComEstadoPadrao() {
        assertThat(service.getTelaAtualFocada()).isEqualTo(TipoTelaFocada.DASHBOARD);
        assertThat(service.getLeadAtivo()).isNull();
        assertThat(service.getComissoesAtivas()).isEmpty();
    }

    @Test
    @DisplayName("Deve atualizar o Lead ativo e disparar evento de mudança")
    void deveAtualizarLeadEDispararEvento() {
        // Ação
        service.setLeadAtivo(proponenteMock);

        // Validação de Estado
        assertThat(service.getLeadAtivo()).isEqualTo(proponenteMock);

        // Validação de Evento
        ArgumentCaptor<ContextoAlteradoEvent> captor = ArgumentCaptor.forClass(ContextoAlteradoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue().lead()).isEqualTo(proponenteMock);
        assertThat(captor.getValue().tela()).isEqualTo(TipoTelaFocada.DASHBOARD);
    }

    @Test
    @DisplayName("Deve atualizar a tela focada e disparar evento apenas se a tela mudar")
    void deveAtualizarTelaEDispararEvento() {
        // Ação 1: Mudar para Clientes
        service.setTelaAtualFocada(TipoTelaFocada.LISTA_CLIENTES);

        // Ação 2: Tentar mudar para a mesma tela (não deve disparar novo evento)
        service.setTelaAtualFocada(TipoTelaFocada.LISTA_CLIENTES);

        assertThat(service.getTelaAtualFocada()).isEqualTo(TipoTelaFocada.LISTA_CLIENTES);

        // CORREÇÃO: verify com any() e times()
        verify(eventPublisher, times(1)).publishEvent(any(ContextoAlteradoEvent.class));
    }

    @Test
    @DisplayName("Deve realizar atualização atômica de Lead e Tela com um único evento")
    void deveAtualizarFocoInterfaceAtomicamente() {
        // Ação
        service.atualizarFocoInterface(proponenteMock, TipoTelaFocada.CADASTRO_CLIENTE);

        // Validação
        assertThat(service.getLeadAtivo()).isEqualTo(proponenteMock);
        assertThat(service.getTelaAtualFocada()).isEqualTo(TipoTelaFocada.CADASTRO_CLIENTE);
        assertThat(service.isAbaCadastroClienteAtiva()).isTrue();

        // Garante que apenas um evento foi disparado
        verify(eventPublisher, times(1)).publishEvent(any(ContextoAlteradoEvent.class));
    }

    @Test
    @DisplayName("Deve garantir que a lista de comissões retornada seja imutável")
    void deveGarantirImutabilidadeDasComissoes() {
        List<ComissaoModel> comissoes = List.of(new ComissaoModel());
        service.setComissoesAtivas(comissoes);

        List<ComissaoModel> resultado = service.getComissoesAtivas();

        // Tentar modificar a lista deve lançar exceção
        assertThatThrownBy(() -> resultado.add(new ComissaoModel()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve resetar todo o contexto ao limpar o atendimento")
    void deveLimparContextoCompletamente() {
        // Cenário: Contexto populado
        service.atualizarFocoInterface(proponenteMock, TipoTelaFocada.ESTEIRA_PROPOSTAS);

        // Ação
        service.limparContexto();

        // Validação
        assertThat(service.getLeadAtivo()).isNull();
        assertThat(service.getTelaAtualFocada()).isEqualTo(TipoTelaFocada.DASHBOARD);

        // CORREÇÃO: verify com atLeastOnce() e any()
        verify(eventPublisher, atLeastOnce()).publishEvent(any(ContextoAlteradoEvent.class));
    }
}
