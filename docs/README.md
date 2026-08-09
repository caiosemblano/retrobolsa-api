# Documentação Geral do Projeto — RetroBolsa (Cartola Financeiro)

Esta documentação consolida o entendimento completo do projeto RetroBolsa, obtido através da análise de todos os arquivos de documentação (.md) e do escaneamento do código-fonte na estrutura de repositórios.

---

## 1. Visão Geral do Sistema

O **RetroBolsa** (comercialmente denominado **Cartola Financeiro**) é um sistema gamificado de simulação histórica de investimentos e educação financeira. 

O propósito central da plataforma é permitir que usuários aprendam sobre o mercado de ações e ativos de renda fixa participando de competições de investimento baseadas em cenários históricos reais e anonimizados.

### Principais Pilares do Funcionamento:
1. **Anonimização de Ativos**: Durante a montagem da carteira, os ativos reais (ações e títulos públicos) são apresentados por meio de codinomes (ex: "Empresa Alfa", "Título Gama-7") acompanhados apenas de seus indicadores fundamentalistas (P/L, L/VP, ROE, Dividend Yield, D/E, Margem EBITDA, CAGR).
2. **Cenários Históricos e Salto Temporal**: Cada competição estabelece um orçamento fictício (ex: R$ 100.000,00) e um intervalo de anos histórico (ex: 2004 a 2011). O jogador faz a alocação de recursos e o motor de simulação processa a evolução patrimonial com base nos preços reais históricos.
3. **Revelação e Ranking**: Ao término da rodada, a aplicação gera o ranking comparativo de rentabilidade entre os usuários e revela os nomes e tickers reais das empresas investidas (ex: "Empresa Alfa" revela-se "Vale S.A.").

---

## 2. Arquitetura e Tecnologias Utilizadas

O ecossistema é estruturado em uma arquitetura desacoplada cliente-servidor, contendo um backend em Java/Spring Boot e um frontend híbrido (Web React e Mobile React Native).

### 2.1. Backend (`retrobolsa-api`)
* **Linguagem & Framework**: Java 21, Spring Boot 4.0.6 (Parent).
* **Banco de Dados Relacional**: PostgreSQL (produção/desenvolvimento) e H2 (testes em memória).
* **Controle de Migrações DDL**: Flyway (`V1__create_users_table.sql`, `V2__create_game_tables.sql`, `V3__seed_initial_data.sql`).
* **Cache & Sessão**: Redis (orquestrado via Docker Compose).
* **Segurança & Autenticação**: Spring Security + JWT (`io.jsonwebtoken` v0.12.6) de forma stateless com codificação de senhas em BCrypt (`BCryptPasswordEncoder`).
* **ORM e Utilitários**: Hibernate (`spring-boot-starter-data-jpa`), Lombok, Bean Validation.

### 2.2. Frontend Web & Mobile (`retrobolsa-app`)
* **Plataforma Web**: React 18, Vite 6, TypeScript, Tailwind CSS v4.
* **Componentes & UI**: Radix UI (base do shadcn/ui), Material UI v7 (`@mui/material`), Lucide React.
* **Gráficos & Animações**: Recharts (para o gráfico cartesiano de rentabilidade), Framer Motion (`motion` v12).
* **Plataforma Mobile**: React Native 0.85, Expo v56, TypeScript, `react-native-svg`, `lucide-react-native` (localizado em `/retrobolsa-app/mobile`).
* **Comunicação HTTP & Sessão**: Axios com interceptores de injeção de JWT e tratamento automatizado de expiração de token (Auto-Logout no status HTTP 401).

---

## 3. Estrutura do Repositório

O projeto é organizado na raiz em duas aplicações principais e arquivos de documentação técnica:

