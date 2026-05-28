package br.com.poderfinanceiro.app.infrastructure.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.poderfinanceiro.app.application.dto.GeminiRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

@Component
public class GeminiMediaMapper {

    private static final Logger log = LoggerFactory.getLogger(GeminiMediaMapper.class);
    private static final String LOG_PREFIX = "[GeminiMediaMapper]";

    public GeminiRequest.Part toPart(File arquivo) throws IOException {
        if (arquivo == null || !arquivo.exists()) {
            log.warn("{} [NEGOCIO] Tentativa de mapear arquivo inexistente.", LOG_PREFIX);
            throw new IOException("Arquivo não encontrado.");
        }

        log.trace("{} [SISTEMA] Convertendo arquivo para Part: {}", LOG_PREFIX, arquivo.getName());
        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String mimeType = detectarMimeType(arquivo.getName().toLowerCase());

        log.debug("{} [AUDITORIA] Arquivo mapeado com sucesso. MIME: {}, Size: {} bytes", LOG_PREFIX, mimeType,
                bytes.length);
        return GeminiRequest.Part.ofFile(mimeType, base64);
    }

    private String detectarMimeType(String fileName) {
        if (fileName.endsWith(".pdf"))
            return "application/pdf";
        if (fileName.endsWith(".png"))
            return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
            return "image/jpeg";
        return "application/octet-stream";
    }
}
