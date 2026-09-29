-- ============================================================
-- V21: XP retroativo para quem já estudou ou jogou antes do XP existir
-- ============================================================
-- Aplica aos dados existentes as mesmas regras dos gatilhos (XpService,
-- AchievementService), com as datas reais de cada coisa. Tudo entra como
-- visto (seen = TRUE): o app não comemora de uma vez o que aconteceu antes.
--
-- Idempotente (ON CONFLICT DO NOTHING) e sem contas ADMIN, como os gatilhos.
-- "Constância" não é retroativa: ela mede semanas seguidas até hoje, e a
-- primeira atividade nova já a avalia.
-- ============================================================

-- Rentabilidade do CDI e da inflação no período de cada rodada, em %, com o
-- mesmo arredondamento do BenchmarkCalculator. Só rodadas com todos os anos na base.
DROP TABLE IF EXISTS referencias_rodada;
CREATE TEMPORARY TABLE referencias_rodada AS
SELECT c.id AS competition_id,
       ROUND(((EXP(SUM(LN(1 + m.cdi / 100))) - 1) * 100)::numeric, 2)  AS cdi,
       ROUND(((EXP(SUM(LN(1 + m.ipca / 100))) - 1) * 100)::numeric, 2) AS ipca
FROM competitions c
JOIN macro_indicators m ON m.year >= c.start_year AND m.year < c.end_year
GROUP BY c.id, c.start_year, c.end_year
HAVING COUNT(*) = c.end_year - c.start_year;

DROP TABLE IF EXISTS jogadores;
CREATE TEMPORARY TABLE jogadores AS
SELECT id FROM users WHERE role <> 'ADMIN';

-- ------------------------------------------------------------
-- 1. Conquistas novas (V20) que dá para tirar do histórico
-- ------------------------------------------------------------

-- Venceu a Inflação e Bateu o CDI: acima (e não igual) da referência, na data em que a rodada terminou.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT DISTINCT ON (p.user_id) p.user_id, a.id, COALESCE(c.ends_at, p.submitted_at)
FROM portfolios p
JOIN jogadores j ON j.id = p.user_id
JOIN competitions c ON c.id = p.competition_id
JOIN referencias_rodada r ON r.competition_id = c.id
JOIN achievements a ON a.code = 'VENCEU_INFLACAO'
WHERE p.total_return > r.ipca
ORDER BY p.user_id, COALESCE(c.ends_at, p.submitted_at)
ON CONFLICT DO NOTHING;

INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT DISTINCT ON (p.user_id) p.user_id, a.id, COALESCE(c.ends_at, p.submitted_at)
FROM portfolios p
JOIN jogadores j ON j.id = p.user_id
JOIN competitions c ON c.id = p.competition_id
JOIN referencias_rodada r ON r.competition_id = c.id
JOIN achievements a ON a.code = 'BATEU_CDI'
WHERE p.total_return > r.cdi
ORDER BY p.user_id, COALESCE(c.ends_at, p.submitted_at)
ON CONFLICT DO NOTHING;

-- Nota Dez: o primeiro quiz com nota máxima.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT q.user_id, a.id, MIN(q.created_at)
FROM user_quiz_attempts q
JOIN jogadores j ON j.id = q.user_id
JOIN achievements a ON a.code = 'NOTA_DEZ'
WHERE q.score = q.total
GROUP BY q.user_id, a.id
ON CONFLICT DO NOTHING;

-- Estudioso: a data em que chegou à décima aula diferente com nota máxima.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT primeiros.user_id, a.id, primeiros.quando
FROM (
    SELECT user_id, primeira AS quando,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY primeira) AS n
    FROM (
        SELECT q.user_id, q.article_id, MIN(q.created_at) AS primeira
        FROM user_quiz_attempts q
        JOIN jogadores j ON j.id = q.user_id
        WHERE q.score = q.total
        GROUP BY q.user_id, q.article_id
    ) por_aula
) primeiros
JOIN achievements a ON a.code = 'ESTUDIOSO'
WHERE primeiros.n = 10
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 2. XP de aulas, quizzes, carteiras e rodadas
-- ------------------------------------------------------------
INSERT INTO xp_events (user_id, source, ref_id, amount, seen, created_at)
SELECT p.user_id, 'LESSON', p.article_id::text, 20, TRUE, p.completed_at
FROM user_article_progress p JOIN jogadores j ON j.id = p.user_id
ON CONFLICT DO NOTHING;

INSERT INTO xp_events (user_id, source, ref_id, amount, seen, created_at)
SELECT q.user_id, 'QUIZ_PERFECT', q.article_id::text, 15, TRUE, MIN(q.created_at)
FROM user_quiz_attempts q JOIN jogadores j ON j.id = q.user_id
WHERE q.score = q.total
GROUP BY q.user_id, q.article_id
ON CONFLICT DO NOTHING;

INSERT INTO xp_events (user_id, source, ref_id, amount, seen, created_at)
SELECT p.user_id, 'PORTFOLIO', p.competition_id::text, 30, TRUE, p.submitted_at
FROM portfolios p JOIN jogadores j ON j.id = p.user_id
ON CONFLICT DO NOTHING;

INSERT INTO xp_events (user_id, source, ref_id, amount, seen, created_at)
SELECT p.user_id, 'BEAT_CDI', p.competition_id::text, 20, TRUE, COALESCE(c.ends_at, p.submitted_at)
FROM portfolios p
JOIN jogadores j ON j.id = p.user_id
JOIN competitions c ON c.id = p.competition_id
JOIN referencias_rodada r ON r.competition_id = c.id
WHERE p.total_return > r.cdi
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 3. XP de todas as conquistas já desbloqueadas, pela raridade
-- ------------------------------------------------------------
INSERT INTO xp_events (user_id, source, ref_id, amount, seen, created_at)
SELECT ua.user_id, 'ACHIEVEMENT', a.code,
       CASE a.rarity WHEN 'comum' THEN 10 WHEN 'raro' THEN 25 WHEN 'epico' THEN 50 WHEN 'lendario' THEN 100 END,
       TRUE, ua.unlocked_at
FROM user_achievements ua
JOIN jogadores j ON j.id = ua.user_id
JOIN achievements a ON a.id = ua.achievement_id
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 4. Analista (nível 5, a partir de 500 XP) e o XP dela
-- ------------------------------------------------------------
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT x.user_id, a.id, MAX(x.created_at)
FROM xp_events x
JOIN achievements a ON a.code = 'NIVEL_5'
GROUP BY x.user_id, a.id
HAVING SUM(x.amount) >= 500
ON CONFLICT DO NOTHING;

INSERT INTO xp_events (user_id, source, ref_id, amount, seen, created_at)
SELECT ua.user_id, 'ACHIEVEMENT', 'NIVEL_5', 25, TRUE, ua.unlocked_at
FROM user_achievements ua
JOIN jogadores j ON j.id = ua.user_id
JOIN achievements a ON a.id = ua.achievement_id AND a.code = 'NIVEL_5'
ON CONFLICT DO NOTHING;

DROP TABLE referencias_rodada;
DROP TABLE jogadores;
