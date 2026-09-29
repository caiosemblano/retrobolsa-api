-- ============================================================
-- V22: modo treino
-- ============================================================
-- O jogador remonta a carteira de uma rodada já revelada e vê o
-- resultado na hora. Cada treino é uma linha aqui; nada disso entra
-- em portfolios, ranking, pontos ou conquistas de jogo.
--
-- O XP do treino (uma vez por rodada treinada) vai para o livro-caixa
-- com a fonte nova PRACTICE.
-- ============================================================

CREATE TABLE practice_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    competition_id  UUID NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
    total_return    DECIMAL(10, 4) NOT NULL,
    final_value     DECIMAL(15, 2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_practice_runs_user ON practice_runs(user_id, competition_id);

ALTER TABLE xp_events DROP CONSTRAINT xp_events_source_check;
ALTER TABLE xp_events ADD CONSTRAINT xp_events_source_check
    CHECK (source IN ('LESSON', 'QUIZ_PERFECT', 'PORTFOLIO', 'BEAT_CDI', 'ACHIEVEMENT', 'PRACTICE'));
