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
 * Serviço de Domínio responsável apenas pela detecção de novas versões.
 * Estratégia: Manual Assistida (Risco Zero para Windows 11).
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
        log.info("{} [SISTEMA] Sensor de atualizações ativo. Versão local: {}", LOG_PREFIX, this.versaoAtual);
    }

    public Optional<GitHubReleaseDTO> checarNovaVersao() {
        log.info("{} [TELEMETRIA] Verificando disponibilidade de novas versões no GitHub.", LOG_PREFIX);
        try {
            GitHubReleaseDTO ultimaRelease = updateClient.buscarUltimaRelease();
            if (ultimaRelease == null || ultimaRelease.tagName() == null)
                return Optional.empty();

            String versaoRemota = normalizarVersao(ultimaRelease.tagName());

            if (isVersaoNova(versaoRemota, versaoAtual)) {
                log.info("{} [NEGOCIO] Nova versão detectada: {} (Atual: {})", LOG_PREFIX, ultimaRelease.tagName(),
                        versaoAtual);
                return Optional.of(ultimaRelease);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro ao consultar atualizações: {}", LOG_PREFIX, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Método mantido para compatibilidade de contrato, mas sem execução de
     * download.
     */
    public void baixarEExecutarAtualizacaoPorTag(String tag) throws Exception {
        log.info("{} [SISTEMA] Modo manual ativo. Redirecionando usuário para o navegador.", LOG_PREFIX);
    }

    String normalizarVersao(String versao) {
        if (versao == null)
            return "0.0.0";
        return versao.toLowerCase().replace(PREFIXO_V, "").trim();
    }

    boolean isVersaoNova(String remota, String atual) {
        try {
            String[] pR = remota.split("\\.");
            String[] pA = atual.split("\\.");
            int max = Math.max(pR.length, pA.length);
            for (int i = 0; i < max; i++) {
                int vR = i < pR.length ? Integer.parseInt(pR[i].replaceAll("\\D", "")) : 0;
                int vA = i < pA.length ? Integer.parseInt(pA[i].replaceAll("\\D", "")) : 0;
                if (vR > vA)
                    return true;
                if (vR < vA)
                    return false;
            }
        } catch (Exception e) {
            log.error("{} [SISTEMA] Erro na comparação de versões.", LOG_PREFIX);
        }
        return false;
    }
}
