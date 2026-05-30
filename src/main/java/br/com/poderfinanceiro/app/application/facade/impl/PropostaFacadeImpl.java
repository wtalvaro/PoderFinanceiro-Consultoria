package br.com.poderfinanceiro.app.application.facade.impl;

import br.com.poderfinanceiro.app.application.facade.IPropostaFacade;
import br.com.poderfinanceiro.app.domain.event.PropostaPagaEvent;
import br.com.poderfinanceiro.app.domain.model.*;
import br.com.poderfinanceiro.app.domain.model.enums.StatusPropostaModel;
import br.com.poderfinanceiro.app.domain.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/**
 * <h1>PropostaFacadeImpl</h1>
 * <p>
 * Implementação do padrão <b>Facade</b> para o módulo de Propostas. Atua como o
 * único ponto de entrada (Single Point of Entry) para a camada de apresentação
 * (UI), orquestrando múltiplos serviços de domínio, garantindo a
 * transacionalidade e blindando os controladores contra a complexidade das
 * regras de negócio.
 * </p>
 * <p>
 * <b>Princípios Aplicados:</b>
 * </p>
 * <ul>
 * <li><b>SRP (Single Responsibility):</b> Centraliza a orquestração de casos de
 * uso da proposta.</li>
 * <li><b>DIP (Dependency Inversion):</b> A UI depende da interface
 * {@link IPropostaFacade}, não desta implementação.</li>
 * </ul>
 * 
 * @author Arquiteto de Software
 * @version 2.0 (Refatoração SOLID e Telemetria)
 */
@Service
public class PropostaFacadeImpl implements IPropostaFacade {

    // ==========================================================================================
    // MÓDULO 1: CONSTANTES E TELEMETRIA
    // ==========================================================================================
    private static final Logger log = LoggerFactory.getLogger(PropostaFacadeImpl.class);
    private static final String LOG_PREFIX = "[PropostaFacadeImpl]";

    // ==========================================================================================
    // MÓDULO 2: DEPENDÊNCIAS DE DOMÍNIO
    // ==========================================================================================
    private final PropostaService propostaService;
    private final TabelaJurosService tabelaJurosService;
    private final ProponenteService proponenteService;
    private final DocumentoService documentoService;
    private final AtendimentoContextService contextoService;
    private final ApplicationEventPublisher eventPublisher;
    private final AssistenteDocumentalService assistenteIA;

    public PropostaFacadeImpl(PropostaService propostaService, TabelaJurosService tabelaJurosService, ProponenteService proponenteService,
            DocumentoService documentoService, AtendimentoContextService contextoService, ApplicationEventPublisher eventPublisher,
            AssistenteDocumentalService assistenteIA) {
        this.propostaService = propostaService;
        this.tabelaJurosService = tabelaJurosService;
        this.proponenteService = proponenteService;
        this.documentoService = documentoService;
        this.contextoService = contextoService;
        this.eventPublisher = eventPublisher;
        this.assistenteIA = assistenteIA;
        log.debug("{} [SISTEMA] Facade instanciada e dependências injetadas com sucesso.", LOG_PREFIX);
    }

    // ==========================================================================================
    // MÓDULO 3: CONSULTAS E LISTAGENS BASE (Read-Only)
    // ==========================================================================================

