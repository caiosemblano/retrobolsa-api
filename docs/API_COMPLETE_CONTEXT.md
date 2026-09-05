# Contexto Completo da RetroBolsa API

## 1. Objetivo

A RetroBolsa API é o backend do simulador histórico de investimentos RetroBolsa. Usuários escolhem uma carteira de ativos anonimizados dentro de uma competição histórica. A API valida a carteira, aplica os retornos anuais armazenados no banco, salva o resultado e disponibiliza a evolução do patrimônio e a revelação dos ativos.

O projeto atual é um backend Spring Boot. O frontend é documentado separadamente e, em parte, ainda utiliza dados mockados.

## 2. Tecnologias

- Java 21.
- Spring Boot 4.0.6.
- Spring MVC para endpoints REST.
- Spring Data JPA e Hibernate para persistência.
- PostgreSQL para produção.
- Flyway para migrations.
- Redis configurado para cache, ainda sem uso relevante no fluxo atual.
- Spring Security com JWT.
- BCrypt para senhas.
- Maven Wrapper para build.
- H2 e Testcontainers para testes.

## 3. Como a aplicação funciona

1. O usuário cria uma conta.
2. A senha é armazenada com BCrypt, nunca em texto puro.
3. O usuário faz login e recebe um JWT.
4. O frontend envia o token JWT no header `Authorization`, usando o esquema de autenticação Bearer.
5. O filtro JWT valida o token e carrega o usuário pelo email.
6. O usuário consulta a competição aberta e seus ativos.
7. Envia os valores que deseja investir em cada ativo.
8. A API valida a rodada, os ativos e o orçamento.
9. O motor de simulação aplica os snapshots anuais.
10. A carteira é persistida e sua posição é recalculada.
11. O resultado retorna patrimônio final, rentabilidade, retorno anualizado, gráfico e ativos revelados.

## 4. Endpoints existentes

### Cadastro

`POST /api/auth/register`

Exemplo:

```json
{
  "username": "investidor",
  "email": "investidor@example.com",
  "senha": "SenhaForte123",
  "confirmarSenha": "SenhaForte123"
}
```

Retorno: `201 Created`, sem corpo.

Validações principais:

- username, email, senha e confirmação obrigatórios;
- email válido;
- senha com pelo menos 8 caracteres;
- confirmação igual à senha;
- email e username únicos.

### Login

`POST /api/auth/login`

Exemplo:

```json
{
  "email": "investidor@example.com",
  "senha": "SenhaForte123"
}
```

Retorno:

```json
{
  "token": "<jwt>",
  "type": "Bearer",
  "expiresIn": 86400000
}
```

### Competição ativa

`GET /api/competitions/active`

Requer JWT. Retorna a competição com status `open`, orçamento, período histórico, cenário e ativos anonimizados. Para ações, retorna indicadores do ano inicial; para títulos, retorna a taxa disponível.

### Envio de carteira

`POST /api/portfolios`

Requer JWT.

Exemplo:

```json
{
  "competitionId": "uuid-da-competicao",
  "allocations": [
    {
      "assetId": "uuid-do-ativo",
      "amount": 30000
    },
    {
      "assetId": "uuid-de-outro-ativo",
      "amount": 20000
    }
  ]
}
```

Regras atuais:

- competição deve existir e estar aberta;
- cada ativo deve pertencer à competição;
- valores devem ser positivos;
- o total não pode exceder o orçamento;
- o usuário só pode enviar uma carteira por competição;
- saldo não investido fica em caixa com retorno de 0%;
- snapshots históricos precisam existir.

Retorno: `201 Created`, com mensagem e possíveis avisos de saldo parado.

### Último resultado

`GET /api/portfolios/my-last-result`

Requer JWT. Retorna:

- posição no ranking;
- rentabilidade total;
- retorno anualizado;
- valor final;
- série anual para gráfico;
- ativos, nomes reais, tickers e valores finais;
- período da competição.

## 5. Segurança

O sistema usa sessão stateless. CSRF está desabilitado porque a autenticação é feita por header JWT. Rotas de autenticação são públicas; as demais exigem autenticação.

O token contém o email do usuário como subject, tem validade configurada em `application.properties` e é assinado com HMAC. A senha usa BCrypt.

As origens CORS atualmente permitidas são:

- `http://localhost:3000`
- `http://localhost:5173`
- `http://localhost:19006`

Não existem papéis/perfis administrativos implementados. Também não há recuperação de senha, refresh token, revogação de token ou limitação de tentativas de login.

## 6. Estrutura de código

```text
src/main/java/com/retrobolsa/api/
├── config/
│   ├── CorsConfig.java
│   ├── GlobalExceptionHandler.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── CompetitionController.java
│   └── PortfolioController.java
├── game/
│   ├── asset/
│   ├── competition/
│   ├── dto/
│   ├── portfolio/
│   └── simulation/
├── security/
├── service/
├── user/
└── validation/
```

Os controllers recebem HTTP, os services concentram regras de negócio, os repositories acessam o banco e os DTOs impedem que as entidades sejam expostas diretamente nas respostas.

## 7. Modelo de dados

### `users`

Usuários, email, username, hash da senha, pontuação e data de criação.

### `assets`

Ativos financeiros com nome anônimo, nome real, ticker, tipo, setor e tipo de título.

### `asset_snapshots`

Indicadores e retornos por ativo e ano. Contém P/L, ROE, dividend yield, taxa, retorno anual e indicadores adicionais.

