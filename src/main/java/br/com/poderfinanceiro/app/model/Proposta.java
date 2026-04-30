package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "propostas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposta_id")
    private Long id;

    // Relacionamentos Fortes (Chaves Estrangeiras)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proponente_id", nullable = false)
    private Proponente proponente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tabela_id")
    private TabelaJuros tabela;

    // Dados da Operação
    @Column(name = "valor_solicitado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorSolicitado;

    @Column(name = "valor_aprovado", precision = 12, scale = 2)
    private BigDecimal valorAprovado;

    @Column(name = "quantidade_parcelas", nullable = false)
    private Integer quantidadeParcelas;

    @Column(name = "valor_parcela", precision = 12, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "taxa_aplicada", precision = 5, scale = 2)
    private BigDecimal taxaAplicada;

    @Column(name = "coeficiente", precision = 10, scale = 6)
    private BigDecimal coeficiente;

    // Status mapeado como String para bater com o Enum do PostgreSQL
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusProposta status = StatusProposta.Lead;

    @Builder.Default
    @Column(name = "modalidade_juros", length = 20)
    private String modalidadeJuros = "Prefixado";

    @Column(name = "custo_efetivo_total", precision = 5, scale = 2)
    private BigDecimal custoEfetivoTotal;

    @Column(name = "margem_utilizada", precision = 12, scale = 2)
    private BigDecimal margemUtilizada;

    @Builder.Default
    @Column(name = "eh_novacao")
    private Boolean ehNovacao = false;

    @Builder.Default
    @Column(name = "saldo_quitacao_anterior", precision = 12, scale = 2)
    private BigDecimal saldoQuitacaoAnterior = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "valor_iof", precision = 12, scale = 2)
    private BigDecimal valorIof = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "taxa_administracao", precision = 12, scale = 2)
    private BigDecimal taxaAdministracao = BigDecimal.ZERO;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Builder.Default
    @Column(name = "data_solicitacao")
    private LocalDate dataSolicitacao = LocalDate.now();

    @Builder.Default
    @Column(name = "ultima_atualizacao", updatable = false)
    private LocalDateTime ultimaAtualizacao = LocalDateTime.now();
}