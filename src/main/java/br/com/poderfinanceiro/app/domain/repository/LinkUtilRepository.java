package br.com.poderfinanceiro.app.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.poderfinanceiro.app.domain.model.LinkUtilModel;
import br.com.poderfinanceiro.app.domain.model.enums.CategoriaLinkModel;

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

    @Query("SELECT l FROM LinkUtilModel l WHERE " +
            "LOWER(l.tags) LIKE LOWER(CONCAT('%', :tagBanco, '%')) OR " +
            "LOWER(l.tags) LIKE LOWER(CONCAT('%', :tagConvenio, '%')) " +
            "ORDER BY l.titulo ASC")
    List<LinkUtilModel> buscarLinksContextuais(@Param("tagBanco") String tagBanco,
            @Param("tagConvenio") String tagConvenio);
}