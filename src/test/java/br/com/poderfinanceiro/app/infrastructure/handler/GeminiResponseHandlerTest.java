package br.com.poderfinanceiro.app.infrastructure.handler;

import br.com.poderfinanceiro.app.application.dto.GeminiResponse;
import br.com.poderfinanceiro.app.application.dto.TabelaImportadaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Infra] Teste de Unidade - GeminiResponseHandler")
class GeminiResponseHandlerTest {

    private GeminiResponseHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GeminiResponseHandler();
    }

    @Test
    @DisplayName("Deve extrair texto profundo da estrutura GeminiResponse")
    void deveExtrairTextoDaEstruturaGemini() {
        // GIVEN: Montagem manual da estrutura aninhada do Record GeminiResponse
        GeminiResponse.Part part = new GeminiResponse.Part("Conteúdo da IA");
        GeminiResponse.Content content = new GeminiResponse.Content(List.of(part));
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate(content);
        GeminiResponse response = new GeminiResponse(List.of(candidate));

        // WHEN
        String texto = handler.extrairTexto(response);

        // THEN
        assertThat(texto).isEqualTo("Conteúdo da IA");
    }

    @Test
    @DisplayName("Scanner Agressivo: Deve extrair JSON de dentro de blocos Markdown")
    void deveExtrairJsonDeMarkdown() {
        // GIVEN: Resposta com ruído de Markdown
        String raw = "Aqui está o resultado:\n```json\n{\"banco\": \"ITAU\", \"taxaMensal\": 1.66}\n```\nEspero que ajude.";

        // WHEN
        String jsonPuro = handler.extrairTextoDeJsonBruto(raw);

        // THEN
        assertThat(jsonPuro).isEqualTo("{\"banco\": \"ITAU\", \"taxaMensal\": 1.66}");
    }

    @Test
    @DisplayName("Conversão: Deve converter JSON sujo para DTO TabelaImportadaDTO")
    void deveConverterParaObjetoComSucesso() {
        // GIVEN
        String raw = "O banco identificado foi: {\"banco\": \"SANTANDER\", \"prazoMaximo\": 84}";

        // WHEN
        TabelaImportadaDTO dto = handler.converterParaObjeto(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.getBanco()).isEqualTo("SANTANDER");
        assertThat(dto.getPrazoMaximo()).isEqualTo(84);
    }

    @Test
    @DisplayName("Lote: Deve converter lista JSON com ruído para List de DTOs")
    void deveConverterParaListaComSucesso() {
        // GIVEN: Lista com texto antes e depois
        String raw = "Resultados: [{\"banco\": \"PAN\"}, {\"banco\": \"BMG\"}] - Fim da lista.";

        // WHEN
        List<TabelaImportadaDTO> lista = handler.converterParaLista(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getBanco()).isEqualTo("PAN");
        assertThat(lista.get(1).getBanco()).isEqualTo("BMG");
    }

    @Test
    @DisplayName("Resiliência: Deve ignorar propriedades desconhecidas enviadas pela IA")
    void deveIgnorarPropriedadesDesconhecidas() {
        // GIVEN: JSON com campo 'alucinacao_ia' que não existe no DTO
        String raw = "{\"banco\": \"CAIXA\", \"alucinacao_ia\": \"texto aleatorio\"}";

        // WHEN
        TabelaImportadaDTO dto = handler.converterParaObjeto(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.getBanco()).isEqualTo("CAIXA");
    }

    @Test
    @DisplayName("Robustez: Deve retornar nulo ou lista vazia para JSON truncado")
    void deveTratarJsonTruncado() {
        // GIVEN: JSON incompleto
        String raw = "{\"banco\": \"BRADESCO\", \"taxa\": ";

        // WHEN
        TabelaImportadaDTO dto = handler.converterParaObjeto(raw, TabelaImportadaDTO.class);
        List<TabelaImportadaDTO> lista = handler.converterParaLista(raw, TabelaImportadaDTO.class);

        // THEN
        assertThat(dto).isNull();
        assertThat(lista).isEmpty();
    }

    @Test
    @DisplayName("Robustez: Deve retornar string vazia se nenhum delimitador for encontrado")
    void deveRetornarVazioSeNaoHouverDelimitadores() {
        // GIVEN: Texto sem chaves ou colchetes
        String raw = "Desculpe, não consegui encontrar dados estruturados no documento.";

        // WHEN
        String extraido = handler.extrairTextoDeJsonBruto(raw);

        // THEN
        assertThat(extraido).isEmpty();
    }
}
