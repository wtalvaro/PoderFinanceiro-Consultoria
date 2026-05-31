package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.util.BancoModelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de Integração de Persistência para BancoRepository.
 * Sincronizado com a estrutura de pacotes do Spring Boot 4.0.6.
 * Valida o mapeamento objeto-relacional e as consultas derivadas no H2.
 */
@DataJpaTest
class BancoRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(BancoRepositoryTest.class);
    private static final String LOG_PREFIX = "[BancoRepositoryTest]";

    @Autowired
    private BancoRepository repository;

    @Autowired
    private TestEntityManager entityManager; // Import corrigido conforme recomendação

    @Test
    @DisplayName("Deve localizar um banco pelo seu código oficial (Business Key)")
    void deveBuscarPorCodigo() {
        log.info("{} [TELEMETRIA] Iniciando teste de busca por Business Key (Código).", LOG_PREFIX);

        // GIVEN
        BancoModel itau = BancoModelBuilder.umBanco().comNome("Itaú").comCodigo("341").build();
        entityManager.persist(itau);
        entityManager.flush();

        // WHEN
        Optional<BancoModel> encontrado = repository.findByCodigo("341");

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Itaú");
        log.info("{} [AUDITORIA] Banco localizado com sucesso via código 341.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar um código que não existe no banco")
    void deveRetornarVazioParaCodigoInexistente() {
        log.info("{} [TELEMETRIA] Testando resiliência para código inexistente.", LOG_PREFIX);

        // WHEN
        Optional<BancoModel> encontrado = repository.findByCodigo("999");

        // THEN
        assertThat(encontrado).isEmpty();
        log.info("{} [AUDITORIA] Fallback para código inexistente validado.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve localizar banco pelo nome ignorando diferenças de caixa (IA Resiliência)")
    void deveBuscarPorNomeIgnoreCase() {
        log.info("{} [TELEMETRIA] Testando busca Case-Insensitive para suporte à IA.", LOG_PREFIX);

        // GIVEN
        BancoModel bradesco = BancoModelBuilder.umBanco().comNome("Bradesco").comCodigo("237").build();
        entityManager.persist(bradesco);
        entityManager.flush();

        // WHEN
        Optional<BancoModel> encontrado = repository.findByNomeIgnoreCase("BRADESCO");

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigo()).isEqualTo("237");
        log.info("{} [AUDITORIA] Busca por nome 'BRADESCO' (uppercase) validada com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve filtrar apenas bancos que possuem o status ativo como verdadeiro")
    void deveRetornarApenasBancosAtivos() {
        log.info("{} [TELEMETRIA] Validando filtro de status ativo.", LOG_PREFIX);

        // GIVEN
        BancoModel ativo = BancoModelBuilder.umBanco().comNome("Ativo").comCodigo("001").build();
        ativo.setAtivo(true);

        BancoModel inativo = BancoModelBuilder.umBanco().comNome("Inativo").comCodigo("002").build();
        inativo.setAtivo(false);

        entityManager.persist(ativo);
        entityManager.persist(inativo);
        entityManager.flush();

        // WHEN
        List<BancoModel> ativos = repository.findByAtivoTrue();

        // THEN
        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).getNome()).isEqualTo("Ativo");
        log.info("{} [AUDITORIA] Filtro de ativos validado. Bancos inativos foram ignorados.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve retornar bancos ativos em ordem alfabética crescente")
    void deveRetornarAtivosOrdenadosPorNome() {
        log.info("{} [TELEMETRIA] Validando ordenação alfabética de bancos ativos.", LOG_PREFIX);

        // GIVEN
        entityManager.persist(BancoModelBuilder.umBanco().comNome("Caixa").comCodigo("104").build());
        entityManager.persist(BancoModelBuilder.umBanco().comNome("BMG").comCodigo("318").build());
        entityManager.persist(BancoModelBuilder.umBanco().comNome("Pan").comCodigo("623").build());
        entityManager.flush();

        // WHEN
        List<BancoModel> ordenados = repository.findByAtivoTrueOrderByNomeAsc();

        // THEN
        assertThat(ordenados)
                .extracting(BancoModel::getNome)
                .containsExactly("BMG", "Caixa", "Pan");

        log.info("{} [AUDITORIA] Ordenação 'BMG -> Caixa -> Pan' validada com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve localizar banco por match aproximado (Suporte a OCR de IA)")
    void deveBuscarPorNomeAproximado() {
        log.info("{} [TELEMETRIA] Testando busca parcial (Containing) para integração com Gemini.", LOG_PREFIX);

        // GIVEN
        entityManager.persist(BancoModelBuilder.umBanco().comNome("Santander Brasil S.A.").comCodigo("033").build());
        entityManager.flush();

        // WHEN
        Optional<BancoModel> encontrado = repository.findFirstByNomeContainingIgnoreCase("santa");

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigo()).isEqualTo("033");
        log.info("{} [AUDITORIA] Match aproximado 'santa' -> 'Santander Brasil S.A.' validado.", LOG_PREFIX);
    }
}
