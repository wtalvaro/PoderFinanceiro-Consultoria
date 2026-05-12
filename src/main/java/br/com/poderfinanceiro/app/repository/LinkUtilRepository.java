package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.LinkUtilModel;
import br.com.poderfinanceiro.app.model.enums.CategoriaLinkModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkUtilRepository extends JpaRepository<LinkUtilModel, Long> {

    /**
     * Retorna todos os links organizados por categoria e depois por título.
     * Ideal para manter a TableView sempre ordenada visualmente.
     */
    List<LinkUtilModel> findAllByOrderByCategoriaAscTituloAsc();

    /**
     * Permite filtrar links de uma categoria específica (Ex: buscar apenas
     * 'BANCO').
     */
    List<LinkUtilModel> findByCategoriaOrderByTituloAsc(CategoriaLinkModel categoria);

    /**
     * Busca rápida por título (ignore case) para filtros de pesquisa na interface.
     */
    List<LinkUtilModel> findByTituloContainingIgnoreCase(String titulo);
}