package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.ProponenteAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.ProponenteCriadoEvent;
import br.com.poderfinanceiro.app.domain.event.ProponenteExcluidoEvent;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.ProponenteRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[Domínio] Teste de Unidade - ProponenteService")
class ProponenteServiceTest {

    @Mock
    private ProponenteRepository proponenteRepository;

    @Mock
    private AuthService authService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProponenteService proponenteService;

    private UsuarioModel consultorMock;
    private ProponenteModel proponenteMock;

    @BeforeEach
    void setUp() {
        consultorMock = new UsuarioModel();
        consultorMock.setId(1L);
        consultorMock.setNome("Consultor Teste");

        proponenteMock = new ProponenteModel();
        proponenteMock.setNomeCompleto("João da Silva");
        proponenteMock.setCpf("123.456.789-00");
        proponenteMock.setTelefone("(11) 98888-7777");
    }

    @Test
    @DisplayName("Deve salvar novo proponente com sanitização e disparar evento de criação")
    void deveSalvarNovoProponenteComSucesso() {
        // GIVEN
        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(proponenteRepository.existsByCpfAndUsuarioIdAndDeletadoEmIsNull(anyString(), anyLong())).thenReturn(false);
        when(proponenteRepository.save(any(ProponenteModel.class))).thenAnswer(invocation -> {
            ProponenteModel p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        // WHEN
        ProponenteModel resultado = proponenteService.salvarProponente(proponenteMock);

        // THEN
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(100L);
        assertThat(resultado.getNomeCompleto()).isEqualTo("JOÃO DA SILVA"); // Sanitização UPPERCASE
        assertThat(resultado.getCpf()).isEqualTo("12345678900"); // Sanitização Digits Only
        assertThat(resultado.getUsuario()).isEqualTo(consultorMock);

        verify(eventPublisher, times(1)).publishEvent(any(ProponenteCriadoEvent.class));
        verify(proponenteRepository, times(1)).save(proponenteMock);
    }

    @Test
    @DisplayName("Deve atualizar proponente existente e disparar evento de atualização")
    void deveAtualizarProponenteExistente() {
        // GIVEN
        proponenteMock.setId(100L);
        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(proponenteRepository.existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(anyString(), anyLong(), anyLong()))
                .thenReturn(false);
        when(proponenteRepository.save(any(ProponenteModel.class))).thenReturn(proponenteMock);

        // WHEN
        proponenteService.salvarProponente(proponenteMock);

        // THEN
        verify(eventPublisher, times(1)).publishEvent(any(ProponenteAtualizadoEvent.class));
        verify(proponenteRepository, times(1)).save(proponenteMock);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar sem consultor logado")
    void deveFalharAoSalvarSemUsuarioLogado() {
        // GIVEN
        when(authService.getUsuarioLogado()).thenReturn(null);

        // WHEN & THEN
        assertThatThrownBy(() -> proponenteService.salvarProponente(proponenteMock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sessão inválida");
    }

    @Test
    @DisplayName("Deve impedir duplicidade de CPF na mesma carteira")
    void deveFalharCpfDuplicadoNaCarteira() {
        // GIVEN
        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(proponenteRepository.existsByCpfAndUsuarioIdAndDeletadoEmIsNull(anyString(), anyLong())).thenReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> proponenteService.salvarProponente(proponenteMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Este CPF já está cadastrado");
    }

    @Test
    @DisplayName("Deve listar apenas proponentes da carteira do consultor logado")
    void deveListarMinhaCarteira() {
        // GIVEN
        when(authService.getUsuarioLogado()).thenReturn(consultorMock);
        when(proponenteRepository.findByUsuarioIdAndDeletadoEmIsNull(1L))
                .thenReturn(List.of(proponenteMock));

        // WHEN
        List<ProponenteModel> carteira = proponenteService.listarMinhaCarteira();

        // THEN
        assertThat(carteira).hasSize(1);
        assertThat(carteira.get(0).getNomeCompleto()).contains("João");
        verify(proponenteRepository, times(1)).findByUsuarioIdAndDeletadoEmIsNull(1L);
    }

    @Test
    @DisplayName("Deve limpar termo de busca antes de consultar repositório")
    void deveSanitizarBuscaRapida() {
        // GIVEN
        String termoSujo = "123.456-78";
        when(authService.getUsuarioLogado()).thenReturn(consultorMock);

        // WHEN
        proponenteService.buscaRapida(termoSujo);

        // THEN
        verify(proponenteRepository).buscarRapidaPorNomeOuCpf("12345678", 1L);
    }

       @Test
    @DisplayName("Deve realizar exclusão e disparar evento de exclusão")
    void deveExcluirProponenteComSucesso() {
        // GIVEN
        Long idExclusao = 50L;
        when(proponenteRepository.existsById(idExclusao)).thenReturn(true);

        // WHEN
        proponenteService.excluirProponente(idExclusao);

        // THEN
        verify(proponenteRepository, times(1)).deleteById(idExclusao);
        
        // Captura o evento para validar se o ID enviado no evento é o correto
        ArgumentCaptor<ProponenteExcluidoEvent> eventCaptor = ArgumentCaptor.forClass(ProponenteExcluidoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        // Ajustado de .proponenteId() para .id() conforme padrão de Records
        assertThat(eventCaptor.getValue().idProponente()).isEqualTo(idExclusao);
    }


    @Test
    @DisplayName("Não deve tentar excluir ID inexistente")
    void naoDeveExcluirInexistente() {
        // GIVEN
        Long idInexistente = 999L;
        when(proponenteRepository.existsById(idInexistente)).thenReturn(false);

        // WHEN
        proponenteService.excluirProponente(idInexistente);

        // THEN
        verify(proponenteRepository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
