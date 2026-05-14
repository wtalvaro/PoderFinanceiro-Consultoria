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
public class ComissaoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comissao_id")
    private Long id;

    // Relacionamento com a proposta (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id", nullable = false)
    private PropostaModel proposta;

    // Relacionamento com o consultor/usuário (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioModel usuario;

    // --- BLOCO FINANCEIRO ---

    @Column(name = "valor_bruto_comissao", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBrutoComissao;

    @Column(name = "impostos_retidos", precision = 12, scale = 2)
    private BigDecimal impostosRetidos = BigDecimal.ZERO;

    @Column(name = "valor_liquido_consultor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquidoConsultor;

    @Column(name = "valor_pago_pela_poder", precision = 12, scale = 2)
    private BigDecimal valorPagoPelaPoder = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean contestada = false;

    // --- CICLO DE PAGAMENTO (MARCOS TEMPORAIS) ---

    // Marco 1: Recebimento do Banco (Quarta-feira)
    @Column(name = "data_recebimento_banco")
    private LocalDateTime dataRecebimentoBanco;

    // Marco 2: Conferência do Consultor (Quinta-feira)
    @Column(name = "verificado_consultor")
    private boolean verificadoConsultor = false;

    @Column(name = "data_verificacao")
    private LocalDateTime dataVerificacao;

    // Marco 3: Pagamento ao Consultor (Sexta-feira)
    @Column(name = "previsao_pagamento")
    private LocalDate previsaoPagamento;

    @Column(name = "data_pagamento_consultor")
    private LocalDateTime dataPagamentoConsultor;

    // --- STATUS E AUDITORIA ---

    @Column(name = "status_pagamento", length = 20)
    private String statusPagamento = "Pendente";

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}