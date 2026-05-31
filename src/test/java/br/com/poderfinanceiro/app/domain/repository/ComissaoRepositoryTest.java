package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.util.BancoModelBuilder;
import br.com.poderfinanceiro.app.util.ComissaoModelBuilder;
import br.com.poderfinanceiro.app.util.ProponenteModelBuilder;
import br.com.poderfinanceiro.app.util.PropostaModelBuilder;
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
 * Teste de Integração para ComissaoRepository.
 * Blindado contra erros de conversão de Enum do PostgreSQL no H2.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        // SOLUÇÃO DEFINITIVA: Força o Hibernate a tratar Enums como VARCHAR no H2
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class ComissaoRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(ComissaoRepositoryTest.class);
    private static final String LOG_PREFIX = "[ComissaoRepositoryTest]";

    @Autowired
    private ComissaoRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Deve listar comissões de um consultor filtrando pelo status de pagamento")
    void deveFiltrarPorUsuarioEStatus() {
        log.info("{} [TELEMETRIA] Iniciando teste de filtro financeiro.", LOG_PREFIX);

        // GIVEN: Setup do grafo completo para satisfazer as constraints (Fix: USERNAME)
        UsuarioModel consultor = criarUsuarioLimpo("consultor_vendas");
        entityManager.persist(consultor);

        BancoModel banco = BancoModelBuilder.umBanco().build();
        entityManager.persist(banco);

        ProponenteModel cliente = ProponenteModelBuilder.umProponente().build();
        cliente.setUsuario(consultor);
        entityManager.persist(cliente);

        PropostaModel proposta = PropostaModelBuilder.umaProposta()
                .vinculadoA(cliente, banco, consultor)
                .build();
        entityManager.persist(proposta);

        ComissaoModel c1 = ComissaoModelBuilder.umaComissao().comStatus("Pago").vinculadaA(consultor, proposta).build();
        entityManager.persist(c1);
        entityManager.flush();

        // WHEN
        List<ComissaoModel> pagas = repository.findByUsuarioIdAndStatusPagamento(consultor.getId(), "Pago");

        // THEN
        assertThat(pagas).hasSize(1);
        log.info("{} [AUDITORIA] Filtro validado com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve carregar comissão com todos os detalhes (JOIN FETCH) para evitar LazyException")
    void deveCarregarComDetalhes() {
        log.info("{} [TELEMETRIA] Validando query complexa JOIN FETCH.", LOG_PREFIX);

        // GIVEN: Montagem do grafo completo
        UsuarioModel u = criarUsuarioLimpo("admin_sistema");
        entityManager.persist(u);

        BancoModel b = BancoModelBuilder.umBanco().comNome("Banco Alfa").build();
        entityManager.persist(b);

        ProponenteModel p = ProponenteModelBuilder.umProponente().comNome("Cliente Beta").build();
        p.setUsuario(u);
        entityManager.persist(p);

        PropostaModel prop = PropostaModelBuilder.umaProposta().vinculadoA(p, b, u).build();
        entityManager.persist(prop);

        ComissaoModel comissao = ComissaoModelBuilder.umaComissao().vinculadaA(u, prop).build();
        entityManager.persist(comissao);

        entityManager.flush();
        entityManager.clear(); // Força consulta ao banco

        // WHEN
        List<ComissaoModel> resultados = repository.findAllComDetalhes();

        // THEN
        assertThat(resultados).isNotEmpty();
        ComissaoModel recuperada = resultados.get(0);
        assertThat(recuperada.getProposta().getProponente().getNomeCompleto()).isEqualTo("Cliente Beta");

        log.info("{} [AUDITORIA] Grafo de objetos carregado via JOIN FETCH com sucesso.", LOG_PREFIX);
    }

    private UsuarioModel criarUsuarioLimpo(String username) {
        UsuarioModel u = new UsuarioModel();
        u.setUsername(username);
        u.setNome("Wagner Teste");
        u.setEmail(username + "@poder.com");
        u.setSenhaHash("hash_fake");
        u.setAtivo(true);
        return u;
    }
}
