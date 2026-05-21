package br.com.poderfinanceiro.app.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.math.BigDecimal;

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

    // Estado da Interface (UI)
    private final BooleanProperty revisado = new SimpleBooleanProperty(false);

    // Getters e Setters Padronizados
    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getNomeTabela() {
        return nomeTabela;
    }

    public void setNomeTabela(String nomeTabela) {
        this.nomeTabela = nomeTabela;
    }

    public String getTipoConvenio() {
        return tipoConvenio;
    }

    public void setTipoConvenio(String tipoConvenio) {
        this.tipoConvenio = tipoConvenio;
    }

    public BigDecimal getValorMinimo() {
        return valorMinimo;
    }

    public void setValorMinimo(BigDecimal valorMinimo) {
        this.valorMinimo = valorMinimo;
    }

    public BigDecimal getValorMaximo() {
        return valorMaximo;
    }

    public void setValorMaximo(BigDecimal valorMaximo) {
        this.valorMaximo = valorMaximo;
    }

    public Integer getPrazoMinimo() {
        return prazoMinimo;
    }

    public void setPrazoMinimo(Integer prazoMinimo) {
        this.prazoMinimo = prazoMinimo;
    }

    public Integer getPrazoMaximo() {
        return prazoMaximo;
    }

    public void setPrazoMaximo(Integer prazoMaximo) {
        this.prazoMaximo = prazoMaximo;
    }

    public Integer getIdadeMinima() {
        return idadeMinima;
    }

    public void setIdadeMinima(Integer idadeMinima) {
        this.idadeMinima = idadeMinima;
    }

    public Integer getIdadeMaxima() {
        return idadeMaxima;
    }

    public void setIdadeMaxima(Integer idadeMaxima) {
        this.idadeMaxima = idadeMaxima;
    }

    public BigDecimal getTaxaMensal() {
        return taxaMensal;
    }

    public void setTaxaMensal(BigDecimal taxaMensal) {
        this.taxaMensal = taxaMensal;
    }

    public BigDecimal getComissaoPercentual() {
        return comissaoPercentual;
    }

    public void setComissaoPercentual(BigDecimal comissaoPercentual) {
        this.comissaoPercentual = comissaoPercentual;
    }

    public String getInicioVigenciaCalculado() {
        return inicioVigenciaCalculado;
    }

    public void setInicioVigenciaCalculado(String inicioVigenciaCalculado) {
        this.inicioVigenciaCalculado = inicioVigenciaCalculado;
    }

    public String getFimVigenciaCalculado() {
        return fimVigenciaCalculado;
    }

    public void setFimVigenciaCalculado(String fimVigenciaCalculado) {
        this.fimVigenciaCalculado = fimVigenciaCalculado;
    }

    // Propriedades do JavaFX
    public boolean isRevisado() {
        return revisado.get();
    }

    public void setRevisado(boolean rev) {
        this.revisado.set(rev);
    }

    public BooleanProperty revisadoProperty() {
        return revisado;
    }
}