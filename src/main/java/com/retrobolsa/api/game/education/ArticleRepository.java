package com.retrobolsa.api.game.education;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
    List<Article> findAllByOrderByModule_DisplayOrderAscDisplayOrderAsc();
}
