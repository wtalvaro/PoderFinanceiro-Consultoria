package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.repository.BancoRepository;
import br.com.poderfinanceiro.app.domain.service.TabelaJurosService;
import br.com.poderfinanceiro.app.util.BancoModelBuilder;
import br.com.poderfinanceiro.app.util.TabelaJurosModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de Unidade Gold Standard para TabelaJurosFacadeImpl.
 * Valida a gestão de tabelas e a precisão do motor de busca textual.
 */
class TabelaJurosFacadeTest {

    private TabelaJurosFacadeImpl facade;
    private TabelaJurosService tabelaJurosService;
    private BancoRepository bancoRepository;

    @BeforeEach
    void setUp() {
        tabelaJurosService = mock(TabelaJurosService.class);
        bancoRepository = mock(BancoRepository.class);
        facade = new TabelaJurosFacadeImpl(tabelaJurosService, bancoRepository);
    }

    @Test
    @DisplayName("Deve salvar uma tabela com sucesso quando os dados forem válidos")
    void deveSalvarTabelaComSucesso() {
        // GIVEN
        TabelaJurosModel tabela = TabelaJurosModelBuilder.umaTabela().comNome("Nova Tabela INSS").build();
        when(tabelaJurosService.salvarComRegraDeOuro(any())).thenReturn(tabela);

        // WHEN
        TabelaJurosModel salva = facade.salvarTabela(tabela);

        // THEN
        assertNotNull(salva);
        assertEquals("Nova Tabela INSS", salva.getNomeTabela());
        verify(tabelaJurosService, times(1)).salvarComRegraDeOuro(tabela);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar tabela sem nome")
    void deveFalharAoSalvarSemNome() {
        // GIVEN
        TabelaJurosModel tabelaInvalida = TabelaJurosModelBuilder.umaTabela().comNome("").build();

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> facade.salvarTabela(tabelaInvalida));
        verifyNoInteractions(tabelaJurosService);
    }

    @Test
    @DisplayName("Deve filtrar tabelas por múltiplos critérios (Banco, Nome, Taxa)")
    void deveFiltrarTabelasPorDiversosCampos() {
        // GIVEN
        BancoModel bancoItaú = BancoModelBuilder.umBanco().comNome("Itaú").build();
        BancoModel bancoPan = BancoModelBuilder.umBanco().comNome("Banco Pan").build();

        TabelaJurosModel t1 = TabelaJurosModelBuilder.umaTabela()
                .comNome("INSS NOVO")
                .comBanco(bancoItaú)
                .comTaxa("1.80")
                .build();

        TabelaJurosModel t2 = TabelaJurosModelBuilder.umaTabela()
                .comNome("SIAPE PORTABILIDADE")
                .comBanco(bancoPan)
                .comTaxa("1.55")
                .build();

        when(tabelaJurosService.listarAtivas()).thenReturn(List.of(t1, t2));

        // WHEN & THEN
        // 1. Busca por nome do Banco
        assertEquals(1, facade.filtrarTabelas("itaú").size());
        // 2. Busca por nome da Tabela
        assertEquals(1, facade.filtrarTabelas("portabilidade").size());
        // 3. Busca por Taxa (Testando utilitário de formatação interno)
        assertEquals(1, facade.filtrarTabelas("1,80").size());
        // 4. Busca vazia
        assertEquals(2, facade.filtrarTabelas("").size());
    }

    @Test
    @DisplayName("Deve delegar o arquivamento para o serviço de domínio")
    void deveArquivarTabela() {
        // GIVEN
        TabelaJurosModel tabela = TabelaJurosModelBuilder.umaTabela().comId(100L).build();

        // WHEN
        facade.arquivarTabela(tabela);

        // THEN
        verify(tabelaJurosService, times(1)).arquivarTabela(tabela);
    }

    @Test
    @DisplayName("Deve listar apenas bancos ativos para vínculo")
    void deveListarBancosAtivos() {
        // GIVEN
        when(bancoRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(new BancoModel()));

        // WHEN
        List<BancoModel> bancos = facade.listarBancosAtivos();

        // THEN
        assertNotNull(bancos);
        verify(bancoRepository).findByAtivoTrueOrderByNomeAsc();
    }
}
