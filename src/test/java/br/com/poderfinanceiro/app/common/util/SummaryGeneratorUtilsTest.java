package br.com.poderfinanceiro.app.common.util;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.TipoVinculoModel;
import br.com.poderfinanceiro.app.presentation.viewmodel.LeadViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("[Common] Teste de Unidade - SummaryGeneratorUtils")
class SummaryGeneratorUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(SummaryGeneratorUtilsTest.class);
    private static final String LOG_PREFIX = "[SummaryGeneratorUtilsTest]";

    private SummaryGeneratorUtils summaryUtils;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            log.debug("{} [SISTEMA] Toolkit JavaFX já ativo.", LOG_PREFIX);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        // CORREÇÃO CRÍTICA: Desabilita timestamps para garantir formato ISO-8601
        // "yyyy-MM-dd"
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        summaryUtils = new SummaryGeneratorUtils(objectMapper);
    }

    @Test
    @DisplayName("Deve gerar relatório Markdown com formatação correta")
    void deveGerarRelatorioMarkdown() {
        log.info("{} [TELEMETRIA] Testando geração Markdown.", LOG_PREFIX);
        LeadViewModel vm = Mockito.mock(LeadViewModel.class);
        when(vm.nomeProperty()).thenReturn(new SimpleStringProperty("CARLOS"));
        when(vm.cpfProperty()).thenReturn(new SimpleStringProperty("123"));
        when(vm.telefoneProperty()).thenReturn(new SimpleStringProperty("456"));
        when(vm.dataNascimentoProperty()).thenReturn(new SimpleObjectProperty<>(LocalDate.of(1985, 10, 15)));
        when(vm.vinculoProperty()).thenReturn(new SimpleObjectProperty<>(TipoVinculoModel.APOSENTADO));
        when(vm.matriculaProperty()).thenReturn(new SimpleStringProperty("99"));

        String relatorio = summaryUtils.gerar(vm, "4.500,00");

        assertThat(relatorio).contains("CARLOS", "15/10/1985");
        log.info("{} [AUDITORIA] Relatório validado.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve gerar JSON contextual para IA com suporte nativo a LocalDate (ISO)")
    void deveGerarJsonContextualComDatasIso() {
        log.info("{} [TELEMETRIA] Testando serialização ISO-8601.", LOG_PREFIX);
        ProponenteModel model = new ProponenteModel();
        model.setNomeCompleto("ANA PAULA");
        model.setDataNascimento(LocalDate.of(1992, 1, 30));

        String json = summaryUtils.gerarJsonContextualParaIA(model, false);

        // Agora o assert passará pois o formato será "1992-01-30" e não [1992,1,30]
        assertThat(json).contains("1992-01-30");
        log.info("{} [AUDITORIA] Serialização ISO validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve serializar lista de comissões com datas formatadas")
    void deveSerializarListaComissoes() {
        log.info("{} [TELEMETRIA] Testando serialização de comissões.", LOG_PREFIX);
        ComissaoModel c = new ComissaoModel();
        c.setId(500L);
        c.setPrevisaoPagamento(LocalDate.of(2026, 6, 5));

        String json = summaryUtils.gerarJsonComissoes(List.of(c));

        assertThat(json).contains("2026-06-05");
        log.info("{} [AUDITORIA] Lista de comissões validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Robustez: Deve retornar estruturas vazias para entradas nulas")
    void deveTratarEntradasNulas() {
        log.info("{} [TELEMETRIA] Testando robustez.", LOG_PREFIX);
        assertThat(summaryUtils.gerar(null, null)).isEmpty();
        assertThat(summaryUtils.gerarJsonContextualParaIA(null, true)).isEqualTo("{}");
        log.info("{} [AUDITORIA] Robustez validada.", LOG_PREFIX);
    }
}
