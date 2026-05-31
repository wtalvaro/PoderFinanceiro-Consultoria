package br.com.poderfinanceiro.app.domain.repository;

import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.util.BancoModelBuilder;
import br.com.poderfinanceiro.app.util.DocumentoProponenteModelBuilder;
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
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de Integração para DocumentoProponenteRepository.
 * Valida a integridade de hashes, filtros de verificação e separação de
 * contextos.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_jdbc_type_for_enums=VARCHAR"
})
class DocumentoProponenteRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProponenteRepositoryTest.class);
    private static final String LOG_PREFIX = "[DocumentoRepositoryTest]";

    @Autowired
    private DocumentoProponenteRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    private UsuarioModel consultor;
    private ProponenteModel proponente;

    @BeforeEach
    void setup() {
        log.info("{} [SISTEMA] Preparando infraestrutura de teste documental.", LOG_PREFIX);

        consultor = new UsuarioModel();
        consultor.setUsername("auditor_" + System.currentTimeMillis());
        consultor.setNome("Auditor Teste");
        consultor.setEmail(consultor.getUsername() + "@poder.com");
        consultor.setSenhaHash("123");
        entityManager.persist(consultor);

        proponente = ProponenteModelBuilder.umProponente().build();
        proponente.setUsuario(consultor);
        entityManager.persist(proponente);

        entityManager.flush();
    }

    @Test
    @DisplayName("Deve localizar um documento pelo seu Hash SHA-256 (Identidade Digital)")
    void deveBuscarPorHash() {
        log.info("{} [TELEMETRIA] Iniciando teste de integridade por Hash.", LOG_PREFIX);

        String hashUnico = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        DocumentoProponenteModel doc = DocumentoProponenteModelBuilder.umDocumento()
                .comHash(hashUnico)
                .vinculadoA(proponente, null, consultor)
                .build();

        entityManager.persist(doc);
        entityManager.flush();

        Optional<DocumentoProponenteModel> encontrado = repository.findByHashSha256(hashUnico);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getHashSha256()).isEqualTo(hashUnico);
        log.info("{} [AUDITORIA] Documento localizado via Hash com sucesso.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve listar documentos pendentes de verificação em ordem cronológica (ID Asc)")
    void deveListarPendentesDeVerificacao() {
        log.info("{} [TELEMETRIA] Validando fila de auditoria documental.", LOG_PREFIX);

        DocumentoProponenteModel d1 = DocumentoProponenteModelBuilder.umDocumento()
                .comTipo("RG").vinculadoA(proponente, null, consultor).build();
        DocumentoProponenteModel d2 = DocumentoProponenteModelBuilder.umDocumento()
                .comTipo("CPF").vinculadoA(proponente, null, consultor).build();

        entityManager.persist(d1);
        entityManager.persist(d2);
        entityManager.flush();

        List<DocumentoProponenteModel> pendentes = repository.findByVerificadoFalseOrderByIdAsc();

        assertThat(pendentes).hasSize(2);
        assertThat(pendentes.get(0).getTipoDocumento()).isEqualTo("RG");
        log.info("{} [AUDITORIA] Fila de verificação validada.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve separar documentos gerais do Lead de documentos específicos de Propostas")
    void deveFiltrarDocumentosSemProposta() {
        log.info("{} [TELEMETRIA] Testando filtro de documentos órfãos de proposta.", LOG_PREFIX);

        // 1. Documento Geral do Lead
        DocumentoProponenteModel docLead = DocumentoProponenteModelBuilder.umDocumento()
                .comTipo("IDENTIDADE").vinculadoA(proponente, null, consultor).build();

        // 2. Documento de uma Proposta
        BancoModel banco = BancoModelBuilder.umBanco().build();
        entityManager.persist(banco);
        PropostaModel proposta = PropostaModelBuilder.umaProposta().vinculadoA(proponente, banco, consultor).build();
        entityManager.persist(proposta);

        DocumentoProponenteModel docProposta = DocumentoProponenteModelBuilder.umDocumento()
                .comTipo("CONTRATO").vinculadoA(proponente, proposta, consultor).build();

        entityManager.persist(docLead);
        entityManager.persist(docProposta);
        entityManager.flush();

        // WHEN
        List<DocumentoProponenteModel> apenasLead = repository
                .findByProponenteIdAndPropostaIdIsNull(proponente.getId());

        // THEN
        assertThat(apenasLead).hasSize(1);
        assertThat(apenasLead.get(0).getTipoDocumento()).isEqualTo("IDENTIDADE");
        log.info("{} [AUDITORIA] Filtro de contexto Lead vs Proposta validado.", LOG_PREFIX);
    }

    @Test
    @DisplayName("Deve listar documentos de um proponente mantendo a ordem de inserção estável")
    void deveListarOrdenadoPorId() {
        log.info("{} [TELEMETRIA] Validando estabilidade de ordenação na listagem.", LOG_PREFIX);

        entityManager.persist(DocumentoProponenteModelBuilder.umDocumento().comTipo("A")
                .vinculadoA(proponente, null, consultor).build());
        entityManager.persist(DocumentoProponenteModelBuilder.umDocumento().comTipo("B")
                .vinculadoA(proponente, null, consultor).build());
        entityManager.flush();

        List<DocumentoProponenteModel> docs = repository.findByProponenteIdOrderByIdAsc(proponente.getId());

        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getTipoDocumento()).isEqualTo("A");
        assertThat(docs.get(1).getTipoDocumento()).isEqualTo("B");
    }
}
