ALTER TABLE asset_snapshots
    ADD COLUMN lvp             DECIMAL(10, 2),
    ADD COLUMN lucro_positivo  BOOLEAN,
    ADD COLUMN cagr_lucro      DECIMAL(10, 4),
    ADD COLUMN cagr_receita    DECIMAL(10, 4),
    ADD COLUMN margem_ebitda   DECIMAL(10, 2);

UPDATE asset_snapshots SET lvp = 2.66, lucro_positivo = true, cagr_lucro = 0.2800, cagr_receita = 0.2200, margem_ebitda = 42.1 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2004;
UPDATE asset_snapshots SET lvp = 2.52, lucro_positivo = true, cagr_lucro = 0.3200, cagr_receita = 0.2500, margem_ebitda = 44.3 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2005;
UPDATE asset_snapshots SET lvp = 3.50, lucro_positivo = true, cagr_lucro = 0.3500, cagr_receita = 0.2800, margem_ebitda = 45.7 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2006;
UPDATE asset_snapshots SET lvp = 2.80, lucro_positivo = true, cagr_lucro = 0.3800, cagr_receita = 0.3100, margem_ebitda = 48.2 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2007;
UPDATE asset_snapshots SET lvp = 3.52, lucro_positivo = true, cagr_lucro = 0.2100, cagr_receita = 0.1800, margem_ebitda = 38.5 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2008;
UPDATE asset_snapshots SET lvp = 1.99, lucro_positivo = true, cagr_lucro = 0.2500, cagr_receita = 0.2000, margem_ebitda = 41.8 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2009;
UPDATE asset_snapshots SET lvp = 2.63, lucro_positivo = true, cagr_lucro = 0.2900, cagr_receita = 0.2400, margem_ebitda = 46.1 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2010;
UPDATE asset_snapshots SET lvp = 3.07, lucro_positivo = true, cagr_lucro = 0.2200, cagr_receita = 0.1900, margem_ebitda = 40.3 WHERE asset_id = 'cccccccc-0001-0000-0000-000000000001' AND year = 2011;

UPDATE asset_snapshots SET lvp = 2.28, lucro_positivo = true, cagr_lucro = 0.1500, cagr_receita = 0.1800, margem_ebitda = 32.4 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2004;
UPDATE asset_snapshots SET lvp = 2.15, lucro_positivo = true, cagr_lucro = 0.1800, cagr_receita = 0.2100, margem_ebitda = 34.8 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2005;
UPDATE asset_snapshots SET lvp = 2.39, lucro_positivo = true, cagr_lucro = 0.2000, cagr_receita = 0.2300, margem_ebitda = 35.6 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2006;
UPDATE asset_snapshots SET lvp = 2.08, lucro_positivo = true, cagr_lucro = 0.2200, cagr_receita = 0.2600, margem_ebitda = 37.2 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2007;
UPDATE asset_snapshots SET lvp = 2.78, lucro_positivo = true, cagr_lucro = 0.1200, cagr_receita = 0.1500, margem_ebitda = 28.9 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2008;
UPDATE asset_snapshots SET lvp = 1.36, lucro_positivo = true, cagr_lucro = 0.1600, cagr_receita = 0.1900, margem_ebitda = 31.5 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2009;
UPDATE asset_snapshots SET lvp = 2.20, lucro_positivo = true, cagr_lucro = 0.1900, cagr_receita = 0.2200, margem_ebitda = 33.7 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2010;
UPDATE asset_snapshots SET lvp = 2.45, lucro_positivo = true, cagr_lucro = 0.1400, cagr_receita = 0.1700, margem_ebitda = 30.1 WHERE asset_id = 'cccccccc-0002-0000-0000-000000000002' AND year = 2011;

