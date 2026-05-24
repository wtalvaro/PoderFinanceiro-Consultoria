package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.event.ProponenteAtualizadoEvent;
import br.com.poderfinanceiro.app.domain.event.ProponenteCriadoEvent;
import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.ProponenteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

@Service
public class ProponenteService {

    private final ProponenteRepository proponenteRepository;
    private final AuthService authService; // Essencial para saber quem está logado
    private final ApplicationEventPublisher eventPublisher;

    public ProponenteService(ProponenteRepository proponenteRepository, AuthService authService,
            ApplicationEventPublisher eventPublisher) {
        this.proponenteRepository = proponenteRepository;
        this.authService = authService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Salva ou Atualiza um Proponente atrelando-o ao consultor logado.
     */
    @Transactional
    public ProponenteModel salvarProponente(ProponenteModel lead) {
        UsuarioModel consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null) {
            throw new IllegalStateException("Erro de segurança: Nenhum consultor logado na sessão.");
        }

        // 🚀 Verifica se é criação antes de salvar
        boolean isNovo = lead.getId() == null;

        if (lead.getCpf() != null) {
            lead.setCpf(lead.getCpf().replaceAll("[^0-9]", ""));
        }
        if (lead.getTelefone() != null) {
            lead.setTelefone(lead.getTelefone().replaceAll("[^0-9]", ""));
        }

        String cpfLimpo = lead.getCpf();
        Long idAtual = lead.getId();
        boolean isCpfPreenchido = cpfLimpo != null && !cpfLimpo.trim().isEmpty();

        if (isCpfPreenchido) {
            if (idAtual == null) {
                if (proponenteRepository.existsByCpfAndUsuarioIdAndDeletadoEmIsNull(cpfLimpo, consultorLogado.getId())) {
                    throw new IllegalArgumentException("Você já possui um cliente cadastrado com este CPF.");
                }
            } else {
                boolean cpfJaExisteEmOutro = proponenteRepository
                        .existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(cpfLimpo, consultorLogado.getId(), idAtual);

                if (cpfJaExisteEmOutro) {
                    throw new IllegalArgumentException("Este CPF já está sendo usado por outro cliente na sua carteira.");
                }
            }
        }

        lead.setUsuario(consultorLogado);
        ProponenteModel leadSalvo = proponenteRepository.save(lead);

        // 🚀 DISPARO DOS EVENTOS APÓS SALVAR
        if (isNovo) {
            eventPublisher.publishEvent(new ProponenteCriadoEvent(leadSalvo.getId()));
        } else {
            eventPublisher.publishEvent(new ProponenteAtualizadoEvent(leadSalvo.getId()));
        }

        return leadSalvo;
    }

    /**
     * Retorna todos os clientes/leads da carteira do consultor logado.
     */
    public List<ProponenteModel> listarMinhaCarteira() {
        UsuarioModel consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null) {
            return List.of(); // Retorna lista vazia se não tiver logado
        }
        return proponenteRepository.findByUsuarioIdAndDeletadoEmIsNull(consultorLogado.getId());
    }

    /**
     * Utilizado pela barra de busca rápida no menu lateral.
     */
    public List<ProponenteModel> buscaRapida(String termo) {
        UsuarioModel consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null || termo == null || termo.isBlank()) {
            return List.of();
        }
        // Limpa o termo caso seja um CPF formatado
        String termoBusca = termo.replaceAll("[^a-zA-Z0-9]", "");
        return proponenteRepository.buscarRapidaPorNomeOuCpf(termoBusca, consultorLogado.getId());
    }

    /**
     * Busca a versão mais atualizada do Proponente diretamente do banco.
     */
    public ProponenteModel buscarPorId(Long id) {
        if (id == null)
            return null;
        return proponenteRepository.findById(id).orElse(null);
    }
}