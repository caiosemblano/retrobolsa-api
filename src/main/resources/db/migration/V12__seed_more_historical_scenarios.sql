-- ============================================================
-- V12 — Novos contextos históricos para teste
-- ============================================================
-- Três rodadas novas, cada uma com 5 ações + 3 títulos próprios:
--   • Recessão e Lava Jato ............ retornos de 2014 a 2016
--   • Juros mínimos e a pandemia ...... retornos de 2017 a 2020
--   • Pandemia e a volta da inflação .. retornos de 2020 a 2023
--
-- Como o motor usa os dados (SimulationEngine.calculateSnapshots):
--   • aplica o annual_return dos anos start_year .. end_year - 1,
--     por isso end_year é o ano seguinte ao último ano de mercado;
--   • o jogador vê só os indicadores do snapshot de start_year
--     (CompetitionService.buildDto), então os anos seguintes
--     precisam apenas do retorno.
--
-- Valores aproximados baseados em dados históricos reais (retorno
-- total anual com proventos; títulos simplificados, como no V3).
--
-- As rodadas entram como 'draft' e pegam o próximo round_number
-- livre, porque em produção já existem rodadas criadas pelo admin.
-- Para jogar, inicie a rodada pela tela de Admin.
-- ============================================================

-- ------------------------------------------------------------
-- Rodada: Recessão e Lava Jato (2014–2016)
-- ------------------------------------------------------------
INSERT INTO assets (id, anonymous_name, real_name, ticker, type, sector) VALUES
    ('cccccccc-0101-0000-0000-000000000101', 'Empresa A', 'Petrobras S.A.',      'PETR4', 'stock', 'Petróleo e Gás'),
    ('cccccccc-0102-0000-0000-000000000102', 'Empresa B', 'Vale S.A.',           'VALE3', 'stock', 'Mineração'),
    ('cccccccc-0103-0000-0000-000000000103', 'Empresa C', 'Itaú Unibanco S.A.',  'ITUB4', 'stock', 'Financeiro'),
    ('cccccccc-0104-0000-0000-000000000104', 'Empresa D', 'Ambev S.A.',          'ABEV3', 'stock', 'Bebidas'),
    ('cccccccc-0105-0000-0000-000000000105', 'Empresa E', 'Magazine Luiza S.A.', 'MGLU3', 'stock', 'Varejo');

INSERT INTO assets (id, anonymous_name, real_name, type, bond_type) VALUES
    ('cccccccc-0106-0000-0000-000000000106', 'Título 1', 'Tesouro Prefixado 2017', 'bond', 'Prefixado'),
    ('cccccccc-0107-0000-0000-000000000107', 'Título 2', 'Tesouro IPCA+ 2024',     'bond', 'IPCA+'),
    ('cccccccc-0108-0000-0000-000000000108', 'Título 3', 'Tesouro Selic 2021',     'bond', 'Selic');

INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return, lvp, lucro_positivo, cagr_lucro, cagr_receita, margem_ebitda) VALUES
    ('cccccccc-0101-0000-0000-000000000101', 2014,  7.6,  6.9, 3.2, -0.3700, 0.62, true,  -0.1500, 0.0800, 21.4),
    ('cccccccc-0102-0000-0000-000000000102', 2014, 16.8,  3.3, 6.1, -0.3300, 1.05, true,  -0.2800, 0.0200, 38.7),
    ('cccccccc-0103-0000-0000-000000000103', 2014,  9.4, 20.9, 3.4,  0.1000, 1.86, true,   0.1300, 0.1100, NULL),
    ('cccccccc-0104-0000-0000-000000000104', 2014, 30.2, 36.4, 3.3,  0.0500, 9.80, true,   0.1400, 0.1000, 50.3),
    ('cccccccc-0105-0000-0000-000000000105', 2014, 19.5, 11.2, 1.5, -0.3500, 2.10, true,   0.0500, 0.2000,  7.8);

