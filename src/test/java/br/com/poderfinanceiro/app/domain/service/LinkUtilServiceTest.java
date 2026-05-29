package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.LinkUtilAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.LinkUtilCriadoEvent;
import br.com.poderfinanceiro.app.domain.event.LinkUtilExcluidoEvent;
import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.repository.LinkUtilRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * <h1>LinkUtilServiceTest</h1>
 * <p>
 * Testes de Unidade para a gestão de Links Úteis.
 * Valida o ciclo de vida de persistência e a emissão de eventos de
 * sincronização de UI.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class LinkUtilServiceTest {

    @InjectMocks
    private LinkUtilService service;

    @Mock
    private LinkUtilRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LinkUtilModel linkMock;

    @BeforeEach
    void setUp() {
        linkMock = new LinkUtilModel();
        linkMock.setTitulo("Portal do Consignado");
        linkMock.setUrl("https://portal.exemplo.com");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar um link nulo")
    void deveFalharAoSalvarNulo() {
        assertThatThrownBy(() -> service.salvar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser nulo");

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    @DisplayName("Deve salvar novo link e disparar LinkUtilCriadoEvent")
    void deveSalvarNovoLinkComEvento() {
        // Simula que o link não tem ID (é novo)
        linkMock.setId(null);

        when(repository.save(any(LinkUtilModel.class))).thenAnswer(i -> {
            LinkUtilModel salvo = i.getArgument(0);
            salvo.setId(1L); // Simula ID gerado
            return salvo;
        });

        LinkUtilModel resultado = service.salvar(linkMock);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(repository).save(linkMock);
        verify(eventPublisher).publishEvent(any(LinkUtilCriadoEvent.class));
        verify(eventPublisher, never()).publishEvent(any(LinkUtilAtualizadoEvent.class));
    }

    @Test
    @DisplayName("Deve atualizar link existente e disparar LinkUtilAtualizadoEvent")
    void deveAtualizarLinkComEvento() {
        // Simula que o link já existe
        linkMock.setId(1L);

        when(repository.save(any(LinkUtilModel.class))).thenReturn(linkMock);

        LinkUtilModel resultado = service.salvar(linkMock);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(repository).save(linkMock);
        verify(eventPublisher).publishEvent(any(LinkUtilAtualizadoEvent.class));
        verify(eventPublisher, never()).publishEvent(any(LinkUtilCriadoEvent.class));
    }

    @Test
    @DisplayName("Deve excluir link e disparar LinkUtilExcluidoEvent quando o ID existir")
    void deveExcluirComSucesso() {
        Long id = 1L;
        when(repository.existsById(id)).thenReturn(true);

        service.excluir(id);

        verify(repository).deleteById(id);
        verify(eventPublisher).publishEvent(any(LinkUtilExcluidoEvent.class));
    }

    @Test
    @DisplayName("Não deve tentar excluir nem disparar evento quando o ID não existir")
    void deveIgnorarExclusaoInexistente() {
        Long id = 99L;
        when(repository.existsById(id)).thenReturn(false);

        service.excluir(id);

        verify(repository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve retornar lista vazia e logar erro em caso de falha no repositório")
    void deveTratarErroNaListagem() {
        when(repository.findAllByOrderByCategoriaAscTituloAsc()).thenThrow(new RuntimeException("DB Offline"));

        List<LinkUtilModel> resultado = service.listarTodos();

        assertThat(resultado).isEmpty();
        // O log [SISTEMA] será gerado internamente
    }
}