```text
Fetin - RetroBolsa/
├── docs/                                 # Pasta de documentação centralizada do projeto
│   └── README.md                         # Este documento de visão geral
├── api_documentation.md                  # Análise detalhada do backend
├── frontend_documentation.md             # Análise detalhada do frontend
├── implementation_plan.md                # Planejamento de tasks do backend (SCRUM-17)
├── integration_roadmap.md                # Roteiro de integração Frontend & Backend
├── task_5_context.md                     # Contexto da Task 5 do Jira
├── todo-frontend.md                      # Lista de pendências no frontend
├── retrobolsa-api/                       # Código-fonte do Backend (Spring Boot)
│   ├── docker-compose.yml                # Containers do PostgreSQL e Redis
│   ├── pom.xml                           # Dependências Maven
│   ├── data_model.md                     # Modelagem detalhada do banco de dados
│   └── src/main/java/com/retrobolsa/api/
│       ├── config/                       # CORS, SecurityConfig, GlobalExceptionHandler
│       ├── controller/                   # AuthController, CompetitionController, PortfolioController
│       ├── game/                         # Entidades e regras de jogo (Asset, Competition, Portfolio, SimulationEngine)
│       ├── security/                     # Filtros JWT, JwtUtil, AuthEntryPointJwt
│       ├── user/                         # Entidade User, UserRepository, DTOs de Auth
│       └── validation/                   # Anotação e validador SenhasIguais
└── retrobolsa-app/                       # Código-fonte do Frontend (React + Expo Mobile)
    ├── package.json                      # Dependências da aplicação Web
    ├── src/app/
    │   ├── components/                   # Componentes reutilizáveis e telas (screens/)
    │   ├── contexts/                     # AuthContext (Gestão global de sessão JWT)
    │   ├── services/                     # Chamadas de API (api.ts, authService, portfolioService, etc.)
    │   └── types/                        # Tipagens TypeScript do domínio do jogo
    └── mobile/                           # Aplicação móvel React Native com Expo v56
```

---

## 4. Modelo de Dados e Banco de Dados

O banco de dados relacional é estruturado em torno das seguintes entidades centrais:

1. **`users`**: Cadastro de jogadores contendo `username`, `email`, `password_hash` e `total_score` (pontuação acumulada para o ranking global).
2. **`assets`**: Cadastro dos ativos financeiros com seus nomes reais (`real_name`, `real_ticker`) e codinomes anônimos (`anonymous_alias`, `sector`).
3. **`asset_snapshots`**: Indicadores fundamentais e preços históricos por ativo e ano (`year`, `price`, `pe_ratio`, `dividend_yield`, `roe`, `debt_to_equity`).
4. **`competitions`**: Rodadas de competição (`start_year`, `end_year`, `budget`, `status`: `DRAFT`, `OPEN`, `CLOSED`, `SIMULATED`, `REVEALED`).
5. **`portfolios`**: Registros de carteiras de cada usuário em uma competição (`total_return`, `ranking`, `submitted_at`).
6. **`allocations`**: Divisão do orçamento do usuário entre os ativos selecionados (`amount_invested`, `percent_weight`).
7. **`modules` & `articles`**: Módulos e lições educacionais para o Hub de Aprendizado.

---

## 5. Fluxo de Navegação e Máquina de Estados do Frontend

O fluxo de telas do usuário é gerenciado de forma interativa:

1. **`HomeScreen`**: Exibe o status da rodada ativa e um resumo do desempenho do usuário na rodada anterior.
2. **`CompetitionContextScreen`**: Apresenta o cenário econômico e os dados macroeconômicos (Inflação, Selic, PIB) antes dos investimentos.
3. **`PortfolioBuilderScreen`**: Interface interativa de montagem da carteira. O jogador distribui o orçamento de R$ 100.000,00 utilizando sliders.
4. **`SimulationWaitScreen`**: Tela de transição simulando a execução do motor financeiro.
5. **`ResultsScreen`**: Exibe a posição obtida no ranking, o gráfico cartesiano de evolução do capital (`RentabilityChart`), a explicação econômica e a revelação dos nomes reais dos ativos.
6. **`LearnScreen`**: Hub de educação financeira com módulos e lições.
7. **`RankingsScreen`**: Tabelas de classificação (Rodada Quinzenal, Temporada e Ranking Geral).
8. **`ProfileScreen`**: Perfil do usuário com conquistas e estatísticas.

---

## 6. Estado Atual do Projeto vs. O Que Falta

Para garantir clareza sobre o nível de maturidade do repositório, a tabela abaixo mapeia a situação atual de cada componente do sistema:

