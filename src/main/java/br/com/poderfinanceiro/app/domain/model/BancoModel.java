package br.com.poderfinanceiro.app.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bancos")
@Getter
@Setter
@NoArgsConstructor
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

    // 1. O MAPEAMENTO (Usando EAGER para a tela conseguir ler sem tomar
    // LazyException)
    @OneToMany(mappedBy = "banco", fetch = FetchType.EAGER)
    private List<TabelaJurosModel> tabelas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }

    // 2. A BLINDAGEM CIRÚRGICA DE EXCLUSÃO
    @PreRemove
    protected void onRemove() {
        if (this.tabelas != null && !this.tabelas.isEmpty()) {
            throw new IllegalStateException(
                    "Segurança Operacional: Não é possível excluir um Banco que possui Tabelas de Juros atreladas. Remova as tabelas primeiro.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BancoModel that))
            return false;

        // A identidade de um banco é o seu código (ex: 001, 341).
        // Se o código for igual, é o mesmo banco, esteja ele no banco de dados ou na
        // memória.
        return java.util.Objects.equals(this.codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        // O hashCode DEVE usar os mesmos campos do equals para manter o contrato do
        // Java.
        return java.util.Objects.hash(codigo);
    }

}