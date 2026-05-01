package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_status_proposta")
@Getter
@Setter
@NoArgsConstructor
public class HistoricoStatusProposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historico_id")
    private Long id;

    // Relacionamento com a Proposta que sofreu a alteração
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    // Relacionamento com o Usuário que fez a alteração (Auditoria)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "status_anterior", length = 50)
    private String statusAnterior;

    @Column(name = "status_novo", nullable = false, length = 50)
    private String statusNovo;

    @Column(name = "data_mudanca", updatable = false)
    private LocalDateTime dataMudanca;

    @Column(name = "motivo_mudanca", columnDefinition = "TEXT")
    private String motivoMudanca;

    @PrePersist
    protected void onCreate() {
        this.dataMudanca = LocalDateTime.now();
    }
}