INSERT INTO asset_snapshots (asset_id, year, annual_return) VALUES
    ('cccccccc-0101-0000-0000-000000000101', 2015, -0.3200),
    ('cccccccc-0101-0000-0000-000000000101', 2016,  1.2100),
    ('cccccccc-0102-0000-0000-000000000102', 2015, -0.4500),
    ('cccccccc-0102-0000-0000-000000000102', 2016,  0.9500),
    ('cccccccc-0103-0000-0000-000000000103', 2015, -0.1800),
    ('cccccccc-0103-0000-0000-000000000103', 2016,  0.3800),
    ('cccccccc-0104-0000-0000-000000000104', 2015,  0.0800),
    ('cccccccc-0104-0000-0000-000000000104', 2016, -0.0400),
    ('cccccccc-0105-0000-0000-000000000105', 2015, -0.7200),
    ('cccccccc-0105-0000-0000-000000000105', 2016,  4.5000);

INSERT INTO asset_snapshots (asset_id, year, rate, annual_return) VALUES
    ('cccccccc-0106-0000-0000-000000000106', 2014, 0.1250, 0.1050),
    ('cccccccc-0106-0000-0000-000000000106', 2015, 0.1300, 0.1150),
    ('cccccccc-0106-0000-0000-000000000106', 2016, 0.1550, 0.1700),
    ('cccccccc-0107-0000-0000-000000000107', 2014, 0.0600, 0.1200),
    ('cccccccc-0107-0000-0000-000000000107', 2015, 0.0650, 0.0500),
    ('cccccccc-0107-0000-0000-000000000107', 2016, 0.0700, 0.2500),
    ('cccccccc-0108-0000-0000-000000000108', 2014, 0.1050, 0.1081),
    ('cccccccc-0108-0000-0000-000000000108', 2015, 0.1175, 0.1324),
    ('cccccccc-0108-0000-0000-000000000108', 2016, 0.1425, 0.1400);

INSERT INTO competitions (id, round_number, status, budget, scenario_title, scenario_description, start_year, end_year, days_left)
SELECT
    'dddddddd-0102-0000-0000-000000000102',
    COALESCE(MAX(round_number), 0) + 1,
    'draft',
    100000.00,
    'Recessão e Lava Jato (2014–2016)',
    'O fim do boom das commodities chegou. O minério e o petróleo despencam no mercado internacional, a Operação Lava Jato expõe a maior estatal do país e a economia entra na pior recessão em décadas. A inflação passa de 10%, a Selic sobe para 14,25% e o país perde o grau de investimento em meio a uma crise política que termina em impeachment. Onde você colocaria seu dinheiro?',
    2014,
    2017,
    7
FROM competitions;

INSERT INTO competition_assets (competition_id, asset_id) VALUES
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0101-0000-0000-000000000101'),
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0102-0000-0000-000000000102'),
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0103-0000-0000-000000000103'),
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0104-0000-0000-000000000104'),
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0105-0000-0000-000000000105'),
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0106-0000-0000-000000000106'),
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0107-0000-0000-000000000107'),
    ('dddddddd-0102-0000-0000-000000000102', 'cccccccc-0108-0000-0000-000000000108');

-- ------------------------------------------------------------
-- Rodada: Juros mínimos e a chegada da pandemia (2017–2020)
-- ------------------------------------------------------------
INSERT INTO assets (id, anonymous_name, real_name, ticker, type, sector) VALUES
    ('cccccccc-0201-0000-0000-000000000201', 'Empresa A', 'WEG S.A.',            'WEGE3', 'stock', 'Bens de Capital'),
    ('cccccccc-0202-0000-0000-000000000202', 'Empresa B', 'Itaú Unibanco S.A.',  'ITUB4', 'stock', 'Financeiro'),
    ('cccccccc-0203-0000-0000-000000000203', 'Empresa C', 'Vale S.A.',           'VALE3', 'stock', 'Mineração'),
    ('cccccccc-0204-0000-0000-000000000204', 'Empresa D', 'CVC Brasil S.A.',     'CVCB3', 'stock', 'Turismo'),
    ('cccccccc-0205-0000-0000-000000000205', 'Empresa E', 'Lojas Renner S.A.',   'LREN3', 'stock', 'Varejo');

