package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tabelas_juros")
@Getter
@Setter
@NoArgsConstructor
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
    private Integer idadeMinima = 18; // Alinhado com o DEFAULT do banco

    @Column(name = "idade_maxima")
    private Integer idadeMaxima = 100;

    @Column(name = "renda_minima", precision = 12, scale = 2)
    private BigDecimal rendaMinima = BigDecimal.ZERO;

    @Column(name = "prazo_maximo")
    private Integer prazoMaximo = 96;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}