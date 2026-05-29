package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.BancoAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.BancoCriadoEvent;
import br.com.poderfinanceiro.app.domain.event.BancoExcluidoEvent;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
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
 * <h1>BancoServiceTest</h1>
 * <p>
 * Testes de Unidade para a gestão de Instituições Bancárias.
 * Valida a persistência de dados mestres e a emissão de eventos de ciclo de
 * vida.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class BancoServiceTest {

    @InjectMocks
    private BancoService service;

    @Mock
    private BancoRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BancoModel bancoMock;

    @BeforeEach
    void setUp() {
        bancoMock = new BancoModel();
        bancoMock.setNome("BANCO ITAU");
        bancoMock.setCodigo("341");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar um banco nulo")
    void deveFalharAoSalvarNulo() {
        assertThatThrownBy(() -> service.salvar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser nulo");

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    @DisplayName("Deve salvar novo banco e disparar BancoCriadoEvent")
    void deveSalvarNovoBancoComEvento() {
        bancoMock.setId(null);

        when(repository.save(any(BancoModel.class))).thenAnswer(i -> {
            BancoModel salvo = i.getArgument(0);
            salvo.setId(10L);
            return salvo;
        });

        BancoModel resultado = service.salvar(bancoMock);

        assertThat(resultado.getId()).isEqualTo(10L);
        verify(repository).save(bancoMock);
        verify(eventPublisher).publishEvent(any(BancoCriadoEvent.class));
        verify(eventPublisher, never()).publishEvent(any(BancoAtualizadoEvent.class));
    }

    @Test
    @DisplayName("Deve atualizar banco existente e disparar BancoAtualizadoEvent")
    void deveAtualizarBancoComEvento() {
        bancoMock.setId(10L);

        when(repository.save(any(BancoModel.class))).thenReturn(bancoMock);

        BancoModel resultado = service.salvar(bancoMock);

        assertThat(resultado.getId()).isEqualTo(10L);
        verify(repository).save(bancoMock);
        verify(eventPublisher).publishEvent(any(BancoAtualizadoEvent.class));
        verify(eventPublisher, never()).publishEvent(any(BancoCriadoEvent.class));
    }

    @Test
    @DisplayName("Deve excluir banco e disparar BancoExcluidoEvent quando o ID existir")
    void deveExcluirComSucesso() {
        Long id = 10L;
        when(repository.existsById(id)).thenReturn(true);

        service.excluir(id);

        verify(repository).deleteById(id);
        verify(eventPublisher).publishEvent(any(BancoExcluidoEvent.class));
    }

    @Test
    @DisplayName("Não deve tentar excluir nem disparar evento quando o banco não for encontrado")
    void deveIgnorarExclusaoInexistente() {
        Long id = 99L;
        when(repository.existsById(id)).thenReturn(false);

        service.excluir(id);

        verify(repository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve retornar lista de bancos do repositório")
    void deveListarTodos() {
        List<BancoModel> bancos = List.of(bancoMock);
        when(repository.findAll()).thenReturn(bancos);

        List<BancoModel> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("BANCO ITAU");
    }

    @Test
    @DisplayName("Deve retornar lista vazia e logar erro em caso de falha no repositório")
    void deveTratarErroNaListagem() {
        when(repository.findAll()).thenThrow(new RuntimeException("Database Error"));

        List<BancoModel> resultado = service.listarTodos();

        assertThat(resultado).isEmpty();
    }
}
