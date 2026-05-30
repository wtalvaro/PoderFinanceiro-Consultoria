package br.com.poderfinanceiro.app.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.poderfinanceiro.app.util.BancoModelBuilder;

import static org.junit.jupiter.api.Assertions.*;

class BancoModelTest {

    @Test
    @DisplayName("Deve considerar bancos iguais se possuírem o mesmo código, mesmo sem ID")
    void deveValidarIgualdadePorChaveDeNegocio() {
        // GIVEN
        BancoModel b1 = BancoModelBuilder.umBanco().comNome("Itaú").comCodigo("341").build();
        BancoModel b2 = BancoModelBuilder.umBanco().comNome("Itaú Unibanco").comCodigo("341").build();

        // THEN
        assertEquals(b1, b2, "Bancos com o mesmo código devem ser considerados iguais (Business Key).");
        assertEquals(b1.hashCode(), b2.hashCode(), "Hashes devem ser idênticos para chaves de negócio iguais.");
    }

    @Test
    @DisplayName("Deve considerar bancos diferentes se possuírem códigos diferentes")
    void deveValidarDiferencaPorChaveDeNegocio() {
        // GIVEN
        BancoModel b1 = BancoModelBuilder.umBanco().comCodigo("001").build();
        BancoModel b2 = BancoModelBuilder.umBanco().comCodigo("033").build();

        // THEN
        assertNotEquals(b1, b2);
    }
}
