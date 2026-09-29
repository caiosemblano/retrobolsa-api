-- ============================================================
-- V27: primeiro acesso, consentimento e troca obrigatória de senha
-- ============================================================
-- onboarded: o jogador já viu o passo a passo do primeiro acesso. Quem já
-- usava o app antes desta versão não está no primeiro acesso: fica TRUE.
--
-- consented_at: quando a pessoa marcou, no cadastro, "Tenho 18 anos ou
-- mais, ou tenho autorização do meu responsável" (LGPD).
--
-- must_change_password: não há e-mail; quem esquece a senha pede ao admin,
-- que gera uma senha temporária. Com ela, o app obriga a trocar a senha
-- antes de seguir.
-- ============================================================

ALTER TABLE users ADD COLUMN onboarded BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET onboarded = TRUE;

ALTER TABLE users ADD COLUMN consented_at TIMESTAMP;

ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
