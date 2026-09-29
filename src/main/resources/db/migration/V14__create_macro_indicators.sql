-- ============================================================
-- V14: indicadores macroeconômicos anuais do Brasil (1995–2025)
-- ============================================================
-- Gerado por tools/macro/gerar_macro.py em 2026-09-28; não editar à mão.
-- Usados para mostrar o cenário econômico de cada rodada e, no resultado,
-- comparar a carteira com CDI, poupança, inflação e Ibovespa.
--
-- Todas as taxas em % no ano, salvo indicação:
--   selic       Selic acumulada no ano            (BCB SGS 4390, composta)
--   selic_meta  meta da Selic no fim do ano, % a.a. (BCB SGS 432; existe desde 1999)
--   cdi         CDI acumulado no ano              (BCB SGS 4391, composto)
--   ipca        inflação oficial no ano           (BCB SGS 433, composta)
--   poupanca    rendimento de um depósito novo    (BCB SGS 7828 até mai/2012, 195 depois)
--   ibovespa    variação do índice no ano         (BCB SGS 7 até 2018; Yahoo Finance ^BVSP depois,
--               conferido com o BCB em 2015–2018; de 2019 em diante pode diferir ~0,3% do oficial)
--   dolar       R$ por US$ no fim do ano          (BCB SGS 3696, PTAX venda de dezembro)
--   pib         crescimento real do PIB           (BCB SGS 7326)
-- ============================================================

CREATE TABLE macro_indicators (
    year        INT PRIMARY KEY,
    selic       NUMERIC(8, 2) NOT NULL,
    selic_meta  NUMERIC(6, 2),
    cdi         NUMERIC(8, 2) NOT NULL,
    ipca        NUMERIC(8, 2) NOT NULL,
    poupanca    NUMERIC(8, 2) NOT NULL,
    ibovespa    NUMERIC(8, 2) NOT NULL,
    dolar       NUMERIC(8, 4) NOT NULL,
    pib         NUMERIC(6, 2) NOT NULL
);

INSERT INTO macro_indicators (year, selic, selic_meta, cdi, ipca, poupanca, ibovespa, dolar, pib) VALUES
    (1995, 53.08, NULL, 53.09, 22.41, 39.74, -0.60, 0.9725, 4.22),
    (1996, 27.41, NULL, 27.09, 9.56, 16.34, 63.76, 1.0394, 2.21),
    (1997, 24.77, NULL, 24.60, 5.22, 16.56, 44.83, 1.1164, 3.39),
    (1998, 28.79, NULL, 28.58, 1.66, 14.44, -33.46, 1.2087, 0.34),
    (1999, 25.59, 19.00, 25.12, 8.94, 12.25, 151.93, 1.7890, 0.47),
    (2000, 17.45, 15.75, 17.33, 5.97, 8.39, -10.72, 1.9554, 4.39),
    (2001, 17.32, 19.00, 17.26, 7.67, 8.59, -11.02, 2.3204, 1.39),
    (2002, 19.16, 25.00, 19.09, 12.53, 9.14, -17.01, 3.5333, 3.05),
    (2003, 23.33, 16.50, 23.26, 9.30, 11.10, 97.34, 2.8892, 1.14),
    (2004, 16.24, 17.75, 16.16, 7.60, 8.10, 17.81, 2.6544, 5.76),
    (2005, 19.04, 18.00, 19.00, 5.69, 9.18, 27.71, 2.3407, 3.20),
    (2006, 15.08, 13.25, 15.04, 3.14, 8.33, 32.93, 2.1380, 3.96),
    (2007, 11.85, 11.25, 11.81, 4.46, 7.70, 43.65, 1.7713, 6.07),
    (2008, 12.48, 13.75, 12.38, 5.90, 7.90, -41.22, 2.3370, 5.09),
    (2009, 9.92, 8.75, 9.87, 4.31, 6.92, 82.66, 1.7412, -0.13),
    (2010, 9.78, 10.75, 9.76, 5.91, 6.90, 1.04, 1.6662, 7.53),
    (2011, 11.62, 11.00, 11.59, 6.50, 7.45, -18.11, 1.8758, 3.97),
    (2012, 8.48, 7.25, 8.41, 5.84, 6.05, 7.40, 2.0435, 1.92),
    (2013, 8.21, 10.00, 8.06, 5.91, 5.81, -15.50, 2.3426, 3.00),
    (2014, 10.91, 11.75, 10.82, 6.41, 7.08, -2.91, 2.6562, 0.50),
    (2015, 13.29, 14.25, 13.26, 10.67, 8.07, -13.31, 3.9048, -3.55),
    (2016, 14.03, 13.75, 13.99, 6.29, 8.30, 38.94, 3.2591, -3.28),
    (2017, 9.96, 7.00, 9.93, 2.95, 6.61, 26.86, 3.3080, 1.32),
    (2018, 6.42, 6.50, 6.41, 3.75, 4.62, 15.03, 3.8748, 1.78),
    (2019, 5.95, 4.50, 5.95, 4.31, 4.26, 31.95, 4.0307, 1.22),
    (2020, 2.75, 2.00, 2.75, 4.52, 2.11, 2.88, 5.1967, -3.28),
    (2021, 4.44, 9.25, 4.44, 10.06, 2.99, -12.14, 5.5805, 4.76),
    (2022, 12.38, 13.75, 12.38, 5.78, 7.90, 4.97, 5.2177, 3.02),
    (2023, 13.03, 11.75, 13.03, 4.62, 8.04, 21.95, 4.8413, 3.24),
    (2024, 10.89, 12.25, 10.89, 4.83, 7.03, -10.36, 6.1923, 3.42),
    (2025, 14.33, 15.00, 14.33, 4.26, 8.26, 33.95, 5.5024, 2.29);
