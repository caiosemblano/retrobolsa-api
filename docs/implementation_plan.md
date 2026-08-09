# Implementar Controller de Rankings (Task 5 - SCRUM-17)

Este plano detalha a implementação do endpoint `GET /api/rankings`, responsável por retornar as listas classificatórias dos usuários do RetroBolsa, com suporte a filtros por rodada e um ranking global.

> [!WARNING]
> ## Dependência Externa (User Review Required)
> O contexto menciona que as entidades `Competition` e `Portfolio` ainda não foram mapeadas na API Java (dependência da Task 1 / SCRUM-14).
> Precisamos decidir: 
> 1. Implementamos essas entidades como parte desta task para que o `RankingController` tenha dados reais do banco?
> 2. Ou criamos mocks temporários no `RankingService` para que a API retorne os dados sem bater no banco, enquanto a SCRUM-14 é finalizada?
> 
> *A proposta abaixo assume a criação das entidades para conectividade real com o banco de dados.*

## Proposed Changes

### Entidades de Banco de Dados (Domínio do Jogo)
Estes arquivos farão o mapeamento das tabelas `competitions` e `portfolios` existentes no banco de dados.

#### [NEW] [Competition.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/competition/Competition.java)
- Criar a entidade mapeando a tabela `competitions`.
- Mapear campos base (como `id`, `round_number`, etc).

#### [NEW] [Portfolio.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/portfolio/Portfolio.java)
- Criar a entidade mapeando a tabela `portfolios`.
- Criar os campos `totalReturn`, `finalValue`, `rank`.
- Estabelecer relacionamentos `@ManyToOne` com `User` e `Competition`.

---

### Camada de Acesso a Dados (Repositories)

#### [NEW] [PortfolioRepository.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/portfolio/PortfolioRepository.java)
- Repositório JPA para buscar carteiras.
- Métodos necessários:
  - `List<Portfolio> findByCompetitionIdOrderByRankAsc(UUID competitionId);`
  - `List<Portfolio> findByCompetitionRoundNumberOrderByRankAsc(int roundNumber);`

#### [MODIFY] [UserRepository.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/user/UserRepository.java)
- Adicionar um método para buscar o ranking global de usuários, ex: `List<User> findAllByOrderByTotalScoreDesc();`

---

### DTOs (Data Transfer Objects)

#### [NEW] [RankingResponseDto.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/ranking/dto/RankingResponseDto.java)
- Classe para retornar o ranking específico de uma rodada contendo os atributos:
  - `username`
  - `rank`
  - `totalReturn`
  - `finalValue`
  - `roundNumber`

#### [NEW] [GlobalRankingResponseDto.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/ranking/dto/GlobalRankingResponseDto.java)
- Classe para o ranking geral (all-time) contendo:
  - `username`
  - `totalScore`

---

### Lógica de Negócio e API

#### [NEW] [RankingService.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/ranking/service/RankingService.java)
- Serviço que orquestra as consultas.
- Lógica para decidir qual repositório chamar baseado nos filtros providos (competição, rodada ou global).
- Mapeamento das entidades (`Portfolio` ou `User`) para os DTOs correspondentes.

#### [NEW] [RankingController.java](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/src/main/java/com/retrobolsa/api/ranking/controller/RankingController.java)
- Controlador mapeando a rota `GET /api/rankings`.
- Definição de `RequestParam` opcionais:
  - `competitionId` (UUID)
  - `roundNumber` (Integer)
  - `type` (String, ex: `global`)
- Validação para garantir que apenas um filtro seja utilizado por vez (ou estabelecer precedência).

## Verification Plan

### Testes Manuais
- Iniciar a aplicação Spring Boot localmente.
- Realizar chamadas para `GET /api/rankings?competitionId={id}` e verificar ordenação por `rank`.
- Realizar chamadas para `GET /api/rankings?roundNumber={num}` e verificar se traz a competição correta.
- Realizar chamada para `GET /api/rankings?type=global` (ou `/api/rankings/global`) e verificar a ordenação decrescente de `totalScore`.

### Testes Automatizados
- Criar testes de integração (`RankingControllerTest.java` ou similar, se houver um padrão de testes estabelecido) cobrindo as 3 variações de consultas usando banco em memória (H2) ou Testcontainers.
