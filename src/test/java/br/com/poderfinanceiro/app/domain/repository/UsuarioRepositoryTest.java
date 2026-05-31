package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.util.UsuarioModelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Teste de Integração para UsuarioRepository.
 * Valida a segurança de acesso e unicidade de credenciais.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class UsuarioRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(UsuarioRepositoryTest.class);
    private static final String LOG_PREFIX = "[UsuarioRepositoryTest]";

    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Deve localizar usuário ativo pelo username para fins de Login")
    void deveBuscarUsuarioAtivoParaLogin() {
        log.info("{} [TELEMETRIA] Iniciando teste de busca para autenticação.", LOG_PREFIX);

        // GIVEN
        UsuarioModel ativo = UsuarioModelBuilder.umUsuario().comUsername("admin").build();
        UsuarioModel inativo = UsuarioModelBuilder.umUsuario().comUsername("bloqueado").inativo().build();

        entityManager.persist(ativo);
        entityManager.persist(inativo);
        entityManager.flush();

        // WHEN
        Optional<UsuarioModel> encontrado = repository.findByUsernameAndAtivoTrue("admin");
        Optional<UsuarioModel> naoEncontrado = repository.findByUsernameAndAtivoTrue("bloqueado");

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("admin");
        assertThat(naoEncontrado).isEmpty();

        log.info("{} [AUDITORIA] Lógica de login (Ativo=True) validada com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve localizar usuário pelo username independente do status (Check de Cadastro)")
    void deveBuscarPorUsername() {
        log.info("{} [TELEMETRIA] Testando busca por username para validação de duplicidade.", LOG_PREFIX);

        // GIVEN
        UsuarioModel user = UsuarioModelBuilder.umUsuario().comUsername("solange").build();
        entityManager.persist(user);
        entityManager.flush();

        // WHEN
        Optional<UsuarioModel> encontrado = repository.findByUsername("solange");

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).contains("Wagner"); // Nome padrão do builder
        log.info("{} [AUDITORIA] Busca por username validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve localizar usuário pelo e-mail (Recuperação/Cadastro)")
    void deveBuscarPorEmail() {
        log.info("{} [TELEMETRIA] Testando busca por e-mail.", LOG_PREFIX);

        // GIVEN
        String email = "contato@poder.com.br";
        UsuarioModel user = UsuarioModelBuilder.umUsuario().comEmail(email).build();
        entityManager.persist(user);
        entityManager.flush();

        // WHEN
        Optional<UsuarioModel> encontrado = repository.findByEmail(email);

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail()).isEqualTo(email);
        log.info("{} [AUDITORIA] Busca por e-mail validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve validar a unicidade do username no banco de dados")
    void deveFalharAoPersistirUsernameDuplicado() {
        log.info("{} [TELEMETRIA] Testando constraint de UNIQUE no username.", LOG_PREFIX);

        // GIVEN
        UsuarioModel u1 = UsuarioModelBuilder.umUsuario().comUsername("duplicado").build();
        entityManager.persist(u1);
        entityManager.flush();

        UsuarioModel u2 = UsuarioModelBuilder.umUsuario().comUsername("duplicado").comEmail("outro@email.com").build();

        // WHEN & THEN
        try {
            entityManager.persist(u2);
            entityManager.flush();
            fail("Deveria ter lançado exceção de violação de constraint UNIQUE");
        } catch (Exception e) {
            log.info("{} [AUDITORIA] Constraint de UNIQUE username validada pelo H2.", LOG_PREFIX);
        }
    }
}
