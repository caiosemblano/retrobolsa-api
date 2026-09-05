package com.retrobolsa.api.controller;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para testes de integração de controllers. Sobe um Postgres real via
 * Testcontainers (compartilhado entre todos os testes que estendem esta
 * classe) e conecta o Spring nele via @ServiceConnection, o que faz o
 * Flyway rodar as migrations reais em vez de usar H2/create-drop.
 */
@SpringBootTest(properties = {
        // Segredo apenas de teste: em produção JWT_SECRET vem do ambiente, sem default.
        "jwt.secret=segredo-de-teste-retrobolsa-com-mais-de-32-bytes",
        "retrobolsa.ranking.season-size=4"
})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
}
