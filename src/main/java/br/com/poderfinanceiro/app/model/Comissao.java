package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comissoes")
@Getter
@Setter
@NoArgsConstructor
public class Comissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comissao_id")
    private Long id;

    // Relacionamento com a tabela Propostas
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    // Relacionamento com a tabela Usuarios (Qual consultor recebe)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "valor_bruto_comissao", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBrutoComissao;

    @Column(name = "impostos_retidos", precision = 12, scale = 2)
    private BigDecimal impostosRetidos = BigDecimal.ZERO;

    @Column(name = "valor_liquido_consultor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquidoConsultor;

    @Column(name = "data_previsao_pagamento")
    private LocalDate dataPrevisaoPagamento;

    @Column(name = "status_pagamento", length = 20)
    private String statusPagamento = "Pendente";

    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}