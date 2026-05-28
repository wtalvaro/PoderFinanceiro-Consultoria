package br.com.poderfinanceiro.app.application.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Data
@NoArgsConstructor
public class TabelaImportadaDTO {

    // Campos mapeados pelo Gemini
    private String banco;
    private String nomeTabela;
    private String tipoConvenio;
    private BigDecimal valorMinimo;
    private BigDecimal valorMaximo;
    private Integer prazoMinimo;
    private Integer prazoMaximo;
    private Integer idadeMinima;
    private Integer idadeMaxima;
    private BigDecimal taxaMensal;
    private BigDecimal comissaoPercentual;
    private String inicioVigenciaCalculado;
    private String fimVigenciaCalculado;

    // Estado da Interface (UI) - gerenciado manualmente
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final BooleanProperty revisado = new SimpleBooleanProperty(false);

    // Construtor all-args manual (substitui @AllArgsConstructor) para adicionar log
    public TabelaImportadaDTO(String banco, String nomeTabela, String tipoConvenio,
            BigDecimal valorMinimo, BigDecimal valorMaximo,
            Integer prazoMinimo, Integer prazoMaximo,
            Integer idadeMinima, Integer idadeMaxima,
            BigDecimal taxaMensal, BigDecimal comissaoPercentual,
            String inicioVigenciaCalculado, String fimVigenciaCalculado) {
        this.banco = banco;
        this.nomeTabela = nomeTabela;
        this.tipoConvenio = tipoConvenio;
        this.valorMinimo = valorMinimo;
        this.valorMaximo = valorMaximo;
        this.prazoMinimo = prazoMinimo;
        this.prazoMaximo = prazoMaximo;
        this.idadeMinima = idadeMinima;
        this.idadeMaxima = idadeMaxima;
        this.taxaMensal = taxaMensal;
        this.comissaoPercentual = comissaoPercentual;
        this.inicioVigenciaCalculado = inicioVigenciaCalculado;
        this.fimVigenciaCalculado = fimVigenciaCalculado;
        log.debug("[TABELA_IMPORTADA_DTO] Criado: banco='{}', nomeTabela='{}'", banco, nomeTabela);
    }

    // Métodos manuais para o JavaFX Property
    public boolean isRevisado() {
        return revisado.get();
    }

    public void setRevisado(boolean rev) {
        log.debug("[TABELA_IMPORTADA_DTO] setRevisado: {} -> {} (banco='{}')", revisado.get(), rev, banco);
        this.revisado.set(rev);
    }

    public BooleanProperty revisadoProperty() {
        return revisado;
    }
}