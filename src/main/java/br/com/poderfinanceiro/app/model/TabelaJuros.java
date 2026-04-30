package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tabelas_juros")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TabelaJuros {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tabela_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @Column(name = "nome_tabela", nullable = false, length = 100)
    private String nomeTabela;

    @Column(name = "taxa_mensal", nullable = false, precision = 6, scale = 4)
    private BigDecimal taxaMensal;

    @Column(name = "idade_minima")
    private Integer idadeMinima;

    @Column(name = "idade_maxima")
    private Integer idadeMaxima;

    @Column(name = "renda_minima", precision = 12, scale = 2)
    private BigDecimal rendaMinima;

    @Column(name = "prazo_maximo")
    private Integer prazoMaximo;

    @Builder.Default
    @Column(name = "ativo")
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}