package br.com.poderfinanceiro.app.application.facade;

/**
 * Interface de Contrato para a Facade de Menu.
 * Mantida conforme as necessidades do MenuController (Humble Object).
 */
public interface IMenuFacade {

    /**
     * Verifica se há uma nova versão e retorna a Tag da versão.
     * 
     * @return String contendo a tag (ex: "v2.1.3") ou null se não houver
     *         atualização.
     * @throws Exception Caso ocorra falha na comunicação com a API.
     */
    String checarNovaVersao() throws Exception;

    /**
     * Inicia o processo de download e preparação da atualização baseada na tag.
     * 
     * @param tag A tag da versão a ser baixada.
     * @throws Exception Caso ocorra erro no I/O ou rede.
     */
    void baixarEExecutarAtualizacao(String tag) throws Exception;
}
