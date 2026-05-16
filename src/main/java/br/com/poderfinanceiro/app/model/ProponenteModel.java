package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.poderfinanceiro.app.model.enums.OrigemLeadModel;
import br.com.poderfinanceiro.app.model.enums.TipoRelacionamentoModel;
import br.com.poderfinanceiro.app.model.enums.TipoVinculoModel;

@Entity
@Table(name = "proponentes")
@Getter
@Setter
@NoArgsConstructor
public class ProponenteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proponente_id")
    private Long id;

    // O consultor dono deste lead (amarrado via AuthService na hora de salvar)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioModel usuario;

    @Column(name = "nome_completo", nullable = false, length = 255)
    private String nomeCompleto;

    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(name = "renda_mensal", precision = 12, scale = 2)
    private BigDecimal rendaMensal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_vinculo", columnDefinition = "tipo_vinculo_enum")
    private TipoVinculoModel tipoVinculo;

    @Column(length = 50)
    private String matricula;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "origem_consentimento", columnDefinition = "origem_consentimento_enum")
    private OrigemLeadModel origemConsentimento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "classificacao", columnDefinition = "tipo_relacionamento_enum") // Nome EXATO do TYPE no seu SQL
    private TipoRelacionamentoModel classificacao = TipoRelacionamentoModel.LEAD; // Valor padrão

    @Column(name = "data_cadastro", updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicado_por_id")
    private ProponenteModel indicadoPor;

    // Relacionamento com a tabela de Endereços
    // CascadeType.ALL garante que ao salvar o Proponente, o Endereço salva junto
    // orphanRemoval = true garante que se o endereço for removido da lista, ele
    // será deletado do banco
    @OneToMany(mappedBy = "proponente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EnderecoProponenteModel> enderecos = new ArrayList<>();

    // REMOVIDO o cascade e orphanRemoval. O Proponente não salva nem deleta
    // Propostas automaticamente.
    @OneToMany(mappedBy = "proponente", fetch = FetchType.LAZY)
    private List<PropostaModel> propostas = new ArrayList<>();

    @OneToMany(mappedBy = "proponente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocumentoProponenteModel> documentos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
    }
}