package br.com.poderfinanceiro.app.common.util;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.TipoVinculoModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;
import br.com.poderfinanceiro.app.presentation.viewmodel.LeadViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("[Common] Teste de Unidade - SummaryGeneratorUtils")
class SummaryGeneratorUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(SummaryGeneratorUtilsTest.class);
    private static final String LOG_PREFIX = "[SummaryGeneratorUtilsTest]";

    @BeforeAll
    static void initJFX() {
        log.info("{} [SISTEMA] Inicializando Toolkit JavaFX para testes de UI Properties.", LOG_PREFIX);
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            log.debug("{} [SISTEMA] Toolkit JavaFX já estava em execução.", LOG_PREFIX);
        }
    }

    @Test
    @DisplayName("Deve gerar relatório Markdown (gerar) com formatação correta para WhatsApp")
    void deveGerarRelatorioMarkdown() {
        log.info("{} [TELEMETRIA] Testando geração de relatório Markdown para WhatsApp.", LOG_PREFIX);

        // GIVEN
        LeadViewModel vm = Mockito.mock(LeadViewModel.class);
        when(vm.nomeProperty()).thenReturn(new SimpleStringProperty("CARLOS ALBERTO"));
        when(vm.cpfProperty()).thenReturn(new SimpleStringProperty("123.456.789-01"));
        when(vm.telefoneProperty()).thenReturn(new SimpleStringProperty("(11) 98888-7777"));
        when(vm.dataNascimentoProperty()).thenReturn(new SimpleObjectProperty<>(LocalDate.of(1985, 10, 15)));
        when(vm.vinculoProperty()).thenReturn(new SimpleObjectProperty<>(TipoVinculoModel.APOSENTADO));
        when(vm.matriculaProperty()).thenReturn(new SimpleStringProperty("998877"));

        // WHEN
        String relatorio = SummaryGeneratorUtils.gerar(vm, "4.500,00");

        // THEN
        assertThat(relatorio).contains("CARLOS ALBERTO", "123.456.789-01", "4.500,00");
        assertThat(relatorio).contains("15/10/1985");
        assertThat(relatorio).contains("📑 *RELATÓRIO DE QUALIFICAÇÃO");
        log.info("{} [AUDITORIA] Teste de relatório Markdown concluído com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve gerar JSON contextual para IA com suporte nativo a LocalDate (JSR-310)")
    void deveGerarJsonContextualComDatasIso() {
        log.info("{} [TELEMETRIA] Testando serialização JSON JSR-310 para IA.", LOG_PREFIX);

        // GIVEN
        ProponenteModel model = new ProponenteModel();
        model.setNomeCompleto("ANA PAULA");
        model.setDataNascimento(LocalDate.of(1992, 1, 30));
        model.setRendaMensal(new BigDecimal("7200.50"));

        // WHEN
        String json = SummaryGeneratorUtils.gerarJsonContextualParaIA(model, false);

        // THEN
        assertThat(json).contains("ANA PAULA");
        assertThat(json).contains("1992-01-30"); // Valida o JavaTimeModule
        assertThat(json).contains("Ocultado por segurança");
        log.info("{} [AUDITORIA] Teste de serialização JSON concluído com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve incluir endereço principal no JSON quando permitido")
    void deveIncluirEnderecoNoJson() {
        log.info("{} [TELEMETRIA] Testando extração de endereço principal no JSON.", LOG_PREFIX);

        // GIVEN
        ProponenteModel model = new ProponenteModel();
        EnderecoProponenteModel end = new EnderecoProponenteModel();
        end.setLogradouro("RUA DAS FLORES");
        end.setNumero("123");
        end.setBairro("CENTRO");
        end.setCidade("SÃO PAULO");
        end.setUf(UfModel.SP);
        end.setPrincipal(true);
        model.setEnderecos(List.of(end));

        // WHEN
        String json = SummaryGeneratorUtils.gerarJsonContextualParaIA(model, true);

        // THEN
        assertThat(json).contains("RUA DAS FLORES", "123", "CENTRO", "SP");
        log.info("{} [AUDITORIA] Teste de inclusão de endereço concluído.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve serializar lista de tabelas de juros para JSON")
    void deveSerializarListaTabelas() {
        log.info("{} [TELEMETRIA] Testando serialização de lista de tabelas.", LOG_PREFIX);

        // GIVEN
        TabelaJurosModel t1 = new TabelaJurosModel();
        t1.setNomeTabela("TABELA TESTE");
        t1.setTaxaMensal(new BigDecimal("1.66"));

        // WHEN
        String json = SummaryGeneratorUtils.gerarJsonTabelasJuros(List.of(t1));

        // THEN
        assertThat(json).contains("TABELA TESTE", "1.66");
        assertThat(json).startsWith("[");
        log.info("{} [AUDITORIA] Teste de serialização de tabelas concluído.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve serializar lista de comissões com datas formatadas")
    void deveSerializarListaComissoes() {
        log.info("{} [TELEMETRIA] Testando serialização de lista de comissões.", LOG_PREFIX);

        // GIVEN
        ComissaoModel c = new ComissaoModel();
        c.setId(500L);
        c.setPrevisaoPagamento(LocalDate.of(2026, 6, 5));
        c.setValorBrutoComissao(new BigDecimal("1250.00"));

        // WHEN
        String json = SummaryGeneratorUtils.gerarJsonComissoes(List.of(c));

        // THEN
        assertThat(json).contains("500", "2026-06-05", "1250.0");
        log.info("{} [AUDITORIA] Teste de serialização de comissões concluído.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Robustez: Deve retornar estruturas vazias para entradas nulas")
    void deveTratarEntradasNulas() {
        log.info("{} [TELEMETRIA] Testando robustez contra entradas nulas.", LOG_PREFIX);

        assertThat(SummaryGeneratorUtils.gerar(null, null)).isEmpty();
        assertThat(SummaryGeneratorUtils.gerarJsonContextualParaIA(null, true)).isEqualTo("{}");
        assertThat(SummaryGeneratorUtils.gerarJsonTabelasJuros(null)).isEqualTo("[]");
        assertThat(SummaryGeneratorUtils.gerarJsonLinksUteis(null)).isEqualTo("[]");
        assertThat(SummaryGeneratorUtils.gerarJsonComissoes(null)).isEqualTo("[]");

        log.info("{} [AUDITORIA] Teste de robustez concluído.", LOG_PREFIX);
    }
}