INSERT INTO assets (id, anonymous_name, real_name, type, bond_type) VALUES
    ('cccccccc-0206-0000-0000-000000000206', 'Título 1', 'Tesouro Prefixado 2023', 'bond', 'Prefixado'),
    ('cccccccc-0207-0000-0000-000000000207', 'Título 2', 'Tesouro IPCA+ 2035',     'bond', 'IPCA+'),
    ('cccccccc-0208-0000-0000-000000000208', 'Título 3', 'Tesouro Selic 2023',     'bond', 'Selic');

INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return, lvp, lucro_positivo, cagr_lucro, cagr_receita, margem_ebitda) VALUES
    ('cccccccc-0201-0000-0000-000000000201', 2017, 29.5, 13.8, 2.1,  0.3000, 4.10, true,  -0.0300, 0.0400, 16.2),
    ('cccccccc-0202-0000-0000-000000000202', 2017, 10.6, 20.0, 5.0,  0.3000, 2.05, true,   0.0200, 0.0500, NULL),
    ('cccccccc-0203-0000-0000-000000000203', 2017,  8.2, 10.4, 1.2,  0.4700, 1.45, true,   0.0000, 0.0500, 41.3),
    ('cccccccc-0204-0000-0000-000000000204', 2017, 34.8, 17.5, 1.9,  0.6500, 5.90, true,   0.0900, 0.0800, 44.0),
    ('cccccccc-0205-0000-0000-000000000205', 2017, 27.3, 17.6, 2.3,  0.5500, 4.70, true,   0.0600, 0.0900, 18.4);

INSERT INTO asset_snapshots (asset_id, year, annual_return) VALUES
    ('cccccccc-0201-0000-0000-000000000201', 2018, -0.0200),
    ('cccccccc-0201-0000-0000-000000000201', 2019,  0.9200),
    ('cccccccc-0201-0000-0000-000000000201', 2020,  1.1500),
    ('cccccccc-0202-0000-0000-000000000202', 2018,  0.2600),
    ('cccccccc-0202-0000-0000-000000000202', 2019,  0.0500),
    ('cccccccc-0202-0000-0000-000000000202', 2020, -0.1200),
    ('cccccccc-0203-0000-0000-000000000203', 2018,  0.3000),
    ('cccccccc-0203-0000-0000-000000000203', 2019,  0.0200),
    ('cccccccc-0203-0000-0000-000000000203', 2020,  0.7000),
    ('cccccccc-0204-0000-0000-000000000204', 2018,  0.1200),
    ('cccccccc-0204-0000-0000-000000000204', 2019, -0.3200),
    ('cccccccc-0204-0000-0000-000000000204', 2020, -0.6200),
    ('cccccccc-0205-0000-0000-000000000205', 2018,  0.2000),
    ('cccccccc-0205-0000-0000-000000000205', 2019,  0.3000),
    ('cccccccc-0205-0000-0000-000000000205', 2020, -0.2000);

INSERT INTO asset_snapshots (asset_id, year, rate, annual_return) VALUES
    ('cccccccc-0206-0000-0000-000000000206', 2017, 0.1050, 0.1500),
    ('cccccccc-0206-0000-0000-000000000206', 2018, 0.0950, 0.0900),
    ('cccccccc-0206-0000-0000-000000000206', 2019, 0.0900, 0.1200),
    ('cccccccc-0206-0000-0000-000000000206', 2020, 0.0650, 0.0400),
    ('cccccccc-0207-0000-0000-000000000207', 2017, 0.0550, 0.1500),
    ('cccccccc-0207-0000-0000-000000000207', 2018, 0.0500, 0.1400),
    ('cccccccc-0207-0000-0000-000000000207', 2019, 0.0450, 0.3000),
    ('cccccccc-0207-0000-0000-000000000207', 2020, 0.0300, 0.0500),
    ('cccccccc-0208-0000-0000-000000000208', 2017, 0.1375, 0.0993),
    ('cccccccc-0208-0000-0000-000000000208', 2018, 0.0700, 0.0642),
    ('cccccccc-0208-0000-0000-000000000208', 2019, 0.0650, 0.0596),
    ('cccccccc-0208-0000-0000-000000000208', 2020, 0.0450, 0.0276);

