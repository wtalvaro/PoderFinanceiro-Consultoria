package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.EnderecoProponenteModel;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
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
 * Teste de Integração para ProponenteRepository.
 * Valida o isolamento de carteira por usuário, integridade de CPF e buscas
 * complexas.
 * Sincronizado com Spring Boot 4.0.6 e Java 25.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class ProponenteRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(ProponenteRepositoryTest.class);
    private static final String LOG_PREFIX = "[ProponenteRepositoryTest]";

    @Autowired
    private ProponenteRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private UsuarioModel consultorA;
    private UsuarioModel consultorB;

    @BeforeEach
    void setup() {
        log.info("{} [SISTEMA] Preparando ambiente de teste de proponentes.", LOG_PREFIX);

        // Criando consultores para testar isolamento
        consultorA = criarUsuario("wagner_pf", "wagner@poder.com");
        consultorB = criarUsuario("solange_pf", "solange@poder.com");

        entityManager.persist(consultorA);
        entityManager.persist(consultorB);
        entityManager.flush();
    }

    @Test
    @DisplayName("Deve localizar proponente por CPF e Usuário garantindo isolamento")
    void deveBuscarPorCpfEUsuario() {
        log.info("{} [TELEMETRIA] Testando busca por CPF com isolamento de usuário.", LOG_PREFIX);

        // GIVEN
        ProponenteModel p1 = ProponenteModelBuilder.umProponente().comCpf("12345678901").build();
        p1.setUsuario(consultorA);
        entityManager.persist(p1);
        entityManager.flush();

        // WHEN
        Optional<ProponenteModel> encontrado = repository.findByCpfAndUsuarioIdAndDeletadoEmIsNull("12345678901",
                consultorA.getId());
        Optional<ProponenteModel> naoEncontrado = repository.findByCpfAndUsuarioIdAndDeletadoEmIsNull("12345678901",
                consultorB.getId());

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(naoEncontrado).isEmpty();
        log.info("{} [AUDITORIA] Isolamento de CPF por consultor validado.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve executar busca rápida por parte do nome ou CPF")
    void deveExecutarBuscaRapida() {
        log.info("{} [TELEMETRIA] Testando query JPQL de busca rápida.", LOG_PREFIX);

        // GIVEN
        ProponenteModel p = ProponenteModelBuilder.umProponente().comNome("CARLOS ALBERTO").comCpf("98765432100")
                .build();
        p.setUsuario(consultorA);
        entityManager.persist(p);
        entityManager.flush();

        // WHEN
        List<ProponenteModel> porNome = repository.buscarRapidaPorNomeOuCpf("alberto", consultorA.getId());
        List<ProponenteModel> porCpf = repository.buscarRapidaPorNomeOuCpf("987654", consultorA.getId());

        // THEN
        assertThat(porNome).hasSize(1);
        assertThat(porCpf).hasSize(1);
        log.info("{} [AUDITORIA] Busca rápida multi-campo validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve verificar duplicidade de CPF ignorando o ID do próprio registro")
    void deveValidarDuplicidadeIgnorandoIdProprio() {
        log.info("{} [TELEMETRIA] Testando regra de validação de duplicidade em edição.", LOG_PREFIX);

        // GIVEN
        ProponenteModel p = ProponenteModelBuilder.umProponente().comCpf("11122233344").build();
        p.setUsuario(consultorA);
        entityManager.persist(p);
        entityManager.flush();

        // WHEN
        // Existe outro registro com esse CPF para esse usuário? (Ignorando o ID do p)
        boolean existeOutro = repository.existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull("11122233344",
                consultorA.getId(), p.getId());

        // THEN
        assertThat(existeOutro).isFalse();
        log.info("{} [AUDITORIA] Lógica de exclusão de ID na validação de duplicidade validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve carregar proponente com endereços usando JOIN FETCH")
    void deveCarregarComEnderecos() {
        log.info("{} [TELEMETRIA] Validando carregamento ansioso de endereços.", LOG_PREFIX);

        // GIVEN
        ProponenteModel p = ProponenteModelBuilder.umProponente().build();
        p.setUsuario(consultorA);

        EnderecoProponenteModel end = new EnderecoProponenteModel();
        end.setCep("01001000");
        end.setLogradouro("Rua Teste");
        end.setNumero("10");
        end.setCidade("São Paulo");
        end.setBairro("Centro");
        end.setProponente(p);
        p.getEnderecos().add(end);

        entityManager.persist(p);
        entityManager.flush();
        entityManager.clear(); // Limpa cache para forçar o JOIN FETCH

        // WHEN
        Optional<ProponenteModel> encontrado = repository.findByIdWithEnderecos(p.getId());

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEnderecos()).isNotEmpty();
        assertThat(encontrado.get().getEnderecos().get(0).getLogradouro()).isEqualTo("Rua Teste");
        log.info("{} [AUDITORIA] JOIN FETCH de endereços validado com sucesso.", LOG_PREFIX);
    }

    private UsuarioModel criarUsuario(String username, String email) {
        UsuarioModel u = new UsuarioModel();
        u.setUsername(username);
        u.setNome("Consultor " + username);
        u.setEmail(email);
        u.setSenhaHash("hash_fake");
        u.setAtivo(true);
        return u;
    }
}
