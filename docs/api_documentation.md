# Documentação e Análise Técnica — RetroBolsa API

Esta documentação apresenta uma análise detalhada da arquitetura, estrutura de dados, fluxo de segurança e estado atual do desenvolvimento do backend da **RetroBolsa API**, um sistema gamificado de investimentos.

---

## 1. Visão Geral do Sistema

A **RetroBolsa API** é desenvolvida utilizando o ecossistema **Java 21** e **Spring Boot 4.0.6**. O sistema é projetado sob os pilares de uma arquitetura limpa, segurança baseada em tokens descentralizados (**JWT**), persistência relacional com **PostgreSQL**, controle de migrações com **Flyway**, e suporte a cache com **Redis**.

O propósito central da aplicação é permitir que usuários participem de competições de investimento baseadas em cenários históricos reais anonimizados. No entanto, conforme demonstrado no mapeamento do banco de dados e nos códigos de backend, o sistema encontra-se atualmente focado na **infraestrutura de segurança, autenticação e gerenciamento de usuários**, servindo de base para o desenvolvimento das regras de negócio financeiras.

---

## 2. Tecnologias e Dependências

As principais tecnologias declaradas no arquivo [pom.xml](file:///c:/Users/sembl/dev/Fetin%20-%20RetroBolsa/retrobolsa-api/pom.xml) incluem:

*   **Linguagem & Framework**: Java 21 & Spring Boot 4.0.6 (Parent).
*   **Acesso a Banco de Dados**:
    *   `spring-boot-starter-data-jpa` (Hibernate) para mapeamento objeto-relacional (ORM).
    *   `spring-boot-starter-flyway` + `flyway-database-postgresql` para controle de migrações de esquema.
    *   Driver JDBC oficial do `postgresql`.
    *   Banco de dados `H2` para execução de testes em memória.
*   **Segurança**:
    *   `spring-boot-starter-security` para autenticação e autorização de requisições.
    *   `io.jsonwebtoken` (JJWT v0.12.6) para geração, validação e decodificação de tokens JWT.
*   **Armazenamento de Sessão/Cache**:
    *   `spring-boot-starter-data-redis` para cache ou gerenciamento de estado rápido.
*   **Utilitários & Validação**:
    *   `lombok` para redução de código boilerplate (Getters, Setters, Builders, etc.).
    *   `spring-boot-starter-validation` para validação declarativa de payloads e parâmetros (Bean Validation).

---

## 3. Estrutura de Pastas e Pacotes

A estrutura do projeto segue o padrão convencional do Spring Boot:

```text
retrobolsa-api/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/retrobolsa/api/
│   │   │   ├── ApiApplication.java             # Ponto de entrada do Spring Boot
│   │   │   ├── config/                         # Configurações globais (CORS, Segurança, Exceptions)
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/                     # Controladores REST (Exposição de Endpoints)
│   │   │   │   └── AuthController.java
│   │   │   ├── exception/                      # Modelagem de erros customizados
│   │   │   │   └── ErrorResponse.java
│   │   │   ├── security/                       # Regras e filtros do JWT e Spring Security
│   │   │   │   ├── AuthEntryPointJwt.java
│   │   │   │   ├── AuthTokenFilter.java
│   │   │   │   └── JwtUtil.java
│   │   │   ├── service/                        # Classes de lógica de negócios
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── UserService.java
│   │   │   ├── user/                           # Entidades JPA, Repositórios e DTOs de Usuário
│   │   │   │   ├── User.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── dto/
│   │   │   │       ├── AuthResponse.java       # DTO de resposta estruturada com o token e expiração
│   │   │   │       ├── LoginRequest.java
│   │   │   │       └── RegisterRequest.java
│   │   │   └── validation/                     # Validadores personalizados (Bean Validation)
│   │   │       ├── SenhasIguais.java
│   │   │       └── SenhasIguaisValidator.java
│   │   └── resources/
│   │       ├── application.properties          # Configurações de ambiente (Banco, JWT, Redis)
│   │       └── db/migration/                   # Scripts de Migração do Flyway
│   │           ├── V1__create_users_table.sql  # Tabela users
│   │           ├── V2__create_game_tables.sql  # Tabelas do motor de jogo (ativos, rodadas, alocações, educação)
│   │           └── V3__seed_initial_data.sql   # População inicial (Rodada 1 Brasil 2004-2011, artigos e lições)
│   └── test/java/com/retrobolsa/api/
│       └── ApiApplicationTests.java            # Testes básicos de carregamento de contexto
├── docker-compose.yml                          # Orquestração local do Postgres e Redis
├── pom.xml                                     # Gerenciador de dependências Maven
└── data_model.md                               # Documentação de modelagem do banco de dados (Alvo)
```

---

## 4. Análise Detalhada dos Componentes

### 4.1. Módulo de Usuário (`com.retrobolsa.api.user`)
*   **`User.java`**: Entidade JPA mapeada para a tabela `users`.
    *   Utiliza IDs do tipo **UUID** (`java.util.UUID`) gerados automaticamente.
    *   Campos mapeados: `id`, `username`, `email`, `passwordHash`, `totalScore` (pontuação total acumulada) e `createdAt`.
    *   Usa anotações do Lombok para gerar automaticamente construtores, getters, setters e um padrão Builder com valores padrão (`@Builder.Default`) definindo `totalScore = 0` e `createdAt = LocalDateTime.now()`.
*   **`UserRepository.java`**: Interface de persistência estendendo `JpaRepository`.
    *   Oferece buscas customizadas: `findByEmail`, `findByUsername`, `existsByEmail` e `existsByUsername`.
*   **DTOs (`dto/`)**:
    *   `RegisterRequest.java`: Dados de registro recebendo `username`, `email`, `senha` e `confirmarSenha`. É validado no nível de classe com a anotação personalizada `@SenhasIguais`.
    *   `LoginRequest.java`: Dados de login recebendo `email` e `senha` com validação de tamanho consistente (mínimo 8 caracteres).
    *   `AuthResponse.java` (Novo): Objeto JSON retornado após login bem-sucedido contendo os campos `token` (JWT string), `type` ("Bearer") e `expiresIn` (tempo de expiração em milissegundos).

### 4.2. Fluxo de Autenticação e Segurança (`com.retrobolsa.api.security` & `com.retrobolsa.api.config`)
A segurança do sistema é configurada na classe **`SecurityConfig.java`**, que define políticas **Stateless** (sem sessão no servidor) e adiciona o filtro JWT antes da validação de formulários tradicionais:

1.  **Filtro Customizado (`AuthTokenFilter`)**:
    *   Executado a cada requisição (`OncePerRequestFilter`).
    *   Extrai o cabeçalho `Authorization` e busca pelo prefixo `Bearer `.
    *   Se o token estiver presente e for válido (verificado pelo `JwtUtil`), extrai o email do usuário.
    *   Usa o **`CustomUserDetailsService`** para carregar os dados cadastrais (email e hash da senha) e cria um objeto `UsernamePasswordAuthenticationToken` injetando-o no `SecurityContextHolder`.
2.  **Gerenciador de Tokens (`JwtUtil`)**:
    *   Lê a chave secreta e o tempo de expiração do arquivo `application.properties`.
    *   Utiliza algoritmos modernos de criptografia simétrica baseados em chaves HMAC-SHA (`Keys.hmacShaKeyFor`).
    *   Injeta o email do usuário como o `Subject` do token JWT.
    *   Contém tratamentos robustos de exceção para tokens malformados, expirados, não suportados ou com assinaturas inválidas.
    *   Expõe o método público `getExpirationMs()` para cálculo do tempo restante de expiração do token em milissegundos.
3.  **Tratamento de Acesso Negado (`AuthEntryPointJwt`)**:
    *   Componente que captura exceções de autenticação e responde com `401 Unauthorized` quando um endpoint seguro é acessado sem credenciais válidas.
4.  **Codificação de Senhas**:
    *   Injeta o `BCryptPasswordEncoder` no contexto do Spring para garantir o hashing unidirecional seguro das senhas durante o registro e validação no login.
5.  **CORS (`CorsConfig`)**:
    *   Configurado para permitir origens do ecossistema local do projeto: `http://localhost:5173` (Vite) e `http://localhost:19006` (Expo Web para simulador mobile).
    *   Permite os métodos HTTP `GET`, `POST`, `PUT`, `DELETE`, `PATCH` e `OPTIONS`, e autoriza o envio de cookies/cabeçalhos de autorização (`allowCredentials(true)`).

### 4.3. Validação Personalizada (`com.retrobolsa.api.validation`)
O sistema implementa uma validação elegante de correspondência de senhas no cadastro utilizando reflexão do Bean Validation API:
*   **`@SenhasIguais`**: Interface de anotação aplicável apenas em classes (`ElementType.TYPE`).
*   **`SenhasIguaisValidator`**: Classe validadora que verifica se o campo `senha` é diferente de nulo e igual ao campo `confirmarSenha` contido no `RegisterRequest`. Caso falhe, retorna a mensagem customizada `"Senhas não coincidem"`.

### 4.4. Controle de Exceções (`com.retrobolsa.api.config` & `exception`)
O **`GlobalExceptionHandler`** captura falhas centralizadas geradas pelos controllers REST:
*   `Exception.class` (Genérica): Retorna status `500 Internal Server Error` com a mensagem genérica `"Aconteceu um erro interno..."`.
*   `IllegalArgumentException.class`: Retorna status `400 Bad Request` com a mensagem explícita da exceção.
*   `MethodArgumentNotValidException.class` (Falhas de validação dos DTOs): Captura e formata erros de validação de campo do Bean Validation. Retorna status `400 Bad Request` com um JSON estruturado contendo a lista dos campos com problemas e suas respectivas mensagens individuais, eliminando a exibição de logs e mensagens brutas internas do Spring.
*   As respostas seguem a modelagem da classe **`ErrorResponse.java`**, que unifica as mensagens de erro retornando os atributos: `status` (inteiro), `erro` (que pode ser uma String descritiva ou o array formatado de validações) e `timestamp` (`LocalDateTime`).

---

## 5. Banco de Dados e Mapeamento: Estado Atual vs. Alvo

O banco de dados relacional foi totalmente expandido através de novas migrações estruturais do Flyway. Abaixo mapeamos o estado das tabelas no banco de dados e sua modelagem correspondente no código Java (JPA):

| Entidade / Tabela | Descrição e Propósito no Sistema | Status no Banco de Dados (Flyway) | Status no Código (JPA) |
| :--- | :--- | :--- | :--- |
| **`users`** | Cadastro de jogadores, e-mail, senha criptografada (BCrypt) e score total. | **Implementada** (`V1__create_users_table.sql`) | **Implementada** (`User.java`) |
| **`assets`** | Ativos financeiros com nomes reais e codinomes anônimos para o jogo. | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`Asset.java`) |
| **`asset_snapshots`** | Dados e indicadores fundamentalistas históricos por ativo e ano (P/L, DY, ROE, etc.). | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`AssetSnapshot.java`) |
| **`competitions`** | Configurações das rodadas (orçamento, descrição do cenário, ano inicial e final). | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`Competition.java`) |
| **`competition_assets`** | Associação N:N entre rodadas de competição e ativos financeiros correspondentes. | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`Competition.java`) |
| **`portfolios`** | Carteira do usuário em uma rodada contendo o retorno total obtido e o ranking final. | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`Portfolio.java`) |
| **`allocations`** | Tabela N:N que descreve as parcelas e pesos de cada ativo na carteira do jogador. | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`Allocation.java`) |
| **`modules`** | Módulos educativos agrupadores de lições financeiras. | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`Module.java`) |
| **`articles`** | Artigos e lições educativas sobre investimentos associados a um módulo. | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`Article.java`) |
| **`user_article_progress`** | Controle N:N do progresso e conclusão das lições educativas pelos usuários. | **Implementada** (`V2__create_game_tables.sql`) | **Implementada** (`UserArticleProgress.java`) |

