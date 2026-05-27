-- ============================================================
-- V3 — Seed de dados iniciais para testes e demonstração
-- ============================================================
-- Popula uma competição histórica completa (Brasil 2004–2011)
-- com 5 ações + 3 títulos públicos, snapshots anuais e
-- módulos educacionais básicos.
-- ============================================================

-- ------------------------------------------------------------
-- Módulos Educacionais
-- ------------------------------------------------------------
INSERT INTO modules (id, title, description, icon, display_order) VALUES
    ('aaaaaaaa-0001-0000-0000-000000000001', 'Matemática Financeira', 'Aprenda os fundamentos de juros, rentabilidade e valor do dinheiro no tempo.', 'calculator', 1),
    ('aaaaaaaa-0002-0000-0000-000000000002', 'Fundamentos de Investimentos', 'Entenda ações, títulos, dividendos e como analisar empresas.', 'trending-up', 2),
    ('aaaaaaaa-0003-0000-0000-000000000003', 'Macroeconomia', 'Explore como Selic, inflação, câmbio e PIB afetam seus investimentos.', 'globe', 3);

-- Artigos do módulo 1 — Matemática Financeira
INSERT INTO articles (id, module_id, title, duration_min, display_order) VALUES
    ('bbbbbbbb-0001-0000-0000-000000000001', 'aaaaaaaa-0001-0000-0000-000000000001', 'O que é rentabilidade?', 5, 1),
    ('bbbbbbbb-0002-0000-0000-000000000002', 'aaaaaaaa-0001-0000-0000-000000000001', 'Juros simples vs. compostos', 8, 2),
    ('bbbbbbbb-0003-0000-0000-000000000003', 'aaaaaaaa-0001-0000-0000-000000000001', 'Como calcular o retorno anualizado', 6, 3);

-- Artigos do módulo 2 — Fundamentos de Investimentos
INSERT INTO articles (id, module_id, title, duration_min, display_order) VALUES
    ('bbbbbbbb-0004-0000-0000-000000000004', 'aaaaaaaa-0002-0000-0000-000000000002', 'O que é P/L?', 5, 1),
    ('bbbbbbbb-0005-0000-0000-000000000005', 'aaaaaaaa-0002-0000-0000-000000000002', 'O que é ROE?', 5, 2),
    ('bbbbbbbb-0006-0000-0000-000000000006', 'aaaaaaaa-0002-0000-0000-000000000002', 'Dividend Yield na prática', 7, 3);

-- Artigos do módulo 3 — Macroeconomia
INSERT INTO articles (id, module_id, title, duration_min, display_order) VALUES
    ('bbbbbbbb-0007-0000-0000-000000000007', 'aaaaaaaa-0003-0000-0000-000000000003', 'O que é a Taxa Selic?', 5, 1),
    ('bbbbbbbb-0008-0000-0000-000000000008', 'aaaaaaaa-0003-0000-0000-000000000003', 'IPCA: como a inflação corrói seus ganhos', 6, 2);

-- ------------------------------------------------------------
-- Ativos — Ações (nomes reais ocultos até o fim da simulação)
-- ------------------------------------------------------------
INSERT INTO assets (id, anonymous_name, real_name, ticker, type, sector) VALUES
    ('cccccccc-0001-0000-0000-000000000001', 'Empresa A', 'Vale S.A.',          'VALE3', 'stock', 'Mineração'),
    ('cccccccc-0002-0000-0000-000000000002', 'Empresa B', 'Petrobras S.A.',      'PETR4', 'stock', 'Petróleo e Gás'),
    ('cccccccc-0003-0000-0000-000000000003', 'Empresa C', 'Itaú Unibanco S.A.',  'ITUB4', 'stock', 'Financeiro'),
    ('cccccccc-0004-0000-0000-000000000004', 'Empresa D', 'Bradesco S.A.',        'BBDC4', 'stock', 'Financeiro'),
    ('cccccccc-0005-0000-0000-000000000005', 'Empresa E', 'Gerdau S.A.',          'GGBR4', 'stock', 'Siderurgia');

