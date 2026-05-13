package br.com.poderfinanceiro.app.model;

import br.com.poderfinanceiro.app.model.enums.CategoriaLinkModel;
import br.com.poderfinanceiro.app.model.enums.TipoConvenioModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "links_uteis")
@Getter
@Setter
@NoArgsConstructor
public class LinkUtilModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "categoria", columnDefinition = "categoria_link_enum")
    private CategoriaLinkModel categoria = CategoriaLinkModel.OUTROS;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // 💉 A injeção que avisa que isso é um Enum nativo do Postgres
    @Column(name = "tipo_convenio")
    private TipoConvenioModel tipoConvenio;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "tags_busca", length = 255)
    private String tags; // Exemplo de conteúdo: "inss, pan, refinanciamento"

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.categoria == null) {
            this.categoria = CategoriaLinkModel.OUTROS;
        }
    }
}