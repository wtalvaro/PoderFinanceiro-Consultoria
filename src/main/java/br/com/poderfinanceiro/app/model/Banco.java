package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bancos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Builder.Default
    @Column(name = "sistema_amortizacao", length = 50)
    private String sistemaAmortizacao = "Price";

    @Builder.Default
    @Column(name = "permite_pos_fixado")
    private Boolean permitePosFixado = false;

    @Builder.Default
    @Column(name = "ativo")
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // Relacionamento inverso (opcional, ajuda na navegação do objeto)
    @OneToMany(mappedBy = "banco", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TabelaJuros> tabelasJuros;
}