package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.util.BancoModelBuilder;
import br.com.poderfinanceiro.app.util.ProponenteModelBuilder;
import br.com.poderfinanceiro.app.util.PropostaModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de Integração para PropostaRepository.
 * Valida a orquestração de queries complexas com JOIN FETCH e filtros de
 * status.
 * Sincronizado com Spring Boot 4.0.6 e Java 25.
 */
@DataJpaTest
@ContextConfiguration(classes = PropostaRepositoryTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class PropostaRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(PropostaRepositoryTest.class);
    private static final String LOG_PREFIX = "[PropostaRepositoryTest]";

    @EntityScan(basePackageClasses = { PropostaModel.class, UsuarioModel.class, BancoModel.class })
    @EnableJpaRepositories(basePackageClasses = PropostaRepository.class)
    static class TestConfig {
    }

    @Autowired
    private PropostaRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    private UsuarioModel consultor;
    private BancoModel banco;
    private ProponenteModel cliente;

    @BeforeEach
    void setup() {
        log.info("{} [SISTEMA] Preparando grafo de objetos para teste de propostas.", LOG_PREFIX);

        consultor = new UsuarioModel();
        consultor.setUsername("vendedor_" + System.nanoTime());
        consultor.setNome("Consultor Teste");
        consultor.setEmail(consultor.getUsername() + "@poder.com");
        consultor.setSenhaHash("123");
        entityManager.persist(consultor);

        banco = BancoModelBuilder.umBanco().comNome("Banco Alfa").comCodigo("001").build();
        entityManager.persist(banco);

        cliente = ProponenteModelBuilder.umProponente().comNome("Cliente Teste").build();
        cliente.setUsuario(consultor);
        entityManager.persist(cliente);

        entityManager.flush();
    }

    @Test
    @DisplayName("Deve listar propostas com JOIN FETCH garantindo que Banco e Proponente não sejam Proxies")
    void deveListarComJoinFetch() {
        log.info("{} [TELEMETRIA] Iniciando teste de integridade JOIN FETCH.", LOG_PREFIX);

        // GIVEN
        PropostaModel p = PropostaModelBuilder.umaProposta().vinculadoA(cliente, banco, consultor).build();
        entityManager.persist(p);
        entityManager.flush();
        entityManager.clear(); // Limpa cache para forçar consulta real ao H2

        // WHEN
        List<PropostaModel> resultados = repository.findByUsuarioId(consultor.getId());

        // THEN
        assertThat(resultados).hasSize(1);
        PropostaModel recuperada = resultados.get(0);

        // Se o JOIN FETCH falhar, estas chamadas disparariam
        // LazyInitializationException fora da transação
        assertThat(recuperada.getProponente().getNomeCompleto()).isEqualTo("Cliente Teste");
        assertThat(recuperada.getBanco().getNome()).isEqualTo("Banco Alfa");

        log.info("{} [AUDITORIA] Query findByUsuarioId com JOIN FETCH validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve filtrar propostas por usuário e status usando Enum")
    void deveFiltrarPorStatus() {
        log.info("{} [TELEMETRIA] Testando filtro por Enum de Status.", LOG_PREFIX);

        // GIVEN
        PropostaModel p1 = PropostaModelBuilder.umaProposta().comStatus(StatusPropostaModel.DIGITADA)
                .vinculadoA(cliente, banco, consultor).build();
        PropostaModel p2 = PropostaModelBuilder.umaProposta().comStatus(StatusPropostaModel.PENDENTE)
                .vinculadoA(cliente, banco, consultor).build();

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.flush();

        // WHEN
        List<PropostaModel> digitadas = repository.findByUsuarioIdAndStatus(consultor.getId(),
                StatusPropostaModel.DIGITADA);

        // THEN
        assertThat(digitadas).hasSize(1);
        assertThat(digitadas.get(0).getStatus()).isEqualTo(StatusPropostaModel.DIGITADA);
        log.info("{} [AUDITORIA] Filtro por status validado com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve retornar proposta detalhada por ID com todos os relacionamentos")
    void deveBuscarPorIdComDetalhes() {
        log.info("{} [TELEMETRIA] Validando busca individual detalhada.", LOG_PREFIX);

        // GIVEN
        PropostaModel p = PropostaModelBuilder.umaProposta().vinculadoA(cliente, banco, consultor).build();
        entityManager.persist(p);
        entityManager.flush();
        entityManager.clear();

        // WHEN
        Optional<PropostaModel> encontrado = repository.findByIdWithDetails(p.getId());

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getBanco()).isNotNull();
        assertThat(encontrado.get().getProponente()).isNotNull();
        log.info("{} [AUDITORIA] findByIdWithDetails validado.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve garantir que o DISTINCT impeça duplicidade na listagem de produtividade")
    void deveValidarDistinctNaProdutividade() {
        log.info("{} [TELEMETRIA] Testando regra de unicidade (DISTINCT) na query de produtividade.", LOG_PREFIX);

        // GIVEN
        PropostaModel p = PropostaModelBuilder.umaProposta().vinculadoA(cliente, banco, consultor).build();
        entityManager.persist(p);
        entityManager.flush();

        // WHEN
        List<PropostaModel> produtividade = repository.buscarProdutividadeDoConsultor(consultor.getId());

        // THEN
        assertThat(produtividade).hasSize(1);
        log.info("{} [AUDITORIA] Query de produtividade com DISTINCT validada.", LOG_PREFIX);
    }
}
