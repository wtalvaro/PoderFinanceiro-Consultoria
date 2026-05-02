package br.com.poderfinanceiro.app.service;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.model.Usuario;
import br.com.poderfinanceiro.app.repository.ProponenteRepository;
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
    public Proponente salvarLead(Proponente lead) {
        // 1. Segurança: Verifica se há um consultor logado
        Usuario consultorLogado = authService.getUsuarioLogado();
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

        if (idAtual == null) {
            // NOVO REGISTRO: O CPF não pode existir na carteira deste consultor
            if (proponenteRepository.existsByCpfAndUsuarioIdAndDeletadoEmIsNull(cpfLimpo, consultorLogado.getId())) {
                throw new IllegalArgumentException("Você já possui um cliente cadastrado com este CPF.");
            }
        } else {
            // EDIÇÃO: O CPF pode existir, desde que não seja de OUTRO cliente (IdNot)
            boolean cpfJaExisteEmOutro = proponenteRepository
                    .existsByCpfAndUsuarioIdAndIdNotAndDeletadoEmIsNull(
                            cpfLimpo, consultorLogado.getId(), idAtual);

            if (cpfJaExisteEmOutro) {
                throw new IllegalArgumentException("Este CPF já está sendo usado por outro cliente na sua carteira.");
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
    public List<Proponente> listarMinhaCarteira() {
        Usuario consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null) {
            return List.of(); // Retorna lista vazia se não tiver logado
        }
        return proponenteRepository.findByUsuarioIdAndDeletadoEmIsNull(consultorLogado.getId());
    }

    /**
     * Utilizado pela barra de busca rápida no menu lateral.
     */
    public List<Proponente> buscaRapida(String termo) {
        Usuario consultorLogado = authService.getUsuarioLogado();
        if (consultorLogado == null || termo == null || termo.isBlank()) {
            return List.of();
        }
        // Limpa o termo caso seja um CPF formatado
        String termoBusca = termo.replaceAll("[^a-zA-Z0-9]", "");
        return proponenteRepository.buscarRapidaPorNomeOuCpf(termoBusca, consultorLogado.getId());
    }
}