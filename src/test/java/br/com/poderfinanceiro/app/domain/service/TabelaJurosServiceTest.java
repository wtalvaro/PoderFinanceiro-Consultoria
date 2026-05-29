package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import br.com.poderfinanceiro.app.domain.event.TabelaJurosArquivadoEvent;
import br.com.poderfinanceiro.app.domain.event.TabelaJurosAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.TabelaJurosCriadoEvent;
import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[Domínio] Teste de Unidade - TabelaJurosService")
class TabelaJurosServiceTest {

    @Mock
    private TabelaJurosRepository tabelaJurosRepository;
    @Mock
    private BancoRepository bancoRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TabelaJurosService tabelaJurosService;

    private BancoModel bancoMock;
    private TabelaJurosModel tabelaMock;

    @BeforeEach
    void setUp() {
        bancoMock = new BancoModel();
        bancoMock.setId(1L);
        bancoMock.setNome("BANCO TESTE");

        tabelaMock = new TabelaJurosModel();
        tabelaMock.setId(100L);
        tabelaMock.setBanco(bancoMock);
        tabelaMock.setNomeTabela("TABELA ORIGINAL");
        tabelaMock.setAtivo(true);
        tabelaMock.setTaxaMensal(new BigDecimal("1.50"));
    }

    @Test
    @DisplayName("Deve salvar nova tabela e disparar evento de criação")
    void deveSalvarNovaTabela() {
        // GIVEN
        TabelaJurosModel nova = new TabelaJurosModel();
        nova.setNomeTabela("NOVA TABELA");
        when(tabelaJurosRepository.save(any(TabelaJurosModel.class))).thenAnswer(i -> {
            TabelaJurosModel m = i.getArgument(0);
            m.setId(200L);
            return m;
        });

        // WHEN
        TabelaJurosModel salva = tabelaJurosService.salvarComRegraDeOuro(nova);

        // THEN
        assertThat(salva.getId()).isEqualTo(200L);
        assertThat(salva.getInicioVigencia()).isEqualTo(LocalDate.now());
        assertThat(salva.getAtivo()).isTrue();
        verify(eventPublisher).publishEvent(any(TabelaJurosCriadoEvent.class));
    }

    @Test
    @DisplayName("Regra de Ouro: Deve arquivar antiga e criar nova versão ao editar")
    void deveAplicarRegraDeOuroNaEdicao() {
        // GIVEN
        TabelaJurosModel edicao = new TabelaJurosModel();
        edicao.setId(100L); // Mesmo ID da tabelaMock
        edicao.setNomeTabela("TABELA EDITADA");
        edicao.setTaxaMensal(new BigDecimal("1.60"));

        when(tabelaJurosRepository.findById(100L)).thenReturn(Optional.of(tabelaMock));
        when(tabelaJurosRepository.save(any(TabelaJurosModel.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        TabelaJurosModel resultado = tabelaJurosService.salvarComRegraDeOuro(edicao);

        // THEN
        // 1. Verifica se a antiga foi arquivada
        assertThat(tabelaMock.getAtivo()).isFalse();
        assertThat(tabelaMock.getFimVigencia()).isEqualTo(LocalDate.now());

        // 2. Verifica se a nova versão foi criada corretamente (Deep Copy)
        assertThat(resultado.getId()).isNull(); // Antes de salvar no DB real seria null
        assertThat(resultado.getNomeTabela()).isEqualTo("TABELA EDITADA");
        assertThat(resultado.getTaxaMensal()).isEqualTo(new BigDecimal("1.60"));
        assertThat(resultado.getBanco()).isEqualTo(bancoMock);
        assertThat(resultado.getAtivo()).isTrue();

        verify(tabelaJurosRepository, times(2)).save(any());
        verify(eventPublisher).publishEvent(any(TabelaJurosAtualizadoEvent.class));
    }

    @Test
    @DisplayName("Deve arquivar tabela existente com sucesso")
    void deveArquivarTabela() {
        // GIVEN
        when(tabelaJurosRepository.findById(100L)).thenReturn(Optional.of(tabelaMock));

        // WHEN
        tabelaJurosService.arquivarTabela(tabelaMock);

        // THEN
        assertThat(tabelaMock.getAtivo()).isFalse();
        assertThat(tabelaMock.getFimVigencia()).isEqualTo(LocalDate.now());
        verify(eventPublisher).publishEvent(any(TabelaJurosArquivadoEvent.class));
    }

    @Test
    @DisplayName("Deve processar importação em lote e ignorar falhas individuais")
    void deveProcessarLoteImportado() {
        // GIVEN
        TabelaImportadaDTO dtoSucesso = new TabelaImportadaDTO();
        dtoSucesso.setBanco("BANCO TESTE");
        dtoSucesso.setNomeTabela("TABELA IMPORTADA");
        dtoSucesso.setTipoConvenio("INSS_CONSIGNADO");

        TabelaImportadaDTO dtoErro = new TabelaImportadaDTO();
        dtoErro.setBanco("BANCO INEXISTENTE");

        when(bancoRepository.findFirstByNomeContainingIgnoreCase("BANCO TESTE")).thenReturn(Optional.of(bancoMock));
        when(bancoRepository.findFirstByNomeContainingIgnoreCase("BANCO INEXISTENTE")).thenReturn(Optional.empty());

        // WHEN
        tabelaJurosService.salvarLoteTabelasImportadas(List.of(dtoSucesso, dtoErro));

        // THEN
        verify(tabelaJurosRepository, times(1)).save(any(TabelaJurosModel.class));
        // O erro no segundo item não deve impedir a execução, apenas logar
    }

    @Test
    @DisplayName("Deve mapear DTO para Model com fallback de convênio e datas")
    void deveMapearDtoParaModelCorretamente() {
        // GIVEN
        TabelaImportadaDTO dto = new TabelaImportadaDTO();
        dto.setBanco("BANCO TESTE");
        dto.setNomeTabela("IA_TABELA");
        dto.setTipoConvenio("CONVENIO_INVALIDO"); // Deve cair no fallback
        dto.setInicioVigenciaCalculado("2025-01-01");
        dto.setFimVigenciaCalculado("null"); // Deve ser tratado como null

        when(bancoRepository.findFirstByNomeContainingIgnoreCase(anyString())).thenReturn(Optional.of(bancoMock));
        when(tabelaJurosRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // WHEN
        tabelaJurosService.salvarLoteTabelasImportadas(List.of(dto));

        // THEN
        ArgumentCaptor<TabelaJurosModel> captor = ArgumentCaptor.forClass(TabelaJurosModel.class);
        verify(tabelaJurosRepository).save(captor.capture());

        TabelaJurosModel model = captor.getValue();
        assertThat(model.getTipoConvenio()).isEqualTo(TipoConvenioModel.INSS_CONSIGNADO); // Fallback
        assertThat(model.getInicioVigencia()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(model.getAtivo()).isTrue();
    }
}
