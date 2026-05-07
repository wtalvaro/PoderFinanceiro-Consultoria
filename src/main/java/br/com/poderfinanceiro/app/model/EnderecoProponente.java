package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "enderecos_proponente")
@Getter
@Setter
@NoArgsConstructor
public class EnderecoProponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "endereco_id")
    private Long id;

    // Relacionamento com o Proponente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proponente_id", nullable = false)
    private Proponente proponente;

    @Column(nullable = false, length = 8)
    private String cep;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_logradouro", columnDefinition = "tipo_logradouro_enum")
    private TipoLogradouro tipoLogradouro = TipoLogradouro.RUA;

    @Column(nullable = false, length = 255)
    private String logradouro;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "uf", columnDefinition = "uf_enum")
    private Uf uf;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.ultimaAtualizacao = LocalDateTime.now();
    }
}