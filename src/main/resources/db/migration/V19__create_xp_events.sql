-- ============================================================
-- V19: livro-caixa de XP
-- ============================================================
-- Cada ganho de XP é uma linha: de onde veio (source), a que se refere
-- (ref_id: a aula, a rodada, o código da conquista) e quanto valeu. O UNIQUE
-- (user_id, source, ref_id) garante que a mesma coisa nunca dá XP duas vezes,
-- e o total, o nível e a sequência de semanas saem daqui, sem estado extra.
--
-- seen = FALSE marca o que o jogador ainda não viu comemorado no app.
-- ============================================================

CREATE TABLE xp_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source      VARCHAR(30) NOT NULL
                CHECK (source IN ('LESSON', 'QUIZ_PERFECT', 'PORTFOLIO', 'BEAT_CDI', 'ACHIEVEMENT')),
    ref_id      VARCHAR(80) NOT NULL,
    amount      INT NOT NULL CHECK (amount > 0),
    seen        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, source, ref_id)
);

CREATE INDEX idx_xp_events_user_unseen ON xp_events(user_id) WHERE NOT seen;
