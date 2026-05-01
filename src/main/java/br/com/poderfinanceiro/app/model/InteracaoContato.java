package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "interacoes_contato")
@Getter
@Setter
@NoArgsConstructor
public class InteracaoContato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interacao_id")
    private Long id;

    // Relacionamento com o Cliente (Proponente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proponente_id")
    private Proponente proponente;

    // Relacionamento com o Consultor que realizou o contato
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 20)
    private String canal = "WhatsApp"; // Valor padrão conforme o banco

    @Column(name = "mensagem_texto", columnDefinition = "TEXT")
    private String mensagemTexto;

    @Column(length = 10)
    private String direcao; // Ex: "Entrada" ou "Saída"

    @Column(name = "data_interacao", updatable = false)
    private LocalDateTime dataInteracao;

    @PrePersist
    protected void onCreate() {
        this.dataInteracao = LocalDateTime.now();
    }
}