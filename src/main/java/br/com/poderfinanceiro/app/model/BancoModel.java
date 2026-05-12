package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bancos")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class BancoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banco_id")
    private Long id;

    @Column(name = "codigo_banco", length = 10)
    private String codigo;

    @Column(name = "nome_banco", nullable = false, length = 100)
    private String nome;

    @Column(name = "link_portal_banco", columnDefinition = "TEXT")
    private String sitePortal;

    @Column(name = "telefone_suporte", length = 50)
    private String telefoneSuporte;

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