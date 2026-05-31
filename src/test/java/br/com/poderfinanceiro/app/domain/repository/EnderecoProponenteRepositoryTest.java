package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.util.EnderecoProponenteModelBuilder;
import br.com.poderfinanceiro.app.util.ProponenteModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de Integração para EnderecoProponenteRepository.
 * Valida a persistência de localização e vínculos com proponentes.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class EnderecoProponenteRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(EnderecoProponenteRepositoryTest.class);
    private static final String LOG_PREFIX = "[EnderecoRepositoryTest]";

    @Autowired
    private EnderecoProponenteRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    private ProponenteModel proponente;

    @BeforeEach
    void setup() {
        log.info("{} [SISTEMA] Preparando cenário de teste de endereço.", LOG_PREFIX);

        // Setup de dependências obrigatórias (Consultor -> Proponente)
        UsuarioModel consultor = new UsuarioModel();
        consultor.setUsername("atendente_" + System.nanoTime());
        consultor.setNome("Atendente Teste");
        consultor.setEmail(consultor.getUsername() + "@poder.com");
        consultor.setSenhaHash("123");
        entityManager.persist(consultor);

        proponente = ProponenteModelBuilder.umProponente().build();
        proponente.setUsuario(consultor);
        entityManager.persist(proponente);

        entityManager.flush();
    }

    @Test
    @DisplayName("Deve localizar o endereço vinculado a um proponente pelo ID do proponente")
    void deveBuscarPorProponenteId() {
        log.info("{} [TELEMETRIA] Iniciando busca de endereço por ID de proponente.", LOG_PREFIX);

        // GIVEN
        EnderecoProponenteModel endereco = EnderecoProponenteModelBuilder.umEndereco()
                .vinculadoA(proponente)
                .build();
        entityManager.persist(endereco);
        entityManager.flush();

        // WHEN
        Optional<EnderecoProponenteModel> encontrado = repository.findByProponenteId(proponente.getId());

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCep()).isEqualTo("01001000");
        log.info("{} [AUDITORIA] Endereço localizado com sucesso para o proponente ID: {}", LOG_PREFIX,
                proponente.getId());
    }

    @Test
    @DisplayName("Deve verificar corretamente a existência de endereço para um proponente")
    void deveValidarExistencia() {
        log.info("{} [TELEMETRIA] Testando predicado de existência de endereço.", LOG_PREFIX);

        // GIVEN
        EnderecoProponenteModel endereco = EnderecoProponenteModelBuilder.umEndereco()
                .vinculadoA(proponente)
                .build();
        entityManager.persist(endereco);
        entityManager.flush();

        // WHEN & THEN
        assertThat(repository.existsByProponenteId(proponente.getId())).isTrue();
        assertThat(repository.existsByProponenteId(999L)).isFalse();

        log.info("{} [AUDITORIA] Lógica de existência validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve listar endereços ordenados pela data de criação decrescente")
    void deveListarOrdenadoPorData() throws InterruptedException {
        log.info("{} [TELEMETRIA] Validando ordenação cronológica de endereços.", LOG_PREFIX);

        // GIVEN: Dois endereços com timestamps diferentes
        EnderecoProponenteModel e1 = EnderecoProponenteModelBuilder.umEndereco()
                .comCep("11111111").vinculadoA(proponente).build();
        entityManager.persist(e1);
        entityManager.flush();

        Thread.sleep(10); // Garante diferença no timestamp

        EnderecoProponenteModel e2 = EnderecoProponenteModelBuilder.umEndereco()
                .comCep("22222222").vinculadoA(proponente).build();
        entityManager.persist(e2);
        entityManager.flush();

        // WHEN
        List<EnderecoProponenteModel> resultados = repository.findAllByProponenteOrderByCriadoEmDesc(proponente);

        // THEN
        assertThat(resultados).hasSize(2);
        assertThat(resultados.get(0).getCep()).isEqualTo("22222222"); // O mais recente primeiro
        log.info("{} [AUDITORIA] Ordenação decrescente validada com sucesso.", LOG_PREFIX);
    }
}
