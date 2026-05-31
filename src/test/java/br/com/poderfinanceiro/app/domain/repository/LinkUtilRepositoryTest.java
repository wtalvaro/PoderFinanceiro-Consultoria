package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.model.enums.CategoriaLinkModel;
import br.com.poderfinanceiro.app.util.LinkUtilModelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de Integração para LinkUtilRepository.
 * Valida a ordenação por categoria e a lógica de busca contextual por tags.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class LinkUtilRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(LinkUtilRepositoryTest.class);
    private static final String LOG_PREFIX = "[LinkUtilRepositoryTest]";

    @Autowired
    private LinkUtilRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Deve listar links ordenados por categoria e depois por título")
    void deveListarOrdenado() {
        log.info("{} [TELEMETRIA] Iniciando teste de ordenação multi-nível.", LOG_PREFIX);

        // GIVEN: Inserção fora de ordem
        entityManager
                .persist(LinkUtilModelBuilder.umLink().comTitulo("Z").comCategoria(CategoriaLinkModel.BANCO).build());
        entityManager
                .persist(LinkUtilModelBuilder.umLink().comTitulo("A").comCategoria(CategoriaLinkModel.BANCO).build());
        entityManager
                .persist(LinkUtilModelBuilder.umLink().comTitulo("B").comCategoria(CategoriaLinkModel.GOVERNO).build());
        entityManager.flush();

        // WHEN
        List<LinkUtilModel> links = repository.findAllByOrderByCategoriaAscTituloAsc();

        // THEN: Ordem esperada: BANCO A -> BANCO Z -> GOVERNO B
        assertThat(links).hasSize(3);
        assertThat(links.get(0).getTitulo()).isEqualTo("A");
        assertThat(links.get(1).getTitulo()).isEqualTo("Z");
        assertThat(links.get(2).getTitulo()).isEqualTo("B");
        log.info("{} [AUDITORIA] Ordenação por Categoria e Título validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve buscar links contextuais baseados em tags de banco ou convênio")
    void deveBuscarLinksContextuais() {
        log.info("{} [TELEMETRIA] Testando query customizada de busca por tags.", LOG_PREFIX);

        // GIVEN
        entityManager.persist(LinkUtilModelBuilder.umLink().comTitulo("Portal INSS").comTags("inss, oficial").build());
        entityManager.persist(LinkUtilModelBuilder.umLink().comTitulo("Portal Itaú").comTags("itau, banco").build());
        entityManager.persist(LinkUtilModelBuilder.umLink().comTitulo("Outro").comTags("diversos").build());
        entityManager.flush();

        // WHEN: Buscamos por "itau" ou "inss" (Simulando contexto de uma proposta)
        List<LinkUtilModel> resultados = repository.buscarLinksContextuais("itau", "inss");

        // THEN
        assertThat(resultados).hasSize(2);
        assertThat(resultados).extracting(LinkUtilModel::getTitulo)
                .containsExactly("Portal INSS", "Portal Itaú"); // Ordenado por título ASC na query
        log.info("{} [AUDITORIA] Busca contextual validada com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve buscar por título ignorando maiúsculas/minúsculas (Busca Rápida)")
    void deveBuscarPorTitulo() {
        log.info("{} [TELEMETRIA] Testando busca rápida Case-Insensitive.", LOG_PREFIX);

        // GIVEN
        entityManager.persist(LinkUtilModelBuilder.umLink().comTitulo("Calculadora Price").build());
        entityManager.flush();

        // WHEN
        List<LinkUtilModel> resultados = repository.findByTituloContainingIgnoreCase("PRICE");

        // THEN
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getTitulo()).isEqualTo("Calculadora Price");
        log.info("{} [AUDITORIA] Busca rápida validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve filtrar links por uma categoria específica")
    void deveFiltrarPorCategoria() {
        // GIVEN
        entityManager.persist(LinkUtilModelBuilder.umLink().comCategoria(CategoriaLinkModel.BANCO).build());
        entityManager.persist(LinkUtilModelBuilder.umLink().comCategoria(CategoriaLinkModel.CONSULTA).build());
        entityManager.flush();

        // WHEN
        List<LinkUtilModel> bancos = repository.findByCategoriaOrderByTituloAsc(CategoriaLinkModel.BANCO);

        // THEN
        assertThat(bancos).hasSize(1);
        assertThat(bancos.get(0).getCategoria()).isEqualTo(CategoriaLinkModel.BANCO);
    }
}
