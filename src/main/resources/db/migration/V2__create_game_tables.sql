-- ============================================================
-- V2 — Criação das tabelas de jogo do RetroBolsa
-- ============================================================
-- Depende de: V1__create_users_table.sql (tabela users)
-- ============================================================

-- Extensão para geração de UUID (já habilitada no Postgres moderno)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- Tabela de ativos financeiros
-- Guarda o nome anônimo usado durante a rodada e o nome real,
-- revelado apenas após o fim da simulação.
-- ------------------------------------------------------------
CREATE TABLE assets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anonymous_name  VARCHAR(50)  NOT NULL,          -- "Empresa A", "Título 1"
    real_name       VARCHAR(100),                    -- "Vale S.A.", "PETR4" — revelado ao final
    ticker          VARCHAR(10),                     -- Ex: VALE3, PETR4
    type            VARCHAR(10)  NOT NULL            -- 'stock' | 'bond'
                    CHECK (type IN ('stock', 'bond')),
    sector          VARCHAR(100),                    -- Ex: "Mineração", "Energia"
    bond_type       VARCHAR(50),                     -- Ex: "Prefixado", "IPCA+", "Selic"
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Snapshots anuais de indicadores fundamentalistas
-- Um registro por ativo por ano, com indicadores e retorno real.
-- ------------------------------------------------------------
CREATE TABLE asset_snapshots (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id        UUID         NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    year            INT          NOT NULL,
    pl              DECIMAL(10, 2),                  -- Preço/Lucro
    roe             DECIMAL(10, 2),                  -- Return on Equity (%)
    dividend_yield  DECIMAL(10, 2),                  -- Dividend Yield (%)
    rate            DECIMAL(10, 4),                  -- Taxa prefixada/IPCA/Selic para títulos
    annual_return   DECIMAL(10, 4),                  -- Retorno real anual para motor de cálculo
    UNIQUE (asset_id, year)
);

-- ------------------------------------------------------------
-- Rodadas de competição
-- Cada rodada representa um período histórico específico.
-- ------------------------------------------------------------
CREATE TABLE competitions (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    round_number         INT          NOT NULL UNIQUE,
    status               VARCHAR(15)  NOT NULL DEFAULT 'open'
                         CHECK (status IN ('open', 'closed', 'simulating')),
    budget               DECIMAL(15, 2) NOT NULL DEFAULT 100000.00,
    scenario_title       VARCHAR(200),
    scenario_description TEXT,
    start_year           INT          NOT NULL,
    end_year             INT          NOT NULL,
    days_left            INT,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabela de junção: ativos disponíveis em cada rodada
CREATE TABLE competition_assets (
    competition_id  UUID NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
    asset_id        UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    PRIMARY KEY (competition_id, asset_id)
);

-- ------------------------------------------------------------
-- Portfólios dos usuários por rodada
-- Um usuário pode ter apenas um portfólio por rodada.
-- ------------------------------------------------------------
CREATE TABLE portfolios (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    competition_id  UUID         NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
    total_return    DECIMAL(10, 4),                  -- Retorno total (ex: 1.52 = 152%)
    final_value     DECIMAL(15, 2),                  -- Valor final da carteira
    rank            INT,                             -- Posição no ranking da rodada
    submitted_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, competition_id)
);

-- ------------------------------------------------------------
-- Alocações individuais de cada portfólio
-- Registra quanto foi investido em cada ativo.
-- ------------------------------------------------------------
CREATE TABLE allocations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id    UUID         NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    asset_id        UUID         NOT NULL REFERENCES assets(id),
    amount_invested DECIMAL(15, 2) NOT NULL,
    percent_weight  DECIMAL(7, 4)  NOT NULL          -- Ex: 0.3000 = 30%
);

-- ------------------------------------------------------------
-- Artigos e módulos educacionais
-- Usado pela LearnScreen para carregar o hub educacional.
-- ------------------------------------------------------------
CREATE TABLE modules (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    icon            VARCHAR(50),
    display_order   INT          NOT NULL DEFAULT 0
);

CREATE TABLE articles (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id       UUID         NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    content         TEXT,
    duration_min    INT          NOT NULL DEFAULT 5,   -- Tempo estimado de leitura em minutos
    display_order   INT          NOT NULL DEFAULT 0
);

-- Progresso de lições por usuário
CREATE TABLE user_article_progress (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id      UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    completed_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, article_id)
);
