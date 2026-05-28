package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.ProponenteAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.ProponenteCriadoEvent;
import br.com.poderfinanceiro.app.domain.event.ProponenteExcluidoEvent;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.infrastructure.repository.ProponenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço de Domínio para Gestão de Proponentes (Leads). Centraliza as regras
 * de negócio de prospecção, isolamento de carteira por consultor e integridade
 * de dados cadastrais.
 */
@Service
@Transactional(readOnly = true)
public class ProponenteService {

    private static final Logger log = LoggerFactory.getLogger(ProponenteService.class);
    private static final String LOG_PREFIX = "[ProponenteService]";

    private final ProponenteRepository proponenteRepository;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    public ProponenteService(ProponenteRepository proponenteRepository, AuthService authService,
            ApplicationEventPublisher eventPublisher) {
        this.proponenteRepository = proponenteRepository;
        this.authService = authService;
        this.eventPublisher = eventPublisher;
        log.info("{} [SISTEMA] Serviço de Proponentes inicializado com isolamento de carteira.", LOG_PREFIX);
    }

    /**
     * Salva ou atualiza um proponente, aplicando saneamento de CPF/Telefone e
     * validando duplicidade na carteira do consultor.
     */
    @Transactional public ProponenteModel salvarProponente(ProponenteModel proponente) {
        log.info("{} [TELEMETRIA] Iniciando persistência de proponente: {}", LOG_PREFIX,
                proponente != null ? proponente.getNomeCompleto() : "NULL");

        // 1. Validação de Segurança e Contexto
        UsuarioModel consultor = authService.getUsuarioLogado();
        if (consultor == null) {
            log.error("{} [NEGOCIO] Falha de segurança: Tentativa de salvar proponente sem consultor logado.",
                    LOG_PREFIX);
            throw new IllegalStateException("Sessão inválida ou expirada.");
        }

        if (proponente == null) {
            throw new IllegalArgumentException("Os dados do proponente são obrigatórios.");
        }

        // 2. Saneamento de Dados (Sanitization)
        sanitizarDadosCadastrais(proponente);

        // 3. Validação de Regra de Negócio: Unicidade de CPF na Carteira do
        // Consultor
        validarUnicidadeCpf(proponente, consultor.getId());

        try {
            boolean isNovo = proponente.getId() == null;
            proponente.setUsuario(consultor); // Garante o vínculo com o dono da
                                              // carteira

            ProponenteModel salvo = proponenteRepository.save(proponente);
            log.info("{} [AUDITORIA] Proponente {} com sucesso. ID: {}, Nome: {}", LOG_PREFIX,
                    isNovo ? "CRIADO" : "ATUALIZADO", salvo.getId(), salvo.getNomeCompleto());

            // 4. Notificação de Eventos
            if (isNovo) {
                eventPublisher.publishEvent(new ProponenteCriadoEvent(salvo.getId()));
            } else {
                eventPublisher.publishEvent(new ProponenteAtualizadoEvent(salvo.getId()));
            }

            return salvo;

        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao persistir proponente no banco de dados: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    /**
     * Recupera todos os proponentes ativos da carteira do consultor logado.
     */
    public List<ProponenteModel> listarMinhaCarteira() {
        log.trace("{} [TELEMETRIA] Solicitada listagem da carteira de clientes.", LOG_PREFIX);

        UsuarioModel consultor = authService.getUsuarioLogado();
        if (consultor == null)
            return List.of();

        return proponenteRepository.findByUsuarioIdAndDeletadoEmIsNull(consultor.getId());
    }

    /**
     * Realiza busca rápida por nome ou CPF dentro da carteira do consultor.
     */
    public List<ProponenteModel> buscaRapida(String termo) {
        log.debug("{} [TELEMETRIA] Executando busca rápida por termo: {}", LOG_PREFIX, termo);

        UsuarioModel consultor = authService.getUsuarioLogado();
        if (consultor == null || termo == null || termo.isBlank())
            return List.of();

        String termoLimpo = termo.replaceAll("[^a-zA-Z0-9]", "");
        return proponenteRepository.buscarRapidaPorNomeOuCpf(termoLimpo, consultor.getId());
    }

    /**
     * Busca um proponente específico pelo ID.
     */
    public Optional<ProponenteModel> buscarPorId(Long id) {
        log.trace("{} [TELEMETRIA] Buscando proponente por ID: {}", LOG_PREFIX, id);
        return proponenteRepository.findById(id);
    }

    /**
     * Realiza a exclusão (ou soft-delete) de um proponente.
     */
    @Transactional public void excluirProponente(Long id) {
        log.info("{} [TELEMETRIA] Solicitada exclusão do proponente ID: {}", LOG_PREFIX, id);

        if (id == null || !proponenteRepository.existsById(id)) {
            log.warn("{} [NEGOCIO] Tentativa de exclusão de ID inexistente ou nulo: {}", LOG_PREFIX, id);
            return;
        }

        try {
            proponenteRepository.deleteById(id);
            log.info("{} [AUDITORIA] Proponente ID {} removido da carteira.", LOG_PREFIX, id);

            eventPublisher.publishEvent(new ProponenteExcluidoEvent(id));
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao excluir proponente: {}", LOG_PREFIX, e.getMessage());
            throw e;
        }
    }

    // --- Métodos Auxiliares de Domínio ---

    private void sanitizarDadosCadastrais(ProponenteModel p) {
        if (p.getCpf() != null) {
            p.setCpf(p.getCpf().replaceAll("\\D", ""));
        }
        if (p.getTelefone() != null) {
            p.setTelefone(p.getTelefone().replaceAll("\\D", ""));
        }
        if (p.getNomeCompleto() != null) {
            p.setNomeCompleto(p.getNomeCompleto().trim().toUpperCase());
        }
        log.trace("{} [SISTEMA] Dados do proponente sanitizados para persistência.", LOG_PREFIX);
    }

    private void validarUnicidadeCpf(ProponenteModel proponente, Long usuarioId) {
        String cpf = proponente.getCpf();
        if (cpf == null || cpf.isBlank())
            return;

        boolean existe;
        if (proponente.getId() == null) {
            existe = proponenteRepository.existsByCpfAndUsuarioIdAndDeletadoEmIsNull(cpf, usuarioId);
        } else {
            existe = proponenteRepository.existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(cpf, usuarioId,
                    proponente.getId());
        }

        if (existe) {
            log.warn("{} [NEGOCIO] Violação de unicidade: CPF {} já existe na carteira do consultor {}.", LOG_PREFIX,
                    cpf, usuarioId);
            throw new IllegalArgumentException("Este CPF já está cadastrado em sua carteira de clientes.");
        }
    }
}