### Observação Estrutural Importante
O Flyway está ativo no projeto (`spring.flyway.enabled=true`) e lê as migrações em `db/migration/`. Atualmente, as migrações `V1`, `V2` e `V3` estão criadas e prontas. Ao iniciar a API, toda a estrutura física de dados é gerada automaticamente no PostgreSQL e populada com os dados iniciais reais em `V3` (incluindo a Rodada 1 Brasil 2004-2011, indicadores de ativos históricos reais anonimizados e os módulos/lições educativas). A correspondência dessas tabelas para o código Java (JPA Entities) está planejada para a **Etapa 2** da integração.

---

## 6. Oportunidades de Melhoria e Inconsistências Detectadas

Durante o processo de integração da Etapa 1, todos os pontos de atenção e inconsistências identificadas inicialmente no código-fonte foram integralmente resolvidos:

1.  **Divergência de Validação de Senha entre Registro e Login**:
    *   *Estado*: **Corrigido e Implementado** ✅
    *   *Resolução*: O parâmetro `min` no `LoginRequest.java` foi atualizado para `@Size(min = 8)` de forma a alinhar-se com a regra de negócios e o fluxo de cadastro do `RegisterRequest.java`.
2.  **Segurança no Retorno de Autenticação (`AuthController.java`)**:
    *   *Estado*: **Corrigido e Implementado** ✅
    *   *Resolução*: A resposta do endpoint de login deixou de retornar a string pura do token. Agora o token JWT é envelopado no DTO estruturado `AuthResponse.java` (retornando um objeto JSON com `token`, `type` e o tempo de expiração em milissegundos).
