package br.com.poderfinanceiro.app.domain.service;

import br.com.poderfinanceiro.app.application.dto.GitHubReleaseDTO;
import br.com.poderfinanceiro.app.infrastructure.client.UpdateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * <h1>UpdateService</h1>
 * <p>
 * Serviço de Domínio responsável pela detecção de novas versões.
 * Implementa a estratégia de Atualização Manual Assistida para garantir
 * estabilidade absoluta no Windows 11, eliminando riscos de bloqueio de
 * arquivo.
 * </p>
 */
@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private static final String LOG_PREFIX = "[UpdateService]";
    private static final String PREFIXO_V = "v";

    private final UpdateClient updateClient;
    private final String versaoAtual;

    public UpdateService(UpdateClient updateClient, @Value("${app.version:1.0.0}") String versaoAtual) {
        this.updateClient = updateClient;
        this.versaoAtual = normalizarVersao(versaoAtual);
        log.info("{} [SISTEMA] Sensor de atualizações inicializado. Versão local: {}", LOG_PREFIX, this.versaoAtual);
    }

    /**
     * Verifica se existe uma nova versão disponível no repositório remoto.
     */
    public Optional<GitHubReleaseDTO> checarNovaVersao() {
        log.info("{} [TELEMETRIA] Iniciando verificação de versão remota no GitHub.", LOG_PREFIX);
        try {
            GitHubReleaseDTO ultimaRelease = updateClient.buscarUltimaRelease();

            if (ultimaRelease == null || ultimaRelease.tagName() == null) {
                log.warn("{} [SISTEMA] Resposta inválida do servidor de releases.", LOG_PREFIX);
                return Optional.empty();
            }

            String versaoRemota = normalizarVersao(ultimaRelease.tagName());

            if (isVersaoNova(versaoRemota, versaoAtual)) {
                log.info("{} [NEGOCIO] Nova release detectada: {} (Atual: {})",
                        LOG_PREFIX, ultimaRelease.tagName(), versaoAtual);
                return Optional.of(ultimaRelease);
            }

            log.debug("{} [NEGOCIO] O sistema já está na versão mais recente.", LOG_PREFIX);
            return Optional.empty();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Falha ao consultar o GitHub: {}", LOG_PREFIX, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * MANTIDO PARA PRESERVAR O CONTRATO DA FACADE E EVITAR ERROS DE COMPILAÇÃO.
     * No modo manual, este método apenas registra o log de intenção.
     */
    public void baixarEExecutarAtualizacaoPorTag(String tag) throws Exception {
        log.info(
                "{} [SISTEMA] Modo de atualização manual ativo. O download deve ser feito via navegador para a tag: {}",
                LOG_PREFIX, tag);
        // Lógica de download automático removida para evitar bloqueios de arquivo no
        // Windows 11.
    }

    /**
     * Normaliza strings de versão removendo prefixos e espaços.
     * Visibilidade package-private para suportar testes unitários.
     */
    String normalizarVersao(String versao) {
        if (versao == null)
            return "0.0.0";
        return versao.toLowerCase().replace(PREFIXO_V, "").trim();
    }

    /**
     * Algoritmo de comparação de versões semânticas (SemVer).
     * Visibilidade package-private para suportar testes unitários.
     */
    boolean isVersaoNova(String remota, String atual) {
        try {
            String[] partesRemota = remota.split("\\.");
            String[] partesAtual = atual.split("\\.");
            int maxPartes = Math.max(partesRemota.length, partesAtual.length);

            for (int i = 0; i < maxPartes; i++) {
                int vRemota = i < partesRemota.length ? Integer.parseInt(partesRemota[i].replaceAll("\\D", "")) : 0;
                int vAtual = i < partesAtual.length ? Integer.parseInt(partesAtual[i].replaceAll("\\D", "")) : 0;

                if (vRemota > vAtual)
                    return true;
                if (vRemota < vAtual)
                    return false;
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao processar comparação numérica de versões: {}", LOG_PREFIX, e.getMessage());
        }
        return false;
    }
}
