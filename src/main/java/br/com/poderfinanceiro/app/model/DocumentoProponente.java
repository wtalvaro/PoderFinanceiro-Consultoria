package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_proponente")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoProponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_id")
    private Long id;

    // Relacionamento com o Cliente (Proponente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proponente_id")
    private Proponente proponente;

    // Relacionamento com o Consultor que fez o upload
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_documento", nullable = false, length = 50)
    private String tipoDocumento;

    @Column(name = "arquivo_path", nullable = false, columnDefinition = "TEXT")
    private String arquivoPath;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(nullable = false)
    private Boolean verificado = false;

    @Column(name = "data_upload", updatable = false)
    private LocalDateTime dataUpload;

    @PrePersist
    protected void onCreate() {
        this.dataUpload = LocalDateTime.now();
    }
}