package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.ProponenteAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.ProponenteCriadoEvent;
import br.com.poderfinanceiro.app.domain.event.ProponenteExcluidoEvent;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.ProponenteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ProponenteService {

    private static final Logger log = LoggerFactory.getLogger(ProponenteService.class);

    private final ProponenteRepository proponenteRepository;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    public ProponenteService(ProponenteRepository proponenteRepository, AuthService authService,
            ApplicationEventPublisher eventPublisher) {
        this.proponenteRepository = proponenteRepository;
        this.authService = authService;
        this.eventPublisher = eventPublisher;
        log.debug("[PROPONENTE_SERVICE] Construtor: Serviço instanciado");
    }

    @Transactional
    public ProponenteModel salvarProponente(ProponenteModel lead) {
        if (lead == null) {
            log.error("[PROPONENTE_SERVICE] salvarProponente: Proponente nulo");
            throw new IllegalArgumentException("Proponente não pode ser nulo.");
        }
        log.debug("[PROPONENTE_SERVICE] salvarProponente: Salvando proponente com ID={}",
                lead.getId());
        UsuarioModel consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null) {
            log.error("[PROPONENTE_SERVICE] salvarProponente: Nenhum consultor logado na sessão");
            throw new IllegalStateException("Erro de segurança: Nenhum consultor logado na sessão.");
        }
        log.trace("[PROPONENTE_SERVICE] Consultor logado: ID={}", consultorLogado.getId());

        boolean isNovo = lead.getId() == null;

        if (lead.getCpf() != null) {
            String cpfAntigo = lead.getCpf();
            lead.setCpf(lead.getCpf().replaceAll("[^0-9]", ""));
            log.trace("[PROPONENTE_SERVICE] CPF normalizado: '{}' -> '{}'", cpfAntigo, lead.getCpf());
        }
        if (lead.getTelefone() != null) {
            String telefoneAntigo = lead.getTelefone();
            lead.setTelefone(lead.getTelefone().replaceAll("[^0-9]", ""));
            log.trace("[PROPONENTE_SERVICE] Telefone normalizado: '{}' -> '{}'", telefoneAntigo, lead.getTelefone());
        }

        String cpfLimpo = lead.getCpf();
        Long idAtual = lead.getId();
        boolean isCpfPreenchido = cpfLimpo != null && !cpfLimpo.trim().isEmpty();

        if (isCpfPreenchido) {
            if (idAtual == null) {
                if (proponenteRepository.existsByCpfAndUsuarioIdAndDeletadoEmIsNull(cpfLimpo,
                        consultorLogado.getId())) {
                    log.warn("[PROPONENTE_SERVICE] salvarProponente: CPF '{}' já existe na carteira do consultor ID={}",
                            cpfLimpo, consultorLogado.getId());
                    throw new IllegalArgumentException("Você já possui um cliente cadastrado com este CPF.");
                }
            } else {
                boolean cpfJaExisteEmOutro = proponenteRepository
                        .existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(cpfLimpo, consultorLogado.getId(), idAtual);

                if (cpfJaExisteEmOutro) {
                    log.warn(
                            "[PROPONENTE_SERVICE] salvarProponente: CPF '{}' já em uso por outro cliente (ID={}) do consultor",
                            cpfLimpo, idAtual);
                    throw new IllegalArgumentException(
                            "Este CPF já está sendo usado por outro cliente na sua carteira.");
                }
            }
        }

        lead.setUsuario(consultorLogado);
        ProponenteModel leadSalvo = proponenteRepository.save(lead);
        log.info("[PROPONENTE_SERVICE] salvarProponente: Proponente salvo com ID={}, nome='{}'", leadSalvo.getId(),
                leadSalvo.getNomeCompleto());

        if (isNovo) {
            log.debug("[PROPONENTE_SERVICE] Disparando evento ProponenteCriadoEvent para ID={}", leadSalvo.getId());
            eventPublisher.publishEvent(new ProponenteCriadoEvent(leadSalvo.getId()));
        } else {
            log.debug("[PROPONENTE_SERVICE] Disparando evento ProponenteAtualizadoEvent para ID={}", leadSalvo.getId());
            eventPublisher.publishEvent(new ProponenteAtualizadoEvent(leadSalvo.getId()));
        }

        return leadSalvo;
    }

    public List<ProponenteModel> listarMinhaCarteira() {
        log.debug("[PROPONENTE_SERVICE] listarMinhaCarteira: Consultando carteira do consultor logado");
        UsuarioModel consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null) {
            log.warn("[PROPONENTE_SERVICE] listarMinhaCarteira: Nenhum consultor logado, retornando lista vazia");
            return List.of();
        }
        List<ProponenteModel> clientes = proponenteRepository
                .findByUsuarioIdAndDeletadoEmIsNull(consultorLogado.getId());
        log.info("[PROPONENTE_SERVICE] listarMinhaCarteira: {} cliente(s) encontrado(s) para consultor ID={}",
                clientes.size(), consultorLogado.getId());
        return clientes;
    }

    public List<ProponenteModel> buscaRapida(String termo) {
        log.debug("[PROPONENTE_SERVICE] buscaRapida: termo='{}'", termo);
        UsuarioModel consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null || termo == null || termo.isBlank()) {
            if (consultorLogado == null) {
                log.warn("[PROPONENTE_SERVICE] buscaRapida: Nenhum consultor logado");
            } else {
                log.warn("[PROPONENTE_SERVICE] buscaRapida: Termo vazio ou nulo");
            }
            return List.of();
        }
        String termoBusca = termo.replaceAll("[^a-zA-Z0-9]", "");
        log.trace("[PROPONENTE_SERVICE] Termo limpo para busca: '{}'", termoBusca);
        List<ProponenteModel> resultados = proponenteRepository.buscarRapidaPorNomeOuCpf(termoBusca,
                consultorLogado.getId());
        log.info("[PROPONENTE_SERVICE] buscaRapida: {} resultado(s) encontrado(s) para termo '{}'", resultados.size(),
                termo);
        return resultados;
    }

    public ProponenteModel buscarPorId(Long id) {
        log.debug("[PROPONENTE_SERVICE] buscarPorId: Buscando proponente com ID={}", id);
        if (id == null) {
            log.warn("[PROPONENTE_SERVICE] buscarPorId: ID nulo, retornando null");
            return null;
        }
        return proponenteRepository.findById(id).orElse(null);
    }

    @Transactional
    public void excluirProponente(Long id) {
        log.info("[PROPONENTE_SERVICE] excluirProponente: Solicitada exclusão do proponente ID={}", id);
        if (id == null) return;
        
        // Aqui você pode adicionar lógica de soft-delete ou delete real, dependendo da sua regra.
        // Exemplo para delete real:
        proponenteRepository.deleteById(id);
        
        // Publica o evento de exclusão
        log.debug("[PROPONENTE_SERVICE] Disparando evento ProponenteExcluidoEvent para ID={}", id);
        eventPublisher.publishEvent(new ProponenteExcluidoEvent(id));
    }
}