package br.com.poderfinanceiro.app.strategy;

public interface DocumentStrategy {

    boolean supports(String convenio);

    String getChecklist();

}
