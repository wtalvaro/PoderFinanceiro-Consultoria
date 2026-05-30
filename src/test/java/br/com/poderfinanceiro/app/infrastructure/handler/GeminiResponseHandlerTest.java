package br.com.poderfinanceiro.app.infrastructure.handler;

import br.com.poderfinanceiro.app.application.dto.GeminiResponse;
import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de Unidade Gold Standard para GeminiResponseHandler.
 * Sincronizado com a injeção de dependência do ObjectMapper (v2.1.4).
 */
@DisplayName("[Infra] Teste de Unidade - GeminiResponseHandler")
class GeminiResponseHandlerTest {

    private static final Logger log = LoggerFactory.getLogger(GeminiResponseHandlerTest.class);
    private static final String LOG_PREFIX = "[GeminiResponseHandlerTest]";

    private GeminiResponseHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 1. Instanciação do motor JSON
        objectMapper = new ObjectMapper();

        // 2. Registro de módulos para Java 25 (Records e Datas)
        objectMapper.findAndRegisterModules();

        // 3. CONFIGURAÇÃO DE RESILIÊNCIA (O que estava faltando no teste)
        // Impede que o Jackson quebre ao encontrar campos extras enviados pela IA
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 4. Padronização de datas
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // 5. Injeção no Handler
        handler = new GeminiResponseHandler(objectMapper);

        log.info("{} [SISTEMA] Ambiente de teste configurado com Resiliência Ativa.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve extrair texto profundo da estrutura GeminiResponse")
    void deveExtrairTextoDaEstruturaGemini() {
        log.info("{} [TELEMETRIA] Testando extração de texto da estrutura aninhada.", LOG_PREFIX);

        // GIVEN: Montagem da estrutura aninhada do Record GeminiResponse
        GeminiResponse.Part part = new GeminiResponse.Part("Conteúdo da IA");
        GeminiResponse.Content content = new GeminiResponse.Content(List.of(part));
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate(content);
        GeminiResponse response = new GeminiResponse(List.of(candidate));

        // WHEN
        String texto = handler.extrairTexto(response);

        // THEN
        assertThat(texto).isEqualTo("Conteúdo da IA");
        log.info("{} [AUDITORIA] Extração de texto validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Scanner Agressivo: Deve extrair JSON de dentro de blocos Markdown")
    void deveExtrairJsonDeMarkdown() {
        log.info("{} [TELEMETRIA] Testando scanner agressivo contra ruído Markdown.", LOG_PREFIX);

        // GIVEN: Resposta com ruído típico de IA
        String raw = "Aqui está o resultado:\n```json\n{\"nomeTabela\": \"INSS\", \"taxaMensal\": 1.66}\n```\nEspero que ajude.";

        // WHEN
        String jsonPuro = handler.extrairTextoDeJsonBruto(raw);

        // THEN
        assertThat(jsonPuro).isEqualTo("{\"nomeTabela\": \"INSS\", \"taxaMensal\": 1.66}");
        log.info("{} [AUDITORIA] Scanner agressivo validado com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Conversão: Deve converter JSON sujo para DTO TabelaImportadaDTO")
    void deveConverterParaObjetoComSucesso() {
        log.info("{} [TELEMETRIA] Testando conversão direta para DTO.", LOG_PREFIX);

        // GIVEN
        String raw = "O banco identificado foi: {\"nomeTabela\": \"SANTANDER\", \"prazoMaximo\": 84}";

        // WHEN
        TabelaImportadaDTO dto = handler.converterParaObjeto(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.getNomeTabela()).isEqualTo("SANTANDER");
        assertThat(dto.getPrazoMaximo()).isEqualTo(84);
        log.info("{} [AUDITORIA] Conversão para objeto validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Lote: Deve converter lista JSON com ruído para List de DTOs")
    void deveConverterParaListaComSucesso() {
        log.info("{} [TELEMETRIA] Testando conversão de lista em lote.", LOG_PREFIX);

        // GIVEN: Lista com texto antes e depois
        String raw = "Resultados: [{\"nomeTabela\": \"PAN\"}, {\"nomeTabela\": \"BMG\"}] - Fim da lista.";

        // WHEN
        List<TabelaImportadaDTO> lista = handler.converterParaLista(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getNomeTabela()).isEqualTo("PAN");
        assertThat(lista.get(1).getNomeTabela()).isEqualTo("BMG");
        log.info("{} [AUDITORIA] Conversão em lote validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Resiliência: Deve ignorar propriedades desconhecidas enviadas pela IA")
    void deveIgnorarPropriedadesDesconhecidas() {
        log.info("{} [TELEMETRIA] Testando resiliência contra campos extras da IA.", LOG_PREFIX);

        // GIVEN: JSON com campo 'alucinacao_ia' que não existe no DTO
        String raw = "{\"nomeTabela\": \"CAIXA\", \"alucinacao_ia\": \"texto aleatorio\"}";

        // WHEN
        TabelaImportadaDTO dto = handler.converterParaObjeto(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.getNomeTabela()).isEqualTo("CAIXA");
        log.info("{} [AUDITORIA] Resiliência contra campos desconhecidos validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Robustez: Deve retornar nulo ou lista vazia para JSON truncado")
    void deveTratarJsonTruncado() {
        log.info("{} [TELEMETRIA] Testando robustez contra JSON malformado.", LOG_PREFIX);

        // GIVEN: JSON incompleto
        String raw = "{\"nomeTabela\": \"BRADESCO\", \"taxa\": ";

        // WHEN
        TabelaImportadaDTO dto = handler.converterParaObjeto(raw, TabelaImportadaDTO.class);
        List<TabelaImportadaDTO> lista = handler.converterParaLista(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(dto).isNull();
        assertThat(lista).isEmpty();
        log.info("{} [AUDITORIA] Tratamento de erro de parse validado.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Robustez: Deve retornar string vazia se nenhum delimitador for encontrado")
    void deveRetornarVazioSeNaoHouverDelimitadores() {
        log.info("{} [TELEMETRIA] Testando fallback para texto sem JSON.", LOG_PREFIX);

        // GIVEN: Texto sem chaves ou colchetes
        String raw = "Desculpe, não consegui encontrar dados estruturados no documento.";

        // WHEN
        String extraido = handler.extrairTextoDeJsonBruto(raw);

        // THEN
        assertThat(extraido).isEmpty();
        log.info("{} [AUDITORIA] Fallback de scanner validado.", LOG_PREFIX);
    }
}