INSERT INTO competitions (id, round_number, status, budget, scenario_title, scenario_description, start_year, end_year, days_left)
SELECT
    'dddddddd-0202-0000-0000-000000000202',
    COALESCE(MAX(round_number), 0) + 1,
    'draft',
    100000.00,
    'Juros mínimos e a chegada da pandemia (2017–2020)',
    'A recessão ficou para trás. A inflação despenca e o Banco Central corta a Selic de 13,75% até a mínima histórica de 2%. A renda fixa rende cada vez menos e milhões de brasileiros descobrem a bolsa pela primeira vez. Mas o caminho tem sustos: o rompimento de uma barragem em Brumadinho em 2019 e, no começo de 2020, um vírus que fecha fronteiras, aeroportos e lojas no mundo todo. Como montar uma carteira para esses quatro anos?',
    2017,
    2021,
    7
FROM competitions;

INSERT INTO competition_assets (competition_id, asset_id) VALUES
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0201-0000-0000-000000000201'),
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0202-0000-0000-000000000202'),
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0203-0000-0000-000000000203'),
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0204-0000-0000-000000000204'),
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0205-0000-0000-000000000205'),
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0206-0000-0000-000000000206'),
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0207-0000-0000-000000000207'),
    ('dddddddd-0202-0000-0000-000000000202', 'cccccccc-0208-0000-0000-000000000208');

-- ------------------------------------------------------------
-- Rodada: Pandemia e a volta da inflação (2020–2023)
-- ------------------------------------------------------------
INSERT INTO assets (id, anonymous_name, real_name, ticker, type, sector) VALUES
    ('cccccccc-0301-0000-0000-000000000301', 'Empresa A', 'Petrobras S.A.',      'PETR4', 'stock', 'Petróleo e Gás'),
    ('cccccccc-0302-0000-0000-000000000302', 'Empresa B', 'Magazine Luiza S.A.', 'MGLU3', 'stock', 'Varejo'),
    ('cccccccc-0303-0000-0000-000000000303', 'Empresa C', 'Azul S.A.',           'AZUL4', 'stock', 'Aviação'),
    ('cccccccc-0304-0000-0000-000000000304', 'Empresa D', 'Banco do Brasil S.A.', 'BBAS3', 'stock', 'Financeiro'),
    ('cccccccc-0305-0000-0000-000000000305', 'Empresa E', 'WEG S.A.',            'WEGE3', 'stock', 'Bens de Capital');

INSERT INTO assets (id, anonymous_name, real_name, type, bond_type) VALUES
    ('cccccccc-0306-0000-0000-000000000306', 'Título 1', 'Tesouro Prefixado 2025', 'bond', 'Prefixado'),
    ('cccccccc-0307-0000-0000-000000000307', 'Título 2', 'Tesouro IPCA+ 2045',     'bond', 'IPCA+'),
    ('cccccccc-0308-0000-0000-000000000308', 'Título 3', 'Tesouro Selic 2025',     'bond', 'Selic');

INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return, lvp, lucro_positivo, cagr_lucro, cagr_receita, margem_ebitda) VALUES
    ('cccccccc-0301-0000-0000-000000000301', 2020,  9.8, 13.3, 3.5, -0.0500, 1.30, true,   0.4000, 0.0600, 38.9),
    ('cccccccc-0302-0000-0000-000000000302', 2020, 97.0, 14.0, 0.3,  1.1000, 13.50, true,  0.1800, 0.2700,  7.1),
    ('cccccccc-0303-0000-0000-000000000303', 2020, 34.0, -45.0, 0.0, -0.4200, -4.80, false, -0.3000, 0.1500, 32.0),
    ('cccccccc-0304-0000-0000-000000000304', 2020,  8.2, 17.4, 4.3, -0.3000, 1.30, true,   0.1600, 0.0500, NULL),
    ('cccccccc-0305-0000-0000-000000000305', 2020, 36.5, 20.6, 1.6,  1.1500, 7.60, true,   0.1300, 0.1200, 16.9);

