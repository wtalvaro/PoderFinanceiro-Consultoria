package br.com.poderfinanceiro.app.model;

import br.com.poderfinanceiro.app.model.enums.CategoriaLink;
import br.com.poderfinanceiro.app.model.enums.TipoConvenio;
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
public class LinkUtil {

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
    private CategoriaLink categoria = CategoriaLink.OUTROS;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_convenio")
    private TipoConvenio tipo_convenio;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.categoria == null) {
            this.categoria = CategoriaLink.OUTROS;
        }
    }
}