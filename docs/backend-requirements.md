# Requisitos para o Teste Real (End-to-End)

Para que o simulador RetroBolsa funcione de ponta a ponta sem dados falsos (mocks), as seguintes implementações precisam ser feitas no Backend (Spring Boot):

## 1. Dados Históricos de Mercado
- **Objetivo:** Ter a base de preços reais para que o motor possa calcular a rentabilidade.
- **Ação Necessária:** Criar a modelagem do banco de dados (tabelas como `asset`, `historical_quote`, `indicator`) para armazenar os dados importados (via CSV, XLSX ou API externa).
- **Dados Mínimos:** Data, Ticker (ex: PETR4), Preço de Fechamento (Close). Indicadores extras (P/L, RSI, MACD) podem ser adicionados para dar mais contexto ao usuário durante o jogo.

## 2. O Motor de Simulação (`PortfolioService`)
- **Objetivo:** Calcular a rentabilidade real da carteira submetida pelo jogador.
- **Ação Necessária:** Implementar a lógica no `PortfolioService.submit()`. 
- **Lógica:**
  1. Pegar as datas de início e fim da `Competition`.
  2. Pegar o preço de cada ação alocada na data de início e na data de fim.
  3. Calcular a variação percentual de cada ativo.
  4. Ponderar pelo capital investido em cada ativo para chegar na rentabilidade total da carteira.
  5. Salvar o resultado (`PortfolioResult`) no banco atrelado ao `User`.

## 3. Sistema de Rankings (`RankingController` e `RankingService`)
- **Objetivo:** Exibir a classificação dos jogadores na tela inicial.
- **Ação Necessária:** Criar o endpoint `/api/rankings`.
- **Lógica:** Fazer uma query no banco de dados que agrupe os resultados de uma determinada competição, ordenando de forma decrescente pela rentabilidade total, retornando o Top N (ex: Top 5).

## 4. Perfil do Usuário (`UserController`)
- **Objetivo:** Prover os dados reais para a aba "Perfil".
- **Ação Necessária:** Criar o endpoint `/api/users/profile`.
- **Lógica:** Retornar os dados do usuário, calcular seu total de pontos acumulados, sua melhor posição em rankings históricos e listar as conquistas desbloqueadas salvas no banco.

## 5. Limpeza do Frontend
- Remover o mock (`setTimeout` e dados fixos) no arquivo `mobile/services/userService.ts`.
- Remover o botão provisório de "Pular 15 Dias" no `SimulationWaitScreen.tsx`, ou integrá-lo a uma lógica real de "Avançar o tempo da simulação" caso o jogo permita resolução instantânea para o jogador solitário.
