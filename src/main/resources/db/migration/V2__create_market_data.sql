CREATE TABLE asset (
    id SERIAL PRIMARY KEY,
    ticker VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(255),
    sector VARCHAR(255)
);

CREATE TABLE market_data (
    id SERIAL PRIMARY KEY,
    asset_id INTEGER NOT NULL REFERENCES asset(id),
    date DATE NOT NULL,
    close_price DOUBLE PRECISION,
    pe_ratio DOUBLE PRECISION,
    ev_ebitda DOUBLE PRECISION,
    ebitda DOUBLE PRECISION,
    outstanding_shares DOUBLE PRECISION,
    debt_equity DOUBLE PRECISION,
    net_margin DOUBLE PRECISION,
    ebitda_margin DOUBLE PRECISION,
    roa DOUBLE PRECISION
);

CREATE INDEX idx_market_data_asset_date ON market_data(asset_id, date);