    @Override public List<TabelaJurosModel> listarTabelasAtivas() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de tabelas de juros ativas.", LOG_PREFIX);
        return tabelaJurosService.listarAtivas();
    }

    @Override
    public List<BancoModel> listarBancosDasTabelasAtivas() {
        log.trace("{} [TELEMETRIA] Extraindo bancos únicos com verificação de identidade robusta.", LOG_PREFIX);

        return tabelaJurosService.listarAtivas().stream()
                .map(TabelaJurosModel::getBanco)
                .filter(java.util.Objects::nonNull)
                // O distinct() agora é confiável devido ao Pilar 1
                .distinct()
                .toList();
    }

    @Override public List<ProponenteModel> listarClientesCarteira() {
        log.trace("{} [TELEMETRIA] Solicitando listagem de clientes da carteira do usuário.", LOG_PREFIX);
        return proponenteService.listarMinhaCarteira();
    }

    @Override public PropostaModel carregarPropostaCompleta(Long id) {
        log.info("{} [TELEMETRIA] Carregando detalhes completos da proposta ID: {}", LOG_PREFIX, id);
        return propostaService.carregarPropostaDetalhada(id);
    }

    // ==========================================================================================
    // MÓDULO 4: ORQUESTRAÇÃO DE PROPOSTAS (Transacional)
    // ==========================================================================================

    /**
     * Orquestra o salvamento de uma proposta, garantindo o merge seguro de
     * dados da UI com os dados de background, aplicando regras de negócio e
     * disparando eventos de domínio.
     */
    @Override @Transactional public PropostaModel salvarProposta(PropostaModel dadosDaUI) {
        log.info("{} [TELEMETRIA] Iniciando orquestração de salvamento. ID: {}", LOG_PREFIX,
                dadosDaUI.getId() != null ? dadosDaUI.getId() : "NOVA");

        // 1. REGRA DE NEGÓCIO: Validação de Status PAGO
        if (StatusPropostaModel.PAGO.equals(dadosDaUI.getStatus())) {
            BigDecimal valorAprovado = dadosDaUI.getValorAprovado();
            if (valorAprovado == null || valorAprovado.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("{} [NEGOCIO] Bloqueio: Tentativa de salvar proposta PAGA sem valor aprovado. ID: {}", LOG_PREFIX,
                        dadosDaUI.getId());
                throw new IllegalArgumentException(
                        "Para marcar a proposta como Paga, o campo Valor Aprovado deve ser preenchido com um valor maior que R$ 0,00.");
            }
        }

        // 2. LÓGICA DE MERGE: Protege dados de background contra sobrescrita
        // indevida da UI
        PropostaModel modeloParaPersistir;
        if (dadosDaUI.getId() != null) {
            modeloParaPersistir = propostaService.carregarPropostaDetalhada(dadosDaUI.getId());
            if (modeloParaPersistir == null) {
                log.warn("{} [SISTEMA] Proposta ID {} não encontrada no banco durante o merge. Tratando como nova.", LOG_PREFIX,
                        dadosDaUI.getId());
                modeloParaPersistir = dadosDaUI;
            } else {
                modeloParaPersistir.setProponente(dadosDaUI.getProponente());
                modeloParaPersistir.setBanco(dadosDaUI.getBanco());
                modeloParaPersistir.setConvenioOrgao(dadosDaUI.getConvenioOrgao());
                modeloParaPersistir.setValorSolicitado(dadosDaUI.getValorSolicitado());
                modeloParaPersistir.setValorAprovado(dadosDaUI.getValorAprovado());
                modeloParaPersistir.setStatus(dadosDaUI.getStatus());
                modeloParaPersistir.setTabelaId(dadosDaUI.getTabelaId());
                modeloParaPersistir.setObservacoes(dadosDaUI.getObservacoes());
                modeloParaPersistir.setQuantidadeParcelas(dadosDaUI.getQuantidadeParcelas());
                modeloParaPersistir.setPrazoDesejado(dadosDaUI.getPrazoDesejado());
            }
        } else {
            modeloParaPersistir = dadosDaUI;
        }

        // 3. PERSISTÊNCIA
        PropostaModel salva = propostaService.salvarProposta(modeloParaPersistir);
        log.info("{} [AUDITORIA] Proposta persistida com sucesso no banco de dados. ID: {}", LOG_PREFIX, salva.getId());

        // 4. DISPARO DE EVENTOS DE DOMÍNIO
        if (StatusPropostaModel.PAGO.equals(salva.getStatus())) {
            log.info("{} [AUDITORIA] Status PAGO detectado. Disparando PropostaPagaEvent para ID: {}", LOG_PREFIX, salva.getId());
            eventPublisher.publishEvent(new PropostaPagaEvent(salva));
        }

        return propostaService.carregarPropostaDetalhada(salva.getId());
    }

    @Override public void excluirProposta(Long id) {
        log.warn("{} [AUDITORIA] Solicitação de exclusão permanente para a proposta ID: {}", LOG_PREFIX, id);
        propostaService.excluirProposta(id);
        log.info("{} [AUDITORIA] Proposta ID: {} excluída com sucesso.", LOG_PREFIX, id);
    }

    // ==========================================================================================
    // MÓDULO 5: REGRAS DE NEGÓCIO E VALIDAÇÕES
    // ==========================================================================================

    @Override public boolean isStatusTerminal(StatusPropostaModel status, Long id) {
        boolean terminal = id != null && (status == StatusPropostaModel.PAGO || status == StatusPropostaModel.REPROVADA
                || status == StatusPropostaModel.CANCELADO);
        log.trace("{} [NEGOCIO] Verificação de status terminal para ID {}: {}", LOG_PREFIX, id, terminal);
        return terminal;
    }

    @Override public boolean isBloqueadaPeloCopiloto(PropostaModel proposta) {
        boolean bloqueada = proposta != null && proposta.getObservacoes() != null
                && proposta.getObservacoes().contains("Copiloto de Vendas");
        log.trace("{} [NEGOCIO] Verificação de bloqueio por Copiloto: {}", LOG_PREFIX, bloqueada);
        return bloqueada;
    }

    @Override public BigDecimal calcularComissao(PropostaModel proposta) {
        if (proposta == null || proposta.getTabelaId() == null) {
            return BigDecimal.ZERO;
        }

        // REGRA DE NEGÓCIO: Define a base de cálculo (Aprovado tem prioridade
        // sobre Solicitado)
        BigDecimal base = proposta.getValorAprovado();
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            base = proposta.getValorSolicitado();
        }

        log.trace("{} [NEGOCIO] Calculando comissão. Base: {}, Tabela ID: {}", LOG_PREFIX, base, proposta.getTabelaId());
        return propostaService.calcularComissaoEstimada(base, proposta.getTabelaId());
    }

    @Override public BigDecimal calcularComissao(BigDecimal valorBase, Long tabelaId) {
        // Mantido para retrocompatibilidade com outras chamadas da interface
        return propostaService.calcularComissaoEstimada(valorBase, tabelaId);
    }

    // ==========================================================================================
    // MÓDULO 6: GESTÃO DE DOCUMENTOS
    // ==========================================================================================

    @Override public List<DocumentoProponenteModel> buscarDocumentosDaProposta(Long propostaId) {
        log.trace("{} [TELEMETRIA] Buscando documentos vinculados à proposta ID: {}", LOG_PREFIX, propostaId);
        return documentoService.buscarPorProposta(propostaId);
    }

    @Override public DocumentoProponenteModel salvarDocumento(File arquivo, String tipo, ProponenteModel proponente, PropostaModel proposta)
            throws Exception {
        log.info("{} [AUDITORIA] Iniciando upload de documento. Tipo: {}, Proponente ID: {}", LOG_PREFIX, tipo,
                proponente != null ? proponente.getId() : "N/A");
        return documentoService.processarUpload(arquivo, tipo, proponente, proposta);
    }

    @Override public DocumentoProponenteModel atualizarTipoDocumento(Long docId, String novoTipo) throws Exception {
        log.info("{} [AUDITORIA] Atualizando tipo do documento ID: {} para '{}'", LOG_PREFIX, docId, novoTipo);
        return documentoService.atualizarTipoDocumento(docId, novoTipo);
    }

    @Override public void excluirDocumento(Long docId) {
        log.warn("{} [AUDITORIA] Solicitando exclusão do documento ID: {}", LOG_PREFIX, docId);
        documentoService.excluirDocumento(docId);
    }

    // ==========================================================================================
    // MÓDULO 7: INTELIGÊNCIA ARTIFICIAL (GEMINI)
    // ==========================================================================================

    @Override public List<String> listarModelosIADisponiveis() {
        log.trace("{} [TELEMETRIA] Solicitando lista de modelos de IA disponíveis.", LOG_PREFIX);
        return assistenteIA.listarModelosDisponiveis();
    }

    @Override public AssistenteDocumentalService.ConfigIA obterConfiguracaoIADocumento(String tipoDocumento) {
        return assistenteIA.determinarConfiguracaoIA(tipoDocumento);
    }

    @Override public String analisarDocumentoComIA(DocumentoProponenteModel doc, ProponenteModel proponente, String modelo) {
        log.info("{} [TELEMETRIA] Solicitando análise de IA para documento ID: {}, Modelo: {}", LOG_PREFIX, doc.getId(), modelo);
        return assistenteIA.analisarDocumento(doc, proponente, modelo);
    }

    // ==========================================================================================
    // MÓDULO 8: CONTEXTO DE ATENDIMENTO
    // ==========================================================================================

    @Override public void atualizarContextoAtendimento(PropostaModel proposta) {
        if (contextoService != null) {
            log.trace("{} [SISTEMA] Atualizando contexto global de atendimento para a proposta ID: {}", LOG_PREFIX,
                    proposta != null ? proposta.getId() : "N/A");
            contextoService.setPropostaAtiva(proposta);
            contextoService.setTelaAtualFocada(AtendimentoContextService.TipoTelaFocada.ESTEIRA_PROPOSTAS);
        }
    }
}