-- Ativos — Títulos Públicos
INSERT INTO assets (id, anonymous_name, real_name, type, bond_type) VALUES
    ('cccccccc-0006-0000-0000-000000000006', 'Título 1', 'Tesouro Prefixado 2011', 'bond', 'Prefixado'),
    ('cccccccc-0007-0000-0000-000000000007', 'Título 2', 'Tesouro IPCA+ 2015',     'bond', 'IPCA+'),
    ('cccccccc-0008-0000-0000-000000000008', 'Título 3', 'Tesouro Selic 2010',     'bond', 'Selic');

-- ------------------------------------------------------------
-- Snapshots anuais — Ações (2004 a 2011)
-- Valores aproximados baseados em dados históricos reais
-- ------------------------------------------------------------

-- Vale S.A. (VALE3)
INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return) VALUES
    ('cccccccc-0001-0000-0000-000000000001', 2004,  8.3, 32.1, 4.2,  0.1780),
    ('cccccccc-0001-0000-0000-000000000001', 2005,  7.1, 35.6, 5.1,  0.3400),
    ('cccccccc-0001-0000-0000-000000000001', 2006,  9.2, 38.0, 4.8,  0.4200),
    ('cccccccc-0001-0000-0000-000000000001', 2007,  6.8, 41.2, 6.3,  0.5800),
    ('cccccccc-0001-0000-0000-000000000001', 2008, 12.4, 28.3, 3.1, -0.5200),
    ('cccccccc-0001-0000-0000-000000000001', 2009,  5.9, 33.7, 5.6,  0.7800),
    ('cccccccc-0001-0000-0000-000000000001', 2010,  7.3, 36.1, 7.2,  0.2900),
    ('cccccccc-0001-0000-0000-000000000001', 2011, 10.1, 30.4, 8.1, -0.1300);

-- Petrobras S.A. (PETR4)
INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return) VALUES
    ('cccccccc-0002-0000-0000-000000000002', 2004, 10.2, 22.4, 3.1,  0.2200),
    ('cccccccc-0002-0000-0000-000000000002', 2005,  8.7, 24.8, 4.0,  0.4100),
    ('cccccccc-0002-0000-0000-000000000002', 2006,  9.1, 26.3, 3.8,  0.3500),
    ('cccccccc-0002-0000-0000-000000000002', 2007,  7.4, 28.1, 4.5,  0.6200),
    ('cccccccc-0002-0000-0000-000000000002', 2008, 14.2, 19.6, 2.8, -0.4700),
    ('cccccccc-0002-0000-0000-000000000002', 2009,  6.1, 22.3, 4.9,  0.6600),
    ('cccccccc-0002-0000-0000-000000000002', 2010,  8.8, 25.0, 5.2,  0.2300),
    ('cccccccc-0002-0000-0000-000000000002', 2011, 11.3, 21.7, 6.0, -0.2100);

-- Itaú Unibanco (ITUB4)
INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return) VALUES
    ('cccccccc-0003-0000-0000-000000000003', 2004, 11.5, 28.3, 2.8,  0.1500),
    ('cccccccc-0003-0000-0000-000000000003', 2005,  9.8, 30.1, 3.2,  0.2800),
    ('cccccccc-0003-0000-0000-000000000003', 2006, 10.2, 31.8, 3.5,  0.3100),
    ('cccccccc-0003-0000-0000-000000000003', 2007,  8.4, 33.5, 4.1,  0.5400),
    ('cccccccc-0003-0000-0000-000000000003', 2008, 12.8, 25.4, 2.6, -0.3300),
    ('cccccccc-0003-0000-0000-000000000003', 2009,  7.6, 28.9, 4.3,  0.6900),
    ('cccccccc-0003-0000-0000-000000000003', 2010,  9.3, 31.2, 4.8,  0.2100),
    ('cccccccc-0003-0000-0000-000000000003', 2011, 10.7, 28.6, 5.5, -0.0800);

-- ------------------------------------------------------------
-- Snapshots anuais — Títulos Públicos
-- Retorno simplificado para fins de simulação
-- ------------------------------------------------------------

