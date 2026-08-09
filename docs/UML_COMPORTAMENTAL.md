> [!NOTE]
> **Escopo do Diagrama**: Este diagrama representa a **arquitetura-alvo planejada** para a integração fim a fim do RetroBolsa. 
> 
> * **Já Implementado no Código**: Estrutura física de banco de dados (`V1`, `V2`, `V3`), fluxo completo de autenticação e sessão JWT, entidades JPA (`Competition`, `Portfolio`, `Allocation`, `Asset`), motor de simulação Java (`SimulationEngine`) e controllers REST (`CompetitionController`, `PortfolioController`).
> * **O Que Falta Conectar**: Alterar as chamadas no frontend de `mockData.ts` para consumirem os endpoints REST que já estão prontos na API Spring Boot.

Abaixo está o diagrama de sequência (UML comportamental) que ilustra o fluxo completo de interação entre os atores e os componentes do sistema durante o ciclo de vida de uma rodada de investimentos.

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    actor Jogador
    participant Frontend as Frontend (Web/Mobile)
    participant API as Backend (Spring Boot)
    participant Motor as Motor de Simulação
    participant DB as PostgreSQL

    rect rgb(200, 220, 240)
    Note over Admin, DB: Fase 1 - Preparacao (Status DRAFT / OPEN)
    Admin->>API: Cria nova Competicao (Cenario, Orcamento, Anos)
    API->>DB: INSERT competition (Status DRAFT)
    Admin->>API: Associa Ativos reais a Competicao
    API->>DB: INSERT competition_assets
    Admin->>API: Publica Competicao
    API->>DB: UPDATE competition SET status = 'OPEN'
    end

    rect rgb(220, 240, 200)
    Note over Jogador, DB: Fase 2 - Investimento e Montagem de Carteira
    Jogador->>Frontend: Acessa a rodada ativa (HomeScreen)
    Frontend->>API: GET /api/competitions/active
    API->>DB: SELECT competition WHERE status = 'OPEN'
    DB-->>API: Dados da competicao e IDs de ativos
    API->>DB: SELECT snapshots WHERE year = start_year (Anonimizados)
    DB-->>API: Indicadores fundamentalistas e alias
    API-->>Frontend: Retorna cenario e ativos disponiveis
    Frontend-->>Jogador: Exibe Tela de Cenario e Opcoes de Ativos

    Jogador->>Frontend: Distribui orcamento entre ativos (Sliders)
    Jogador->>Frontend: Submete Carteira (PortfolioBuilderScreen)
    Frontend->>API: POST /api/portfolios (com alocacoes)
    API->>API: Valida soma do orcamento e pesos
    API->>DB: INSERT portfolio e allocations
    API-->>Frontend: 201 Created (Sucesso)
    Frontend-->>Jogador: Exibe tela de simulacao
    end

    rect rgb(240, 220, 200)
    Note over API, DB: Fase 3 - Salto Temporal (Status CLOSED / SIMULATED)
    Note over API: Prazo da rodada expira (ends_at)
    API->>DB: UPDATE competition SET status = 'CLOSED'
    API->>Motor: Dispara calculo da rodada
    Motor->>DB: SELECT allocations de todos os portfolios da rodada
    Motor->>DB: SELECT snapshots WHERE year = end_year (Precos finais)
    Motor->>Motor: Calcula rentabilidade individual de cada carteira
    Motor->>DB: UPDATE portfolios SET total_return, ranking
    API->>DB: UPDATE competition SET status = 'SIMULATED'
    end

    rect rgb(240, 240, 240)
    Note over Jogador, DB: Fase 4 - Resultados e Revelacao (Status REVEALED)
    Jogador->>Frontend: Acessa Resultados (ResultsScreen)
    Frontend->>API: GET /api/portfolios/my-last-result
    API->>DB: SELECT portfolio, allocations
    DB-->>API: Retorna dados da evolucao patrimonial
    API-->>Frontend: Retorna resultado (Sem nomes reais ainda)
    Frontend-->>Jogador: Exibe posicao no ranking e grafico de evolucao
    
    Admin->>API: Revela empresas da rodada
    API->>DB: UPDATE competition SET status = 'REVEALED'
    
    Frontend->>API: GET /api/competitions/active
    API-->>Frontend: status = 'REVEALED' e real_name dos ativos
    Frontend-->>Jogador: Revela identidades reais
    end
```