3.  **Formato de Mensagem de Erros de Validação (`GlobalExceptionHandler.java`)**:
    *   *Estado*: **Corrigido e Implementado** ✅
    *   *Resolução*: O handler de exceção `MethodArgumentNotValidException` foi completamente reescrito. Em vez de despejar mensagens internas do Spring, ele processa e formata os erros de validação retornando um array amigável de campos inválidos e suas respectivas mensagens individuais.

---

## 7. Como Executar a Aplicação Localmente

### Pré-requisitos
*   **JDK 21** instalado e configurado no PATH do sistema.
*   **Docker Desktop** instalado (para subir o banco de dados e o redis via docker-compose).
*   **Maven** (ou utilizar o wrapper `./mvnw` incluso no projeto).

### Passo a Passo

1.  **Subir a Infraestrutura de Banco e Cache**:
    Abra o terminal no diretório da API (`retrobolsa-api`) e execute o Docker Compose para criar os contêineres do PostgreSQL e do Redis em segundo plano:
    ```bash
    docker-compose up -d
    ```
    Isso iniciará o Postgres na porta `5432` e o Redis na porta `6379`, mapeando os volumes para manter os dados persistidos.

2.  **Verificar as Configurações de Conexão**:
    As propriedades de conexão estão especificadas no arquivo `src/main/resources/application.properties` e coincidem com as credenciais padrão do docker-compose:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/retrobolsa_db
    spring.datasource.username=retrobolsa_user
    spring.datasource.password=retrobolsa_password
    ```

3.  **Iniciar a API Spring Boot**:
    Ainda no terminal, compile e inicie o servidor:
    *   No Windows (PowerShell/CMD):
        ```powershell
        ./mvnw.cmd spring-boot:run
        ```
    *   No Linux/macOS:
        ```bash
        ./mvnw spring-boot:run
        ```

4.  **Processo Automático de Inicialização**:
    *   O Spring Boot será carregado na porta `8081`.
    *   O **Flyway** detectará a migração pendente e executará a query DDL para criar a tabela `users`.
    *   O Hibernate validará a integridade do banco de dados com a entidade `User` (`spring.jpa.hibernate.ddl-auto=validate`).
    *   O servidor estará pronto para receber conexões HTTP no endereço `http://localhost:8081`.