UPDATE asset_snapshots SET lvp = 3.26, lucro_positivo = true, cagr_lucro = 0.1200, cagr_receita = 0.1400, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2004;
UPDATE asset_snapshots SET lvp = 2.95, lucro_positivo = true, cagr_lucro = 0.1500, cagr_receita = 0.1700, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2005;
UPDATE asset_snapshots SET lvp = 3.24, lucro_positivo = true, cagr_lucro = 0.1700, cagr_receita = 0.1900, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2006;
UPDATE asset_snapshots SET lvp = 2.81, lucro_positivo = true, cagr_lucro = 0.1900, cagr_receita = 0.2100, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2007;
UPDATE asset_snapshots SET lvp = 3.25, lucro_positivo = true, cagr_lucro = 0.1000, cagr_receita = 0.1200, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2008;
UPDATE asset_snapshots SET lvp = 2.19, lucro_positivo = true, cagr_lucro = 0.1400, cagr_receita = 0.1600, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2009;
UPDATE asset_snapshots SET lvp = 2.90, lucro_positivo = true, cagr_lucro = 0.1600, cagr_receita = 0.1800, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2010;
UPDATE asset_snapshots SET lvp = 3.06, lucro_positivo = true, cagr_lucro = 0.1200, cagr_receita = 0.1400, margem_ebitda = NULL WHERE asset_id = 'cccccccc-0003-0000-0000-000000000003' AND year = 2011;

INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return, lvp, lucro_positivo, cagr_lucro, cagr_receita, margem_ebitda) VALUES
    ('cccccccc-0004-0000-0000-000000000004', 2004, 12.1, 26.5, 2.5,  0.1300, 3.21, true, 0.1100, 0.1300, NULL),
    ('cccccccc-0004-0000-0000-000000000004', 2005, 10.4, 28.3, 3.0,  0.2600, 2.95, true, 0.1300, 0.1500, NULL),
    ('cccccccc-0004-0000-0000-000000000004', 2006, 11.0, 29.8, 3.3,  0.2900, 3.28, true, 0.1500, 0.1700, NULL),
    ('cccccccc-0004-0000-0000-000000000004', 2007,  8.9, 31.0, 3.8,  0.4800, 2.76, true, 0.1700, 0.1900, NULL),
    ('cccccccc-0004-0000-0000-000000000004', 2008, 13.5, 23.1, 2.4, -0.3800, 3.12, true, 0.0900, 0.1100, NULL),
    ('cccccccc-0004-0000-0000-000000000004', 2009,  8.1, 26.7, 4.0,  0.6200, 2.17, true, 0.1200, 0.1400, NULL),
    ('cccccccc-0004-0000-0000-000000000004', 2010,  9.8, 29.5, 4.5,  0.1900, 2.88, true, 0.1400, 0.1600, NULL),
    ('cccccccc-0004-0000-0000-000000000004', 2011, 11.2, 27.0, 5.0, -0.1000, 3.02, true, 0.1100, 0.1300, NULL);

INSERT INTO asset_snapshots (asset_id, year, pl, roe, dividend_yield, annual_return, lvp, lucro_positivo, cagr_lucro, cagr_receita, margem_ebitda) VALUES
    ('cccccccc-0005-0000-0000-000000000005', 2004,  7.5, 22.0, 3.5,  0.2500, 1.65, true, 0.1800, 0.2000, 22.3),
    ('cccccccc-0005-0000-0000-000000000005', 2005,  6.8, 24.5, 4.0,  0.3100, 1.67, true, 0.2100, 0.2300, 24.1),
    ('cccccccc-0005-0000-0000-000000000005', 2006,  8.1, 26.8, 3.8,  0.2800, 2.17, true, 0.2400, 0.2600, 25.8),
    ('cccccccc-0005-0000-0000-000000000005', 2007,  5.9, 29.3, 5.0,  0.6500, 1.73, true, 0.2700, 0.2900, 27.5),
    ('cccccccc-0005-0000-0000-000000000005', 2008, 11.8, 18.5, 2.6, -0.5500, 2.18, true, 0.1400, 0.1600, 18.4),
    ('cccccccc-0005-0000-0000-000000000005', 2009,  5.2, 21.6, 4.8,  0.8200, 1.12, true, 0.1700, 0.1900, 21.2),
    ('cccccccc-0005-0000-0000-000000000005', 2010,  7.0, 24.1, 5.5,  0.2600, 1.68, true, 0.2000, 0.2200, 23.7),
    ('cccccccc-0005-0000-0000-000000000005', 2011,  9.4, 20.8, 6.2, -0.1800, 1.96, true, 0.1500, 0.1700, 19.8);

UPDATE asset_snapshots SET lvp = NULL, lucro_positivo = NULL, cagr_lucro = NULL, cagr_receita = NULL, margem_ebitda = NULL
WHERE asset_id IN ('cccccccc-0006-0000-0000-000000000006', 'cccccccc-0007-0000-0000-000000000007', 'cccccccc-0008-0000-0000-000000000008');
