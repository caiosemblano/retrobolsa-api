-- ============================================================
-- V8 — Snapshot de 2011 ausente para "Tesouro Selic 2010"
-- ============================================================
-- A competicao seed (V3) cobre 2004-2011, mas o ativo
-- cccccccc-0008-0000-0000-000000000008 ("Tesouro Selic 2010")
-- só tinha snapshots ate 2010. Sem o snapshot de 2011, o motor de
-- simulacao repetia silenciosamente o retorno do ano anterior para
-- o ultimo ano da competicao.
--
-- Usamos a mesma taxa Selic de 2011 ja usada no ativo irmao
-- cccccccc-0006 (11,50%), que representa o mesmo indexador.
-- ============================================================

INSERT INTO asset_snapshots (asset_id, year, rate, annual_return)
SELECT 'cccccccc-0008-0000-0000-000000000008', 2011, 0.1150, 0.1150
WHERE NOT EXISTS (
    SELECT 1 FROM asset_snapshots
    WHERE asset_id = 'cccccccc-0008-0000-0000-000000000008' AND year = 2011
);
