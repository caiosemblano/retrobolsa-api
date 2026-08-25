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
   > O PostgreSQL ficará disponível na porta `5433` e o Redis na porta `6379`.

2. Inicie a aplicação Spring Boot:
   
   **Usando o Maven Wrapper (Linux/macOS):**
   ```bash
   ./mvnw spring-boot:run
   ```
   
   **Usando o Maven Wrapper (Windows):**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

3. Para testar em celulares conectados à mesma rede Wi-Fi:
   - descubra o IPv4 do computador com `ipconfig`;
   - inicie o frontend usando o host `0.0.0.0`;
   - configure a URL da API como `http://IP_DO_COMPUTADOR:8081`;
   - acesse o endereço do frontend pelo IP do computador, por exemplo
     `http://192.168.0.10:5173`.

   Se o Windows Firewall solicitar permissão para Java/Node, permita o acesso
   em redes privadas. Cada jogador deve usar uma conta diferente.

---

## Documentação do Projeto

A documentação detalhada da arquitetura, diagrama UML de sequência comportamental e estado de integração está disponível na pasta docs:

* [Visão Geral e Status de Integração](docs/README.md)
* [Diagrama UML Comportamental](docs/UML_COMPORTAMENTAL.md)
* [Visualizador Gráfico HTML do UML](docs/uml_preview.html)
* [Documentação da API](docs/api_documentation.md)
* [Documentação do Frontend](docs/frontend_documentation.md)
* [Roteiro de Integração](docs/integration_roadmap.md)
