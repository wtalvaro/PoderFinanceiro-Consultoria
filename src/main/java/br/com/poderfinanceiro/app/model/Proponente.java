package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "proponentes")
@Getter
@Setter
@NoArgsConstructor
public class Proponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proponente_id")
    private Long id;

    // O consultor dono deste lead (amarrado via AuthService na hora de salvar)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nome_completo", nullable = false, length = 255)
    private String nomeCompleto;

    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(name = "renda_mensal", precision = 12, scale = 2)
    private BigDecimal rendaMensal;

    @Column(name = "tipo_vinculo", length = 50)
    private String tipoVinculo; // INSS, SIAPE, Forças Armadas, etc.

    @Column(name = "convenio_orgao", length = 100)
    private String convenioOrgao;

    @Column(length = 50)
    private String matricula;

    @Column(name = "origem_consentimento", columnDefinition = "TEXT")
    private String origemConsentimento;

    // A mágica acontece aqui: Por padrão todo mundo nasce como LEAD!
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // <-- A MÁGICA ESTÁ AQUI
    @Column(name = "classificacao", columnDefinition = "tipo_relacionamento")
    private TipoRelacionamento classificacao = TipoRelacionamento.LEAD;

    @Column(name = "data_cadastro", updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
    }
}