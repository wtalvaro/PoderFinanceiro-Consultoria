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
@Table(name = "propostas")
@Getter
@Setter
@NoArgsConstructor
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposta_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proponente_id", nullable = false)
    private Proponente proponente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // Consultor responsável

    @Column(name = "valor_solicitado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorSolicitado;

    @Column(name = "valor_aprovado", precision = 12, scale = 2)
    private BigDecimal valorAprovado;

    @Column(name = "taxa_aplicada", precision = 5, scale = 2)
    private BigDecimal taxaAplicada;

    @Column(name = "quantidade_parcelas")
    private Integer quantidadeParcelas;

    // 1 e 2. Mudança para Enum e alteração do Default para "Digitada"
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "status_proposta_enum") // Nome EXATO do TYPE no seu SQL
    private StatusProposta status = StatusProposta.DIGITADA;

    @Column(precision = 10, scale = 6)
    private BigDecimal coeficiente;

    @Column(name = "valor_parcela", precision = 12, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "modalidade_juros", length = 20)
    private String modalidadeJuros = "Prefixado";

    @Column(name = "custo_efetivo_total", precision = 5, scale = 2)
    private BigDecimal custoEfetivoTotal;

    @Column(name = "margem_utilizada", precision = 12, scale = 2)
    private BigDecimal margemUtilizada;

    @Column(name = "eh_novacao")
    private Boolean ehNovacao = false;

    @Column(name = "saldo_quitacao_anterior", precision = 12, scale = 2)
    private BigDecimal saldoQuitacaoAnterior = BigDecimal.ZERO;

    // 3. Adicionando os campos que estavam no SQL mas faltavam no Java
    @Column(name = "valor_iof", precision = 12, scale = 2)
    private BigDecimal valorIof = BigDecimal.ZERO;

    @Column(name = "taxa_administracao", precision = 12, scale = 2)
    private BigDecimal taxaAdministracao = BigDecimal.ZERO;

    @Column(name = "data_solicitacao")
    private LocalDate dataSolicitacao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    @Column(name = "tabela_id")
    private Integer tabelaId;

    @Column(name = "usuario_atualizacao_id")
    private Long usuarioAtualizacaoId;

    @PrePersist
    protected void onCreate() {
        this.dataSolicitacao = LocalDate.now();
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.ultimaAtualizacao = LocalDateTime.now();
    }
}