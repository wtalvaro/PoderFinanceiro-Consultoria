package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.LinkUtil;
import br.com.poderfinanceiro.app.model.enums.CategoriaLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkUtilRepository extends JpaRepository<LinkUtil, Long> {

    /**
     * Retorna todos os links organizados por categoria e depois por título.
     * Ideal para manter a TableView sempre ordenada visualmente.
     */
    List<LinkUtil> findAllByOrderByCategoriaAscTituloAsc();

    /**
     * Permite filtrar links de uma categoria específica (Ex: buscar apenas
     * 'BANCO').
     */
    List<LinkUtil> findByCategoriaOrderByTituloAsc(CategoriaLink categoria);

    /**
     * Busca rápida por título (ignore case) para filtros de pesquisa na interface.
     */
    List<LinkUtil> findByTituloContainingIgnoreCase(String titulo);
}