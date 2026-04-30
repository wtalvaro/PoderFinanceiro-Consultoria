package br.com.poderfinanceiro.app.repository;

import br.com.poderfinanceiro.app.model.Proponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProponenteRepository extends JpaRepository<Proponente, Long> {
}