---

## 8. Guia de Teste dos Endpoints Disponíveis

Atualmente, existem dois endpoints ativos principais para autenticação sob o caminho `/api/auth/*`. Abaixo estão os exemplos de requisição para testes (utilizando ferramentas como Postman, Insomnia ou extensão Bruno):

### 8.1. Cadastro de Usuário
*   **Endpoint**: `POST http://localhost:8081/api/auth/register`
*   **Headers**: `Content-Type: application/json`
*   **Payload (JSON)**:
    ```json
    {
      "username": "investidor_retro",
      "email": "investidor@exemplo.com",
      "senha": "SenhaForte123",
      "confirmarSenha": "SenhaForte123"
    }
    ```
*   **Resposta Esperada**: `201 Created` (sem corpo).

### 8.2. Login de Usuário
*   **Endpoint**: `POST http://localhost:8081/api/auth/login`
*   **Headers**: `Content-Type: application/json`
*   **Payload (JSON)**:
    ```json
    {
      "email": "investidor@exemplo.com",
      "senha": "SenhaForte123"
    }
    ```

### 8.3 Operações administrativas de rodada

Os endpoints abaixo exigem um usuário com `role = 'ADMIN'`:

* `POST /api/admin/competitions/{id}/close`
* `POST /api/admin/competitions/{id}/simulate`
* `POST /api/admin/competitions/{id}/reveal`
*   **Resposta Esperada**: `200 OK` retornando o objeto DTO JSON da sessão ativa:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJpbnZlc3RpZG9yQGV4ZW1wbG8uY29tIiwiaWF0IjoxNzEzMTY2ODk4LCJleHAiOjE3MTMyNTMyOTh9...",
      "type": "Bearer",
      "expiresIn": 86400000
    }
    ```

---

*Documentação elaborada para suporte e aceleração no desenvolvimento da plataforma RetroBolsa.*
