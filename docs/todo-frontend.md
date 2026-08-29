Viewed README.md:1-42

Para alinhar o frontend (**retrobolsa-app**) às especificações e contratos implementados no backend, as atualizações e integrações foram concluídas em ambas as versões (Web e Mobile).

Status da Implementação: **CONCLUÍDO (100%)**
- [x] Tipagens TypeScript alinhadas (Web & Mobile)
- [x] Camada de serviços (`services/`) criada e integrada
- [x] Telas integradas com APIs reais (HomeScreen, RankingsScreen, LearnScreen, ProfileScreen, PortfolioBuilderScreen, CompetitionContextScreen, ResultsScreen)
- [x] Autenticação e persistência de token (Web & Mobile)
- [x] Regra de alocação parcial e alertas de warning
- [x] Remoção completa de dados mocados (`mockData.ts`)

---

### 1. Atualização das Tipagens TypeScript (`src/app/types/index.ts`)

#### Ativos e Indicadores Fundamentalistas
O backend fornece 6 indicadores fundamentalistas para ações:
- `pl` (Preço / Lucro)
- `lvp` (Lucro / Valor Patrimonial - L/VP)
- `lucroPositivo` (Booleano: Lucro Positivo ou Negativo)
- `cagrLucro` (CAGR de Lucro)
- `cagrReceita` (CAGR de Receita)
- `margemEbitda` (Margem EBITDA)

A interface `Asset` deve ser atualizada para:
```typescript
export interface Asset {
  id: string;
  type: 'stock' | 'bond';
  anonymousName: string;
  realName?: string;
  ticker?: string;
  sector?: string;
  bondType?: string;
  rate?: number;
  indicators?: {
    pl?: number;
    lvp?: number;
    lucroPositivo?: boolean;
    cagrLucro?: number;
    cagrReceita?: number;
    margemEbitda?: number;
  };
}
```

#### Resposta de Submissão de Carteira
Atualizar o retorno da submissão para capturar mensagens de aviso (warnings de alocação parcial):
```typescript
export interface SubmitPortfolioResponse {
  message: string;
  warnings?: string[];
}
```

---

### 2. Ajustes na Camada de Serviços (`src/app/services/`)

#### `portfolioService.ts`
Atualizar o método `submit` para retornar a resposta contendo avisos:
```typescript
submit: (payload: SubmitPortfolioPayload) =>
  api.post<SubmitPortfolioResponse>('/api/portfolios', payload)
```

---

### 3. Integração e Fluxo nas Telas (`src/app/components/screens/`)

#### A. Tela Inicial (`HomeScreen`)
- Substituir a massa estática do `mockData.ts` por chamadas à API via `competitionService.getActive()` para exibir a rodada ativa.
- Buscar o último resultado do usuário via `portfolioService.getLastResult()` para exibir o card de resumo do desempenho anterior.

#### B. Montagem de Carteira (`PortfolioBuilderScreen`)
- **Exibição dos Novos Indicadores**: Atualizar os cards de ativos (`AssetCard`) para exibir a lista expandida dos 6 indicadores fundamentalistas (P/L, L/VP, Lucro +/-, CAGR Lucro, CAGR Receita, Margem EBITDA).
- **Tratamento de Alocação < 100%**: Permite o envio a qualquer percentual de alocação. Caso o backend retorne a lista de `warnings`, o frontend deve exibir uma notificação (ex: via `sonner` toast) alertando o usuário sobre o valor que permaneceu parado em caixa gerando rentabilidade nula (0%).
- **Envio do Payload**: Formatar as alocações no formato esperado:
  ```json
  {
    "competitionId": "uuid-da-rodada",
    "allocations": [
      { "assetId": "uuid-do-ativo", "amount": 30000.00 }
    ]
  }
  ```

#### C. Tela de Espera e Resultados (`SimulationWaitScreen` & `ResultsScreen`)
- Chamar `portfolioService.getLastResult()` para obter a estrutura de resultados:
  - `rank`: Posição obtida na rodada.
  - `rentability`: Retorno total acumulado.
  - `annualReturn`: Retorno anualizado.
  - `chartData`: Série temporal de evolução patrimonial ano a ano para plotagem no `RentabilityChart`.
  - `revealedAssets`: Lista de ativos com os nomes reais (`realName`), tickers (`VALE3`, `PETR4`) e valores finais de cada investimento.

---

### 4. Tratamento Global de Erros
- Garantir que falhas de rede ou validações vindas do backend (ex: orçamento excedido, rodada encerrada ou tentativa de submissão duplicada) sejam capturadas pelo interceptor do Axios e apresentadas ao usuário via alertas amigáveis na interface.

---

### 5. Regra de Alocação de Carteira (Confirmada)

O backend **aceita submissões com alocação parcial** (abaixo de 100% do orçamento). Quando o jogador não aloca 100% do capital, o backend:
- Processa normalmente a carteira submetida.
- Retorna um campo `warnings` na resposta `SubmitPortfolioResponse` contendo avisos como: "X% do seu capital permaneceu parado em caixa com rentabilidade 0%".
- O frontend deve exibir cada warning como um toast de aviso (cor amarela) via `sonner` (Web) ou `Alert` (Mobile).
- O botão "Confirmar Carteira" deve estar habilitado sempre que o valor alocado for maior que zero (não requer 50% ou 100% mínimo).