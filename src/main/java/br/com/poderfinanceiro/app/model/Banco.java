package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bancos")
@Getter
@Setter
@NoArgsConstructor
public class Banco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banco_id")
    private Long id;

    @Column(name = "nome_banco", nullable = false, length = 100)
    private String nomeBanco;

    @Column(name = "taxa_media_juros", precision = 5, scale = 2)
    private BigDecimal taxaMediaJuros;

    @Column(name = "taxa_minima", precision = 5, scale = 2)
    private BigDecimal taxaMinima;

    @Column(name = "taxa_maxima", precision = 5, scale = 2)
    private BigDecimal taxaMaxima;

    @Column(name = "comissao_percentual", precision = 5, scale = 2)
    private BigDecimal comissaoPercentual;

    @Column(name = "prazo_maximo")
    private Integer prazoMaximo;

    @Column(name = "link_portal_banco", columnDefinition = "TEXT")
    private String linkPortalBanco;

    @Column(name = "sistema_amortizacao", length = 50)
    private String sistemaAmortizacao = "Price";

    @Column(name = "permite_pos_fixado")
    private Boolean permitePosFixado = false;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}