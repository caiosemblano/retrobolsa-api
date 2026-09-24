package com.retrobolsa.api.game.education;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    /**
     * Todos os artigos já com o módulo carregado. Sem o JOIN FETCH, cada
     * {@code article.getModule()} (LAZY) disparava uma query própria ao montar o DTO.
     */
    @Query("""
            SELECT a FROM Article a JOIN FETCH a.module m
            ORDER BY m.displayOrder ASC, a.displayOrder ASC
            """)
    List<Article> findAllByOrderByModule_DisplayOrderAscDisplayOrderAsc();

    long countByModuleId(UUID moduleId);
}
