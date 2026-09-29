-- ============================================================
-- V24: missões semanais
-- ============================================================
-- Toda semana (ISO, de segunda a domingo) valem 3 missões sorteadas
-- destes modelos, as mesmas para todos: a semente do sorteio é a semana.
-- O sorteio fica no MissionService; o MissionCatalogIntegrationTest
-- garante que os códigos daqui e os do Java batem.
--
-- user_mission_refs guarda o que já contou para cada missão na semana
-- (a aula, a rodada, o dia): a mesma coisa não conta duas vezes, então
-- não dá para cumprir "conclua 2 aulas" refazendo a mesma aula.
-- A recompensa é XP com a fonte nova MISSION (ref: "<código>:<semana>").
-- ============================================================

CREATE TABLE mission_templates (
    code          VARCHAR(30) PRIMARY KEY,
    title         VARCHAR(80) NOT NULL,
    description   VARCHAR(200) NOT NULL,
    event         VARCHAR(20) NOT NULL CHECK (event IN ('LESSON', 'QUIZ_PERFECT', 'PORTFOLIO_3', 'PRACTICE', 'VISIT')),
    target        INT NOT NULL CHECK (target > 0),
    xp            INT NOT NULL CHECK (xp > 0),
    display_order INT NOT NULL
);

INSERT INTO mission_templates (code, title, description, event, target, xp, display_order) VALUES
    ('AULAS_2', 'Conclua 2 aulas', 'Duas aulas diferentes contam, até as que você já tinha visto.', 'LESSON', 2, 30, 1),
    ('QUIZ_PERFEITO', 'Gabarite um quiz', 'Acerte todas as perguntas do quiz de uma aula.', 'QUIZ_PERFECT', 1, 25, 2),
    ('CARTEIRA_3', 'Diversifique', 'Numa rodada ou num treino, monte uma carteira com 3 ativos ou mais.', 'PORTFOLIO_3', 1, 25, 3),
    ('TREINO', 'Faça um treino', 'Remonte a carteira de uma rodada que já acabou.', 'PRACTICE', 1, 20, 4),
    ('TREINO_2', 'Treine em 2 rodadas', 'Faça treinos em duas rodadas passadas diferentes.', 'PRACTICE', 2, 30, 5),
    ('DOIS_DIAS', 'Volte em 2 dias', 'Abra o RetroBolsa em dois dias diferentes da semana.', 'VISIT', 2, 20, 6)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE user_missions (
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mission_code VARCHAR(30) NOT NULL REFERENCES mission_templates(code) ON DELETE CASCADE,
    iso_week     VARCHAR(8) NOT NULL,
    progress     INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP,
    PRIMARY KEY (user_id, mission_code, iso_week)
);

CREATE TABLE user_mission_refs (
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mission_code VARCHAR(30) NOT NULL REFERENCES mission_templates(code) ON DELETE CASCADE,
    iso_week     VARCHAR(8) NOT NULL,
    ref_id       VARCHAR(80) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, mission_code, iso_week, ref_id)
);

ALTER TABLE xp_events DROP CONSTRAINT xp_events_source_check;
ALTER TABLE xp_events ADD CONSTRAINT xp_events_source_check
    CHECK (source IN ('LESSON', 'QUIZ_PERFECT', 'PORTFOLIO', 'BEAT_CDI', 'ACHIEVEMENT', 'PRACTICE', 'MISSION'));