INSERT INTO asset_snapshots (asset_id, year, annual_return) VALUES
    ('cccccccc-0301-0000-0000-000000000301', 2021,  0.2500),
    ('cccccccc-0301-0000-0000-000000000301', 2022,  0.3000),
    ('cccccccc-0301-0000-0000-000000000301', 2023,  0.9500),
    ('cccccccc-0302-0000-0000-000000000302', 2021, -0.7000),
    ('cccccccc-0302-0000-0000-000000000302', 2022, -0.6000),
    ('cccccccc-0302-0000-0000-000000000302', 2023, -0.2200),
    ('cccccccc-0303-0000-0000-000000000303', 2021, -0.3800),
    ('cccccccc-0303-0000-0000-000000000303', 2022, -0.5500),
    ('cccccccc-0303-0000-0000-000000000303', 2023,  0.3000),
    ('cccccccc-0304-0000-0000-000000000304', 2021,  0.0200),
    ('cccccccc-0304-0000-0000-000000000304', 2022,  0.4500),
    ('cccccccc-0304-0000-0000-000000000304', 2023,  0.6500),
    ('cccccccc-0305-0000-0000-000000000305', 2021, -0.1500),
    ('cccccccc-0305-0000-0000-000000000305', 2022,  0.2500),
    ('cccccccc-0305-0000-0000-000000000305', 2023, -0.0800);

INSERT INTO asset_snapshots (asset_id, year, rate, annual_return) VALUES
    ('cccccccc-0306-0000-0000-000000000306', 2020, 0.0550, 0.0500),
    ('cccccccc-0306-0000-0000-000000000306', 2021, 0.0700, -0.0400),
    ('cccccccc-0306-0000-0000-000000000306', 2022, 0.1150, 0.0900),
    ('cccccccc-0306-0000-0000-000000000306', 2023, 0.1300, 0.1900),
    ('cccccccc-0307-0000-0000-000000000307', 2020, 0.0330, 0.0600),
    ('cccccccc-0307-0000-0000-000000000307', 2021, 0.0400, -0.1500),
    ('cccccccc-0307-0000-0000-000000000307', 2022, 0.0550, -0.0200),
    ('cccccccc-0307-0000-0000-000000000307', 2023, 0.0600, 0.1800),
    ('cccccccc-0308-0000-0000-000000000308', 2020, 0.0450, 0.0276),
    ('cccccccc-0308-0000-0000-000000000308', 2021, 0.0200, 0.0442),
    ('cccccccc-0308-0000-0000-000000000308', 2022, 0.0925, 0.1239),
    ('cccccccc-0308-0000-0000-000000000308', 2023, 0.1375, 0.1304);

INSERT INTO competitions (id, round_number, status, budget, scenario_title, scenario_description, start_year, end_year, days_left)
SELECT
    'dddddddd-0302-0000-0000-000000000302',
    COALESCE(MAX(round_number), 0) + 1,
    'draft',
    100000.00,
    'Pandemia e a volta da inflação (2020–2023)',
    'Começo de 2020: a Selic está na mínima histórica e a bolsa bate recordes. Semanas depois, a pandemia derruba os mercados, esvazia os aviões e fecha o comércio de rua, enquanto as vendas online explodem. Na saída da crise, os preços de alimentos e combustíveis disparam, a inflação passa de 10% e o Banco Central sobe a Selic de 2% para 13,75% no ciclo de alta mais rápido em décadas. Quem sobrevive a essa montanha-russa?',
    2020,
    2024,
    7
FROM competitions;

INSERT INTO competition_assets (competition_id, asset_id) VALUES
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0301-0000-0000-000000000301'),
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0302-0000-0000-000000000302'),
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0303-0000-0000-000000000303'),
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0304-0000-0000-000000000304'),
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0305-0000-0000-000000000305'),
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0306-0000-0000-000000000306'),
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0307-0000-0000-000000000307'),
    ('dddddddd-0302-0000-0000-000000000302', 'cccccccc-0308-0000-0000-000000000308');
