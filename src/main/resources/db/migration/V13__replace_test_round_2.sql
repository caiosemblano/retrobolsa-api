-- ============================================================
-- V13 — Troca a rodada de teste "nova2" por um contexto real
-- ============================================================
-- Em produção a rodada 2 foi criada à mão para teste ("nova2",
-- 2004–2008, reaproveitando os ativos da rodada 1). Ela vira
-- "Nova matriz econômica (2011–2013)", que preenche o intervalo
-- entre a rodada 1 (até 2011) e a Recessão (a partir de 2014).
--
-- As carteiras de teste dessa rodada são apagadas e os pontos que
-- elas deram são devolvidos (mesma regra do ScoringCalculator:
-- pontos = rentabilidade % arredondada). A rodada volta para
-- 'draft', para ser iniciada pela tela de Admin.
--
-- Em bancos sem a "nova2" (ambiente novo), a rodada é criada no
-- próximo round_number livre.
--
-- Mesma convenção do V12: retornos de start_year a end_year - 1,
-- indicadores só no ano inicial, valores aproximados.
-- ============================================================

INSERT INTO assets (id, anonymous_name, real_name, ticker, type, sector) VALUES
    ('cccccccc-0401-0000-0000-000000000401', 'Empresa A', 'Petrobras S.A.',       'PETR4', 'stock', 'Petróleo e Gás'),
    ('cccccccc-0402-0000-0000-000000000402', 'Empresa B', 'OGX Petróleo S.A.',    'OGXP3', 'stock', 'Petróleo e Gás'),
    ('cccccccc-0403-0000-0000-000000000403', 'Empresa C', 'Vale S.A.',            'VALE3', 'stock', 'Mineração'),
    ('cccccccc-0404-0000-0000-000000000404', 'Empresa D', 'Ambev S.A.',           'AMBV4', 'stock', 'Bebidas'),
    ('cccccccc-0405-0000-0000-000000000405', 'Empresa E', 'Banco do Brasil S.A.', 'BBAS3', 'stock', 'Financeiro');

INSERT INTO assets (id, anonymous_name, real_name, type, bond_type) VALUES
    ('cccccccc-0406-0000-0000-000000000406', 'Título 1', 'Tesouro Prefixado 2014', 'bond', 'Prefixado'),
    ('cccccccc-0407-0000-0000-000000000407', 'Título 2', 'Tesouro IPCA+ 2035',     'bond', 'IPCA+'),
    ('cccccccc-0408-0000-0000-000000000408', 'Título 3', 'Tesouro Selic 2017',     'bond', 'Selic');

INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return, lvp, lucro_positivo, cagr_lucro, cagr_receita, margem_ebitda) VALUES
    ('cccccccc-0401-0000-0000-000000000401', 2011,  7.9, 11.2, 3.8, -0.2000, 0.95, true,   0.0500, 0.1500, 28.0),
    ('cccccccc-0402-0000-0000-000000000402', 2011, -62.0, -4.5, 0.0, -0.2000, 3.40, false, NULL,   NULL,   NULL),
    ('cccccccc-0403-0000-0000-000000000403', 2011,  6.9, 30.1, 5.5, -0.1800, 1.60, true,   0.3000, 0.2500, 55.0),
    ('cccccccc-0404-0000-0000-000000000404', 2011, 22.0, 33.4, 3.5,  0.4000, 7.80, true,   0.1200, 0.1000, 44.0),
    ('cccccccc-0405-0000-0000-000000000405', 2011,  6.0, 22.0, 4.5, -0.1800, 1.30, true,   0.1000, 0.1500, NULL);

INSERT INTO asset_snapshots (asset_id, year, annual_return) VALUES
    ('cccccccc-0401-0000-0000-000000000401', 2012, -0.0400),
    ('cccccccc-0401-0000-0000-000000000401', 2013, -0.1200),
    ('cccccccc-0402-0000-0000-000000000402', 2012, -0.7000),
    ('cccccccc-0402-0000-0000-000000000402', 2013, -0.9300),
    ('cccccccc-0403-0000-0000-000000000403', 2012,  0.0800),
    ('cccccccc-0403-0000-0000-000000000403', 2013, -0.0500),
    ('cccccccc-0404-0000-0000-000000000404', 2012,  0.3000),
    ('cccccccc-0404-0000-0000-000000000404', 2013, -0.0200),
    ('cccccccc-0405-0000-0000-000000000405', 2012, -0.0500),
    ('cccccccc-0405-0000-0000-000000000405', 2013, -0.0800);