### 6.1. Quadro de Status por Componente

| Módulo / Funcionalidade | Estado no Backend Java | Estado no Frontend React/Mobile | Status da Conexão |
| :--- | :--- | :--- | :--- |
| **Banco de Dados (Flyway)** | **Concluído**: Tabelas criadas (`V1`, `V2`) e populadas (`V3`). | N/A | **Concluído** |
| **Autenticação & Registro** | **Concluído**: Endpoints `/api/auth/*` funcionais com JWT. | **Concluído**: Telas `LoginScreen` e `RegisterScreen` com Axios. | **100% Conectado** |
| **Entidades de Jogo (JPA)** | **Concluído**: Mapeadas (`Asset`, `Competition`, `Portfolio`, etc.). | **Concluído**: Tipagens TypeScript ajustadas em `types/index.ts`. | **Pronto para Consumo** |
| **Controlador de Rodadas** | **Concluído**: `CompetitionController` (`GET /api/competitions/active`). | **Pendente**: Consome estaticamente de `mockData.ts`. | **Pendente Virada no Frontend** |
| **Envio de Carteira** | **Concluído**: `PortfolioController` e `PortfolioService`. | **Pendente**: Tela `PortfolioBuilderScreen` opera em modo simulado local. | **Pendente Virada no Frontend** |
| **Motor de Simulação** | **Concluído**: `SimulationEngine` implementado no Java. | **Pendente**: `ResultsScreen` desenha gráfico com massa mockada. | **Pendente Virada no Frontend** |
| **Hub Educacional** | **Pendente**: Tabela `articles` seeded, falta `ArticleController`. | **Pendente**: `LearnScreen` exibe lições locais estáticas. | **Pendente Backend & Frontend** |
| **Controller de Ranking** | **Em Andamento**: Planejado na Task SCRUM-17 (`GET /api/rankings`). | **Pendente**: `RankingsScreen` exibe lista local estática. | **Pendente Backend & Frontend** |

### 6.2. Resumo: O Que Falta Concluir

1. **Troca do Consumo de Dados no Frontend (Etapa 3)**:
   - Alterar o `competitionService.ts` e o `portfolioService.ts` do frontend para deixarem de retornar dados de `mockData.ts` e passarem a realizar as chamadas HTTP para o `CompetitionController` e `PortfolioController` da API Java.
2. **Finalização do Controller de Rankings (Task SCRUM-17)**:
   - Finalizar o endpoint `GET /api/rankings` no backend Java para retornar a lista de pontuações ordenadas por rodada e ranking geral acumulado (`totalScore`).
3. **Controller de Educação**:
   - Implementar os endpoints de consulta de artigos (`GET /api/articles`) e conclusão de lição (`POST /api/articles/{id}/complete`) para dinamizar a tela `LearnScreen`.

---

## 7. Como Executar a Aplicação Localmente

### 7.1. Executando o Backend (`retrobolsa-api`)
1. Certifique-se de possuir o **JDK 21** e o **Docker** instalados.
2. No terminal, navegue até a pasta `retrobolsa-api` e suba os serviços de banco e cache:
   ```bash
   docker-compose up -d
   ```
3. Inicie o servidor Spring Boot:
   * Windows (PowerShell/CMD): `./mvnw.cmd spring-boot:run`
   * Linux/macOS: `./mvnw spring-boot:run`
4. A API estará acessível em `http://localhost:8080`.

### 7.2. Executando o Frontend Web (`retrobolsa-app`)
1. Navegue até a pasta `retrobolsa-app`.
2. Instale as dependências:
   ```bash
   npm install
   ```
3. Execute o servidor de desenvolvimento Vite:
   ```bash
   npm run dev
   ```
4. Acesse o endereço exibido no terminal (geralmente `http://localhost:5173`).

### 7.3. Executando o Frontend Mobile (`retrobolsa-app/mobile`)
1. Navegue até a pasta `retrobolsa-app/mobile`.
2. Instale as dependências:
   ```bash
   npm install
   ```
3. Inicie o Expo Metro Bundler:
   ```bash
   npm run start
   ```
4. Escaneie o QR Code no app **Expo Go** (Android) ou Câmera (iOS).
