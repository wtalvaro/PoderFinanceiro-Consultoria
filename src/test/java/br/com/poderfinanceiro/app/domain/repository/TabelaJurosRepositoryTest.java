package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.BancoModel;
import br.com.poderfinanceiro.app.domain.model.TabelaJurosModel;
import br.com.poderfinanceiro.app.domain.model.enums.TipoConvenioModel;
import br.com.poderfinanceiro.app.util.BancoModelBuilder;
import br.com.poderfinanceiro.app.util.TabelaJurosModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de Integração para TabelaJurosRepository.
 * Valida o motor de elegibilidade e o carregamento ansioso de bancos (JOIN
 * FETCH).
 * Sincronizado com Spring Boot 4.0.6 e Java 25.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class TabelaJurosRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(TabelaJurosRepositoryTest.class);
    private static final String LOG_PREFIX = "[TabelaJurosRepositoryTest]";

    @Autowired
    private TabelaJurosRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private BancoModel bancoItau;

    @BeforeEach
    void setup() {
        log.info("{} [SISTEMA] Preparando infraestrutura de teste para tabelas de juros.", LOG_PREFIX);
        bancoItau = BancoModelBuilder.umBanco().comNome("Itaú").comCodigo("341").build();
        entityManager.persist(bancoItau);
        entityManager.flush();
    }

    @Test
    @DisplayName("Deve listar tabelas ativas de um banco com JOIN FETCH")
    void deveListarPorBancoEAtivo() {
        log.info("{} [TELEMETRIA] Testando busca por banco com carregamento ansioso.", LOG_PREFIX);

        // GIVEN
        TabelaJurosModel t1 = TabelaJurosModelBuilder.umaTabela().comNome("T1").comBanco(bancoItau).build();
        t1.setAtivo(true);
        TabelaJurosModel t2 = TabelaJurosModelBuilder.umaTabela().comNome("T2").comBanco(bancoItau).build();
        t2.setAtivo(false);

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();
        entityManager.clear();

        // WHEN
        List<TabelaJurosModel> resultados = repository.findByBancoIdAndAtivoTrue(bancoItau.getId());

        // THEN
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getBanco().getNome()).isEqualTo("Itaú");
        log.info("{} [AUDITORIA] Listagem por banco validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve filtrar tabelas elegíveis baseando-se em idade, valor e prazo (Motor do Copiloto)")
    void deveValidarElegibilidade() {
        log.info("{} [TELEMETRIA] Validando lógica complexa de elegibilidade do Copiloto.", LOG_PREFIX);

        // GIVEN: Tabela com restrições (Idade 18-80, Valor 1k-50k, Prazo 12-84)
        TabelaJurosModel tabela = TabelaJurosModelBuilder.umaTabela()
                .comNome("Tabela Restrita")
                .comBanco(bancoItau)
                .build();
        tabela.setTipoConvenio(TipoConvenioModel.INSS_CONSIGNADO);
        tabela.setIdadeMinima(18);
        tabela.setIdadeMaxima(80);
        tabela.setValorMinimoEmprestimo(new BigDecimal("1000.00"));
        tabela.setValorMaximoEmprestimo(new BigDecimal("50000.00"));
        tabela.setPrazoMinimo(12);
        tabela.setPrazoMaximo(84);
        tabela.setAtivo(true);

        entityManager.persist(tabela);
        entityManager.flush();

        // WHEN & THEN
        // 1. Cliente elegível
        assertThat(repository.findTabelasElegiveis(TipoConvenioModel.INSS_CONSIGNADO, 45, new BigDecimal("5000"), 48)).hasSize(1);

        // 2. Cliente muito idoso (85 anos) - Não deve retornar
        assertThat(repository.findTabelasElegiveis(TipoConvenioModel.INSS_CONSIGNADO, 85, new BigDecimal("5000"), 48)).isEmpty();

        // 3. Valor acima do permitido (60k) - Não deve retornar
        assertThat(repository.findTabelasElegiveis(TipoConvenioModel.INSS_CONSIGNADO, 45, new BigDecimal("60000"), 48)).isEmpty();

        // 4. Prazo fora do range (96 meses) - Não deve retornar
        assertThat(repository.findTabelasElegiveis(TipoConvenioModel.INSS_CONSIGNADO, 45, new BigDecimal("5000"), 96)).isEmpty();

        log.info("{} [AUDITORIA] Motor de elegibilidade validado para múltiplos cenários.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve respeitar a vigência das tabelas (Data de Fim)")
    void deveValidarVigencia() {
        log.info("{} [TELEMETRIA] Testando lógica de vigência temporal.", LOG_PREFIX);

        // GIVEN: Tabela expirada ontem
        TabelaJurosModel expirada = TabelaJurosModelBuilder.umaTabela().comNome("Campanha Antiga").comBanco(bancoItau)
                .build();
        expirada.setFimVigencia(LocalDate.now().minusDays(1));
        expirada.setAtivo(true);

        // Tabela sem data de fim (Vigência eterna)
        TabelaJurosModel eterna = TabelaJurosModelBuilder.umaTabela().comNome("Tabela Fixa").comBanco(bancoItau)
                .build();
        eterna.setFimVigencia(null);
        eterna.setAtivo(true);

        entityManager.persist(expirada);
        entityManager.persist(eterna);
        entityManager.flush();

        // WHEN
        List<TabelaJurosModel> ativas = repository.findAllAtivasWithBanco();

        // THEN
        assertThat(ativas).hasSize(1);
        assertThat(ativas.get(0).getNomeTabela()).isEqualTo("Tabela Fixa");
        log.info("{} [AUDITORIA] Lógica de vigência validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve buscar tabela por ID com JOIN FETCH do Banco")
    void deveBuscarPorIdComBanco() {
        TabelaJurosModel t = TabelaJurosModelBuilder.umaTabela().comBanco(bancoItau).build();
        entityManager.persist(t);
        entityManager.flush();
        entityManager.clear();

        Optional<TabelaJurosModel> encontrada = repository.findByIdWithBanco(t.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getBanco()).isNotNull();
        assertThat(encontrada.get().getBanco().getNome()).isEqualTo("Itaú");
    }
}