INSERT INTO asset_snapshots (asset_id, year, rate, annual_return) VALUES
    ('cccccccc-0406-0000-0000-000000000406', 2011, 0.1200, 0.1300),
    ('cccccccc-0406-0000-0000-000000000406', 2012, 0.1000, 0.1400),
    ('cccccccc-0406-0000-0000-000000000406', 2013, 0.0900, 0.0200),
    ('cccccccc-0407-0000-0000-000000000407', 2011, 0.0600, 0.1500),
    ('cccccccc-0407-0000-0000-000000000407', 2012, 0.0500, 0.2500),
    ('cccccccc-0407-0000-0000-000000000407', 2013, 0.0350, -0.1000),
    ('cccccccc-0408-0000-0000-000000000408', 2011, 0.1075, 0.1160),
    ('cccccccc-0408-0000-0000-000000000408', 2012, 0.1100, 0.0840),
    ('cccccccc-0408-0000-0000-000000000408', 2013, 0.0725, 0.0806);

-- Devolve os pontos que as carteiras de teste deram
-- (uma carteira por usuário por rodada, garantido pelo UNIQUE de portfolios)
UPDATE users
SET total_score = GREATEST(0, total_score - (
    SELECT CAST(ROUND(p.total_return) AS INT)
    FROM portfolios p
    JOIN competitions c ON c.id = p.competition_id
    WHERE p.user_id = users.id
      AND c.round_number = 2
      AND c.scenario_title = 'nova2'))
WHERE id IN (
    SELECT p.user_id
    FROM portfolios p
    JOIN competitions c ON c.id = p.competition_id
    WHERE c.round_number = 2
      AND c.scenario_title = 'nova2'
      AND p.total_return IS NOT NULL);

DELETE FROM allocations
WHERE portfolio_id IN (
    SELECT p.id FROM portfolios p
    JOIN competitions c ON c.id = p.competition_id
    WHERE c.round_number = 2 AND c.scenario_title = 'nova2'
);

DELETE FROM portfolios
WHERE competition_id IN (SELECT id FROM competitions WHERE round_number = 2 AND scenario_title = 'nova2');

DELETE FROM competition_assets
WHERE competition_id IN (SELECT id FROM competitions WHERE round_number = 2 AND scenario_title = 'nova2');

UPDATE competitions
SET status               = 'draft',
    scenario_title       = 'Nova matriz econômica (2011–2013)',
    scenario_description = 'Depois de crescer 7,5% em 2010, o Brasil quer mais. O governo força a queda dos juros até a Selic de 7,25%, a menor da história até então, segura o preço da gasolina e da energia e aposta no crédito dos bancos públicos. Enquanto isso, a empresa de petróleo de um dos homens mais ricos do mundo promete bilhões de barris no pré-sal. Mas a inflação teima em subir, e em 2013 os juros voltam a disparar. Em quem você confiaria?',
    budget               = 100000.00,
    start_year           = 2011,
    end_year             = 2014,
    days_left            = 7,
    ends_at              = NULL
WHERE round_number = 2 AND scenario_title = 'nova2';

INSERT INTO competitions (id, round_number, status, budget, scenario_title, scenario_description, start_year, end_year, days_left)
SELECT
    'dddddddd-0402-0000-0000-000000000402',
    (SELECT COALESCE(MAX(round_number), 0) + 1 FROM competitions),
    'draft',
    100000.00,
    'Nova matriz econômica (2011–2013)',
    'Depois de crescer 7,5% em 2010, o Brasil quer mais. O governo força a queda dos juros até a Selic de 7,25%, a menor da história até então, segura o preço da gasolina e da energia e aposta no crédito dos bancos públicos. Enquanto isso, a empresa de petróleo de um dos homens mais ricos do mundo promete bilhões de barris no pré-sal. Mas a inflação teima em subir, e em 2013 os juros voltam a disparar. Em quem você confiaria?',
    2011,
    2014,
    7
WHERE NOT EXISTS (SELECT 1 FROM competitions WHERE scenario_title = 'Nova matriz econômica (2011–2013)');

INSERT INTO competition_assets (competition_id, asset_id)
SELECT c.id, a.id
FROM competitions c
CROSS JOIN assets a
WHERE c.scenario_title = 'Nova matriz econômica (2011–2013)'
  AND a.id IN (
    'cccccccc-0401-0000-0000-000000000401', 'cccccccc-0402-0000-0000-000000000402',
    'cccccccc-0403-0000-0000-000000000403', 'cccccccc-0404-0000-0000-000000000404',
    'cccccccc-0405-0000-0000-000000000405', 'cccccccc-0406-0000-0000-000000000406',
    'cccccccc-0407-0000-0000-000000000407', 'cccccccc-0408-0000-0000-000000000408');
