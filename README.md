# RetroBolsa API

Este é o repositório backend do **Simulador Histórico de Investimentos (RetroBolsa)**.

A aplicação é construída com **Java 17+** e **Spring Boot**, utilizando **PostgreSQL** como banco de dados principal e **Redis** para cache, além do **Flyway** para migrações do banco de dados.

---

## 🛠️ Como Executar a Aplicação (Backend)

Siga os passos abaixo para rodar o backend na sua máquina local. O backend já está configurado para buscar as credenciais do banco de dados no arquivo `application.properties`, então **não é necessário criar um arquivo `.env` para o backend**.

### Pré-requisitos
- **Java 17** ou superior instalado.
- **Docker** e **Docker Compose** instalados (para rodar os serviços de banco de dados e cache).

### Passo a Passo

1. Suba os containers do banco de dados (PostgreSQL) e cache (Redis) usando o Docker:
   ```bash
   docker-compose up -d
   ```
   > Isso criará os serviços configurados na porta `5432` (PostgreSQL) e `6379` (Redis).

2. Inicie a aplicação Spring Boot:
   
   **Usando o Maven Wrapper (Linux/macOS):**
   ```bash
   ./mvnw spring-boot:run
   ```
   
   **Usando o Maven Wrapper (Windows):**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

3. A aplicação estará disponível em `http://localhost:8081`.

> **Nota:** As tabelas e dados iniciais (seeds) serão aplicados automaticamente pelo **Flyway** ao rodar a aplicação.

---