### `competitions`

Rodadas, status, orçamento, cenário, ano inicial, ano final e dias restantes.

### `competition_assets`

Relacionamento entre rodadas e ativos disponíveis.

### `portfolios`

Carteira de um usuário em uma rodada, retorno total, valor final, ranking e data de envio.

### `allocations`

Valores investidos por ativo em uma carteira.

### Educação

`modules`, `articles` e `user_article_progress` já existem nas migrations e possuem seed inicial, mas ainda não têm entidades, repositories, services ou controllers implementados.

## 8. Migrations e dados iniciais

- `V1`: tabela de usuários.
- `V2`: tabelas do jogo e educação.
- `V3`: competição histórica Brasil 2004–2011, ativos, snapshots e artigos iniciais.
- `V4`: indicadores adicionais e snapshots complementares.

Existe um ponto importante para banco PostgreSQL novo: `V1` usa `gen_random_uuid()` e a extensão `pgcrypto` só é habilitada em `V2`. A extensão deve ser habilitada antes da criação de `users`, ou a migration deve ser reorganizada, para garantir que o primeiro `migrate` funcione em uma instalação limpa.

## 9. Motor de simulação

O `SimulationEngine` começa com o orçamento total, separa o valor não investido como caixa e aplica o `annual_return` de cada snapshot entre `startYear` e `endYear`.

O gráfico começa no ano inicial com o orçamento original e adiciona um ponto para cada ano. O retorno anualizado é calculado pela fórmula de CAGR:

```text
(valor_final / orçamento) ^ (1 / quantidade_de_anos) - 1
```

O contrato deve deixar explícito que `totalReturn`/`rentability` atualmente representa um multiplicador, por exemplo `1.52`, e não necessariamente o percentual formatado `152%`.

## 10. Tratamento de erros

O handler global retorna:

- `400` para `IllegalArgumentException`;
- `400` para erros de validação, com campo e mensagem;
- `500` para exceções não tratadas;
- `401` para chamadas sem autenticação válida.

A API ainda não possui um formato de erro baseado em código estável, correlation ID ou tratamento específico para falhas de banco e conflitos de concorrência.

## 11. Estado atual

### Implementado

- cadastro;
- login;
- JWT;
- BCrypt;
- CORS;
- competição ativa;
- entidades do núcleo do jogo;
- envio de carteira;
- simulação histórica;
- resultado da carteira;
- ranking básico por competição;
- migrations e seed inicial;
- testes básicos de contexto e autenticação.

### Parcial

- integração com frontend;
- ranking;
- Redis;
- revelação de ativos;
- indicadores históricos;
- documentação de integração.

### Ainda não implementado

- endpoints de módulos e artigos;
- conclusão de artigos e progresso;
- ranking geral, temporada e quinzenal;
- perfil e conquistas;
- score acumulado;
- administração de competições;
- fechamento automático de rodadas;
- atualização dinâmica de `days_left`;
- refresh/revogação de tokens;
- observabilidade e health checks;
- OpenAPI/Swagger.

## 12. Melhorias recomendadas

### Prioridade alta

1. Habilitar `pgcrypto` antes da `V1`.
2. Remover segredo JWT fixo do arquivo e usar variável de ambiente.
3. Corrigir a revelação de nomes reais para acontecer somente depois do encerramento da competição.
4. Corrigir o snapshot ausente do Tesouro Selic em 2011.
5. Transformar UUID inválido em resposta `400`, em vez de `500`.
6. Impedir ativo duplicado na mesma submissão.
7. Adicionar constraints de valores positivos, pesos e unicidade de ativo por carteira.
8. Tratar concorrência na submissão e no recalculo de ranking.
9. Executar testes de integração contra PostgreSQL real.

### Prioridade média

1. Criar entidades e endpoints de educação.
2. Implementar ranking, perfil, score e conquistas.
3. Adicionar OpenAPI/Swagger.
4. Adicionar Spring Boot Actuator e health checks de PostgreSQL/Redis.
5. Substituir `System.out` por logging estruturado.
6. Corrigir consultas N+1 e criar índices.
7. Externalizar CORS e configurações por ambiente.
8. Definir claramente arredondamento e unidade dos retornos financeiros.
9. Usar Redis para competição ativa e rankings, ou remover a dependência enquanto não for necessário.

### Produção

- HTTPS obrigatório;
- secrets manager;
- rate limiting;
- auditoria de ações;
- backup e restauração testada do PostgreSQL;
- CI com build, testes, migrations e análise de dependências;
- métricas, logs centralizados e alertas;
- política de retenção de dados;
- revisão jurídica e metodológica dos dados históricos.

## 13. Execução local

Pré-requisitos:

- Java compatível com o `pom.xml` — atualmente Java 21;
- Docker Desktop com Docker Compose;
- WSL2 configurado quando o Docker Desktop usar o backend WSL.

Com os serviços disponíveis:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

A configuração atual do projeto usa PostgreSQL em `localhost:5433`, Redis em `localhost:6379` e API em `localhost:8081`, conforme `docker-compose.yml` e `application.properties`. As migrations são executadas automaticamente na inicialização.

## 14. Testes

Comandos úteis:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Dtest=ApiApplicationTests test
.\mvnw.cmd clean package -DskipTests
```

Os testes com Testcontainers exigem Docker Engine funcionando. O teste de contexto usa H2 e serve apenas como verificação parcial; a validação mais importante é executar as migrations e os fluxos contra PostgreSQL real.
