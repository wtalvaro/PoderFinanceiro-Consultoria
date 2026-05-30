package br.com.poderfinanceiro.app.common.util;

import br.com.poderfinanceiro.app.domain.model.enums.TipoLogradouroModel;
import br.com.poderfinanceiro.app.domain.model.enums.UfModel;
import br.com.poderfinanceiro.app.presentation.viewmodel.EnderecoViewModel;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("[Common] Teste de Unidade - EnderecoUtils")
class EnderecoUtilsTest {

    @BeforeAll
    static void initJFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // Toolkit já inicializado
        }
    }

    @Test
    @DisplayName("Deve limpar CEP removendo caracteres especiais")
    void deveLimparCep() {
        assertThat(EnderecoUtils.limparCep("01.234-567")).isEqualTo("01234567");
        assertThat(EnderecoUtils.limparCep(null)).isEmpty();
    }

    @Test
    @DisplayName("Deve formatar CEP válido com pontos e traços")
    void deveFormatarCepValido() {
        assertThat(EnderecoUtils.formatarCep("01234567")).isEqualTo("01.234-567");
    }

    @Test
    @DisplayName("Deve montar endereço completo formatado para WhatsApp")
    void deveMontarEnderecoCompleto() {
        // GIVEN
        EnderecoViewModel vm = Mockito.mock(EnderecoViewModel.class);

        // Mockando as properties com os tipos corretos do domínio
        StringProperty logradouro = new SimpleStringProperty("AVENIDA PAULISTA");
        StringProperty numero = new SimpleStringProperty("1000");
        StringProperty bairro = new SimpleStringProperty("BELA VISTA");
        StringProperty cidade = new SimpleStringProperty("SÃO PAULO");
        StringProperty cep = new SimpleStringProperty("01310100");
        StringProperty complemento = new SimpleStringProperty("SALA 10");

        // Correção dos tipos: ObjectProperty usando os Enums do projeto
        ObjectProperty<UfModel> uf = new SimpleObjectProperty<>(UfModel.SP);
        ObjectProperty<TipoLogradouroModel> tipo = new SimpleObjectProperty<>(TipoLogradouroModel.AVENIDA);

        when(vm.logradouroProperty()).thenReturn(logradouro);
        when(vm.numeroProperty()).thenReturn(numero);
        when(vm.bairroProperty()).thenReturn(bairro);
        when(vm.cidadeProperty()).thenReturn(cidade);
        when(vm.ufProperty()).thenReturn(uf); // Agora compatível
        when(vm.cepProperty()).thenReturn(cep);
        when(vm.complementoProperty()).thenReturn(complemento);
        when(vm.tipoLogradouroProperty()).thenReturn(tipo); // Agora compatível

        // WHEN
        String resultado = EnderecoUtils.montarEnderecoCompleto(vm);

        // THEN
        assertThat(resultado).contains("AVENIDA PAULISTA", "1000", "SALA 10", "BELA VISTA", "01.310-100");
        assertThat(resultado).contains("*Bairro:*", "*Cidade:*");
        assertThat(resultado).contains("SP");
    }

    @Test
    @DisplayName("Deve retornar mensagem padrão para endereço incompleto")
    void deveTratarEnderecoIncompleto() {
        assertThat(EnderecoUtils.montarEnderecoCompleto(null)).isEqualTo("Endereço não informado.");
    }
}