-- Tesouro Prefixado 2011
INSERT INTO asset_snapshots (asset_id, year, rate, annual_return) VALUES
    ('cccccccc-0006-0000-0000-000000000006', 2004, 0.1750, 0.1750),
    ('cccccccc-0006-0000-0000-000000000006', 2005, 0.1900, 0.1900),
    ('cccccccc-0006-0000-0000-000000000006', 2006, 0.1350, 0.1350),
    ('cccccccc-0006-0000-0000-000000000006', 2007, 0.1175, 0.1175),
    ('cccccccc-0006-0000-0000-000000000006', 2008, 0.1375, 0.1375),
    ('cccccccc-0006-0000-0000-000000000006', 2009, 0.0875, 0.0875),
    ('cccccccc-0006-0000-0000-000000000006', 2010, 0.1075, 0.1075),
    ('cccccccc-0006-0000-0000-000000000006', 2011, 0.1150, 0.1150);

-- Tesouro IPCA+ 2015
INSERT INTO asset_snapshots (asset_id, year, rate, annual_return) VALUES
    ('cccccccc-0007-0000-0000-000000000007', 2004, 0.0760, 0.1406),
    ('cccccccc-0007-0000-0000-000000000007', 2005, 0.0551, 0.1009),
    ('cccccccc-0007-0000-0000-000000000007', 2006, 0.0314, 0.0874),
    ('cccccccc-0007-0000-0000-000000000007', 2007, 0.0445, 0.1084),
    ('cccccccc-0007-0000-0000-000000000007', 2008, 0.0590, 0.1157),
    ('cccccccc-0007-0000-0000-000000000007', 2009, 0.0431, 0.0860),
    ('cccccccc-0007-0000-0000-000000000007', 2010, 0.0591, 0.1113),
    ('cccccccc-0007-0000-0000-000000000007', 2011, 0.0653, 0.1068);

-- Tesouro Selic 2010
INSERT INTO asset_snapshots (asset_id, year, rate, annual_return) VALUES
    ('cccccccc-0008-0000-0000-000000000008', 2004, 0.1625, 0.1625),
    ('cccccccc-0008-0000-0000-000000000008', 2005, 0.1900, 0.1900),
    ('cccccccc-0008-0000-0000-000000000008', 2006, 0.1325, 0.1325),
    ('cccccccc-0008-0000-0000-000000000008', 2007, 0.1175, 0.1175),
    ('cccccccc-0008-0000-0000-000000000008', 2008, 0.1375, 0.1375),
    ('cccccccc-0008-0000-0000-000000000008', 2009, 0.0875, 0.0875),
    ('cccccccc-0008-0000-0000-000000000008', 2010, 0.1075, 0.1075);

-- ------------------------------------------------------------
-- Competição — Rodada 1: Brasil 2004–2011
-- ------------------------------------------------------------
INSERT INTO competitions (id, round_number, status, budget, scenario_title, scenario_description, start_year, end_year, days_left)
VALUES (
    'dddddddd-0001-0000-0000-000000000001',
    1,
    'open',
    100000.00,
    'O Grande Boom das Commodities (2004–2011)',
    'O Brasil vivia um ciclo virtuoso impulsionado pela valorização das commodities no mercado internacional. A China crescia aceleradamente, demandando minério de ferro, petróleo e grãos brasileiros. A taxa Selic ainda estava em dois dígitos, a inflação controlada e o país acabara de obter o grau de investimento. Quais ativos você escolheria neste cenário?',
    2004,
    2011,
    7
);

-- Vincula todos os ativos à rodada 1
INSERT INTO competition_assets (competition_id, asset_id) VALUES
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0001-0000-0000-000000000001'),
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0002-0000-0000-000000000002'),
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0003-0000-0000-000000000003'),
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0004-0000-0000-000000000004'),
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0005-0000-0000-000000000005'),
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0006-0000-0000-000000000006'),
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0007-0000-0000-000000000007'),
    ('dddddddd-0001-0000-0000-000000000001', 'cccccccc-0008-0000-0000-000000000008');
