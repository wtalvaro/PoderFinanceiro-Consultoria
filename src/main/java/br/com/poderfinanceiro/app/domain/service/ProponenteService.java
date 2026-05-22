package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.domain.model.ProponenteModel;
import br.com.poderfinanceiro.app.domain.model.UsuarioModel;
import br.com.poderfinanceiro.app.domain.repository.ProponenteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProponenteService {

    private final ProponenteRepository proponenteRepository;
    private final AuthService authService; // Essencial para saber quem está logado

    public ProponenteService(ProponenteRepository proponenteRepository, AuthService authService) {
        this.proponenteRepository = proponenteRepository;
        this.authService = authService;
    }

    /**
     * Salva ou Atualiza um Lead atrelando-o ao consultor logado.
     */
    @Transactional
    public ProponenteModel salvarLead(ProponenteModel lead) {
        // 1. Segurança: Verifica se há um consultor logado
        UsuarioModel consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null) {
            throw new IllegalStateException("Erro de segurança: Nenhum consultor logado na sessão.");
        }

        // 2. Sanitização Agressiva (Dados Puros para o Banco)
        // Removemos pontos, traços, parênteses e espaços invisíveis
        if (lead.getCpf() != null) {
            lead.setCpf(lead.getCpf().replaceAll("[^0-9]", ""));
        }

        if (lead.getTelefone() != null) {
            lead.setTelefone(lead.getTelefone().replaceAll("[^0-9]", ""));
        }

        // 3. Validação de Duplicidade Inteligente
        String cpfLimpo = lead.getCpf();
        Long idAtual = lead.getId(); // No seu modelo o campo é 'id'
        boolean isCpfPreenchido = cpfLimpo != null && !cpfLimpo.trim().isEmpty();

        if (isCpfPreenchido) {
            if (idAtual == null) {
                // NOVO REGISTRO: O CPF não pode existir na carteira deste consultor
                if (proponenteRepository.existsByCpfAndUsuarioIdAndDeletadoEmIsNull(cpfLimpo,
                        consultorLogado.getId())) {
                    throw new IllegalArgumentException("Você já possui um cliente cadastrado com este CPF.");
                }
            } else {
                // EDIÇÃO: O CPF pode existir, desde que não seja de OUTRO cliente (IdNot)
                boolean cpfJaExisteEmOutro = proponenteRepository
                        .existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(
                                cpfLimpo, consultorLogado.getId(), idAtual);

                if (cpfJaExisteEmOutro) {
                    throw new IllegalArgumentException(
                            "Este CPF já está sendo usado por outro cliente na sua carteira.");
                }
            }
        }

        // 4. Vínculo de Propriedade
        lead.setUsuario(consultorLogado);

        // 5. Persistência (O Hibernate decide: INSERT se ID for null, UPDATE se ID
        // existir)
        return proponenteRepository.save(lead);
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