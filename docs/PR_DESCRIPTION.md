# PR: Implementação do Core do Jogo, Motor de Simulação, Migrações do Banco e Documentação Completa

## Descrição Resumida
Este Pull Request consolida a evolução da branch `dev` para `main`, introduzindo as regras de negócio de investimentos do RetroBolsa, o mapeamento do modelo de dados relacional (Flyway V1 a V4), o motor de simulação histórica (`SimulationEngine`), os controllers REST de jogo, melhorias de segurança/autenticação JWT, ajustes de infraestrutura e a documentação técnica centralizada com diagrama UML comportamental.

---

## Alterações Incluídas

### 1. Autenticação e Segurança
* **Fluxo de Autenticação**: Aprimorado o retorno do endpoint `/api/auth/login` envelopando o token JWT no DTO estruturado `AuthResponse` com dados de tipo e tempo de expiração em milissegundos.
* **Validação de Payload**: Alinhamento das regras de validação de senha (`@Size(min = 8)`) entre registro e login.
* **Tratamento Global de Exceções**: Reescrita do `GlobalExceptionHandler` para capturar erros de validação do Bean Validation e retornar uma estrutura JSON amigável contendo o par campo e mensagem.
* **Configuração de CORS**: Liberação de origens locais de desenvolvimento web (`:5173`) e mobile (`:19006`).

### 2. Banco de Dados e Migrações (Flyway V1 a V4)
* **`V1__create_users_table.sql`**: Tabela de usuários para autenticação e score global.
* **`V2__create_game_tables.sql`**: Tabelas de ativos (`assets`), snapshots de preços/indicadores (`asset_snapshots`), competições (`competitions`), carteiras (`portfolios`), alocações (`allocations`) e módulos educacionais (`modules`, `articles`).
* **`V3__seed_initial_data.sql`**: Carga inicial de dados contendo a Rodada 1 (Brasil 2004-2011), ativos anonimizados reais e lições teóricas.
* **`V4__add_fundamental_indicators.sql`**: Adição dos 6 indicadores fundamentalistas (P/L, L/VP, Lucro +/-, CAGR Lucro, CAGR Receita, Margem EBITDA) e snapshots complementares.

### 3. Entidades JPA, Repositórios e Serviços de Jogo
* **Mapeamento JPA**: Implementação das entidades JPA no pacote `com.retrobolsa.api.game` (`Asset`, `AssetSnapshot`, `Competition`, `Portfolio`, `Allocation`).
* **Repositórios Spring Data JPA**: Interfaces de persistência com consultas ordenadas por rodada, status e usuaria.
* **Motor de Simulação (`SimulationEngine`)**: Componente responsável por calcular o retorno percentual acumulado e a valorização patrimonial das carteiras comparando os preços do ano inicial e final da rodada.
* **Serviços de Domínio**: `CompetitionService` e `PortfolioService` encapsulando as regras de alocação de orçamento e validação de pesos.

### 4. Controladores REST
* **`CompetitionController`**: Endpoint `GET /api/competitions/active` para consultar a rodada aberta com os ativos anonimizados e dados macroeconômicos do cenário.
* **`PortfolioController`**: Endpoints `POST /api/portfolios` para submissão de carteira e `GET /api/portfolios/my-last-result` para consultar o desempenho da carteira e gráfico histórico.

### 5. Documentação Técnica e Diagramas UML
* **Estrutura em `docs/`**: Centralização de toda a documentação do projeto na pasta `retrobolsa-api/docs/`.
* **Visão Geral (`docs/README.md`)**: Visão arquitetural e quadro de status da integração por componente.
* **UML Comportamental (`docs/UML_COMPORTAMENTAL.md`)**: Diagrama de sequência em Mermaid detalhando a interação entre os atores (Jogador, Admin) e o sistema (Frontend, API, Motor, Banco) nas 4 fases do jogo.
* **Visualizador HTML (`docs/uml_preview.html`)**: Página interativa standalone para pré-visualização do diagrama no navegador.
* **README Principal**: Atualização do `README.md` raiz referenciando a pasta `docs/`.

---

## Como Testar as Alterações

1. **Subir a Infraestrutura**:
   ```bash
   docker-compose up -d
   ```
2. **Executar a API**:
   ```bash
   ./mvnw spring-boot:run
   ```
3. **Verificar a Aplicação de Migrações**:
   * O Flyway aplicará automaticamente as migrações V1, V2, V3 e V4 na inicialização.
4. **Testar os Endpoints REST**:
   * `POST /api/auth/register` (Cadastro de usuário)
   * `POST /api/auth/login` (Obtenção do token JWT no formato `AuthResponse`)
   * `GET /api/competitions/active` (Consulta da rodada ativa e ativos anonimizados)
   * `POST /api/portfolios` (Envio de carteira alocada com cabeçalho JWT)
   * `GET /api/portfolios/my-last-result` (Consulta do resultado da simulação)

---

## Checklist de Revisão
* [x] Código compila sem erros (Java 21 / Maven)
* [x] Migrações DDL do Flyway executando com sucesso
* [x] Endpoints REST testados e validados
* [x] Documentação e diagrama UML atualizados em `docs/`
