# Contexto para a TASK 5 — Controller de Rankings

## Referência Direta à Task
A forma mais eficiente de se referenciar a esta task é utilizando sua chave do Jira ou o link direto para a issue:
*   Chave: **SCRUM-17**
*   Link direto: [Jira SCRUM-17](https://retrobolsa.atlassian.net/browse/SCRUM-17)

## Descrição da Task no Jira
Implementar `RankingController.java` com o endpoint:
*   `GET /api/rankings` — retorna lista classificatória dos usuários por rodada.
*   Suporte a filtros parciais/totais é desejável.

## Informações do Jira
*   **Status atual:** Em andamento
*   **Responsável:** Caio Semblano Silva
*   **Sprint:** Sprint 2 (Fim em 14/06/2026)
*   **Prioridade:** Média

---

## Modelo de Dados e Contexto do Banco de Dados
A tabela de banco de dados principal envolvida na classificação por rodada é a tabela `portfolios`, criada na migration [V2__create_game_tables.sql](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/resources/db/migration/V2__create_game_tables.sql).

O ranking por rodada é determinado pelos campos:
1.  `rank` (coluna da tabela `portfolios` que armazena a posição da carteira daquele usuário na rodada específica).
2.  `total_return` (retorno total, ex: `1.52` para 152%).
3.  `final_value` (valor final da carteira do usuário).

### Colunas da Tabela `portfolios` Relevantes:
*   `id` (UUID) - Chave primária.
*   `user_id` (UUID) - Relacionado com a tabela `users`.
*   `competition_id` (UUID) - Relacionado com a tabela `competitions` (que representa a rodada).
*   `total_return` (DECIMAL) - Retorno obtido pelo usuário.
*   `final_value` (DECIMAL) - Valor financeiro final.
*   `rank` (INT) - Posição do ranking (1 = melhor).
*   `submitted_at` (TIMESTAMP) - Data de envio da carteira.

### Tabela `users` (Ranking Geral):
Para filtros do ranking global ("all-time"), utiliza-se a tabela `users`.
*   Entidade JPA correspondente: [User.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/user/User.java).
*   Campo relevante: `totalScore` (pontuação acumulada somando todas as rodadas).

---

## Estado Atual da API do RetroBolsa
*   A única entidade JPA mapeada até o momento é o modelo de usuário [User.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/user/User.java).
*   As entidades de jogo (`Portfolio`, `Competition`, `Asset`, etc.) ainda não foram mapeadas na API Java.
*   A task **SCRUM-14** (TASK 1 — Mapear Entidades JPA) está marcada como "Em andamento". Consequentemente, para implementar o `RankingController`, as seguintes classes JPA (ou no mínimo `Portfolio` e `Competition`) precisam estar devidamente mapeadas no código ou mockadas para o endpoint.

---

## Estrutura Proposta de Classes para Implementação
Para implementar a funcionalidade do endpoint `GET /api/rankings`, serão necessárias as seguintes classes:

1.  **Novas Entidades JPA (Dependência da Task 1 / SCRUM-14):**
    *   `Competition.java` (mapeada para a tabela `competitions`)
    *   `Portfolio.java` (mapeada para a tabela `portfolios`, com relacionamentos `@ManyToOne` para `User` e `Competition`)

2.  **Repositórios Spring Data JPA:**
    *   `PortfolioRepository.java` contendo métodos para busca ordenada por rodada:
        ```java
        List<Portfolio> findByCompetitionIdOrderByRankAsc(UUID competitionId);
        List<Portfolio> findByCompetitionRoundNumberOrderByRankAsc(int roundNumber);
        ```

3.  **DTOs (Data Transfer Objects):**
    *   `RankingResponseDto.java` contendo campos como username, posição no ranking, retorno total, valor final e número da rodada.

4.  **Camada de Negócio (Service):**
    *   `RankingService.java` para buscar os portfolios e mapear para os DTOs correspondentes, além de gerenciar a lógica de ordenação e filtros.

5.  **Controlador REST:**
    *   `RankingController.java` mapeando a rota `GET /api/rankings`.

### Filtros Desejáveis:
*   Filtro por ID da competição (`competitionId`).
*   Filtro por número da rodada (`roundNumber`).
*   Opção de listagem do ranking global de usuários (`GET /api/rankings/global` ou query parameter `?type=global`), utilizando a ordenação decrescente por `totalScore` da tabela `users`.
