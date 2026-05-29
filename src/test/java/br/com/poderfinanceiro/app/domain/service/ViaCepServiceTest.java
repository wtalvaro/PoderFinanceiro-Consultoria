package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.ViaCepResponse;
import br.com.poderfinanceiro.app.infrastructure.client.ViaCepClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <h1>ViaCepServiceTest</h1>
 * <p>
 * Testes de Unidade para o serviço de busca de CEP.
 * Valida o saneamento de dados, validações de formato e tratamento de respostas
 * da API.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ViaCepServiceTest {

    private ViaCepService service;

    @Mock
    private ViaCepClient viaCepClient;

    @BeforeEach
    void setUp() {
        this.service = new ViaCepService(viaCepClient);
    }

    @Test
    @DisplayName("Deve retornar null quando o CEP for nulo ou vazio")
    void deveRetornarNullParaCepInvalido() {
        assertThat(service.buscarEnderecoPorCep(null)).isNull();
        assertThat(service.buscarEnderecoPorCep("")).isNull();
        assertThat(service.buscarEnderecoPorCep("   ")).isNull();

        // Garante que o cliente de rede sequer foi chamado
        verifyNoInteractions(viaCepClient);
    }

    @Test
    @DisplayName("Deve retornar null quando o CEP tiver tamanho inválido após limpeza")
    void deveValidarTamanhoDoCep() {
        assertThat(service.buscarEnderecoPorCep("123")).isNull();
        assertThat(service.buscarEnderecoPorCep("123456789")).isNull();

        verifyNoInteractions(viaCepClient);
    }

    @Test
    @DisplayName("Deve sanear o CEP removendo máscaras antes de enviar ao cliente")
    void deveSanearCepComMascara() {
        String cepComMascara = "24900-000";
        String cepLimpo = "24900000";

        ViaCepResponse mockResponse = new ViaCepResponse(cepLimpo, "Rua A", "", "Bairro B", "Cidade C", "RJ", false);
        when(viaCepClient.getEndereco(cepLimpo)).thenReturn(mockResponse);

        service.buscarEnderecoPorCep(cepComMascara);

        // Verifica se o cliente recebeu o CEP sem o hífen
        verify(viaCepClient).getEndereco(cepLimpo);
    }

    @Test
    @DisplayName("Deve retornar null quando a API informar que o CEP não existe")
    void deveTratarCepInexistente() {
        String cep = "99999999";
        // ViaCEP retorna 200 OK mas com a flag erro=true para CEPs válidos mas não
        // cadastrados
        ViaCepResponse mockResponse = new ViaCepResponse(null, null, null, null, null, null, true);

        when(viaCepClient.getEndereco(cep)).thenReturn(mockResponse);

        ViaCepResponse resultado = service.buscarEnderecoPorCep(cep);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("Deve retornar o endereço completo quando o CEP for válido e localizado")
    void deveRetornarEnderecoComSucesso() {
        String cep = "24900000";
        ViaCepResponse mockResponse = new ViaCepResponse(cep, "Rua das Flores", "Apto 101", "Centro", "Maricá", "RJ",
                false);

        when(viaCepClient.getEndereco(cep)).thenReturn(mockResponse);

        ViaCepResponse resultado = service.buscarEnderecoPorCep(cep);

        assertThat(resultado).isNotNull();
        assertThat(resultado.logradouro()).isEqualTo("Rua das Flores");
        assertThat(resultado.localidade()).isEqualTo("Maricá");
        assertThat(resultado.uf()).isEqualTo("RJ");
    }

    @Test
    @DisplayName("Deve retornar null e logar erro quando o cliente de infraestrutura falhar")
    void deveTratarExcecaoNoCliente() {
        when(viaCepClient.getEndereco(anyString())).thenThrow(new RuntimeException("Timeout"));

        ViaCepResponse resultado = service.buscarEnderecoPorCep("24900000");

        assertThat(resultado).isNull();
    }
}
