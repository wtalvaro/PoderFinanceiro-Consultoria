package br.com.poderfinanceiro.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proponentes")
@Data
@Builder // <--- É esta anotação que habilita o Proponente.builder()
@NoArgsConstructor
@AllArgsConstructor
public class Proponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proponente_id")
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 255)
    private String nomeCompleto;

    @Column(name = "cpf", nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "renda_mensal", precision = 12, scale = 2)
    private BigDecimal rendaMensal;

    @Column(name = "convenio_orgao", length = 100)
    private String convenioOrgao;

    @Column(name = "matricula", length = 50)
    private String matricula;

    @Builder.Default // <--- Importante para evitar NullPointerException
    @Column(name = "data_cadastro", updatable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;
}