package br.com.poderfinanceiro.app.domain.strategy;

public interface DocumentStrategy {

    boolean supports(String convenio);

    String getChecklist();

}
