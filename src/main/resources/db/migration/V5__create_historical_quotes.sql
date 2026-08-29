CREATE TABLE historical_quotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id),
    date DATE NOT NULL,
    close_price DOUBLE PRECISION,
    pe_ratio DOUBLE PRECISION,
    ev_ebitda DOUBLE PRECISION,
    ebitda DOUBLE PRECISION,
    outstanding_shares DOUBLE PRECISION,
    debt_equity DOUBLE PRECISION,
    net_margin DOUBLE PRECISION,
    ebitda_margin DOUBLE PRECISION,
    roa DOUBLE PRECISION,
    UNIQUE(asset_id, date)
);

CREATE INDEX idx_historical_quotes_date ON historical_quotes(asset_id, date);
