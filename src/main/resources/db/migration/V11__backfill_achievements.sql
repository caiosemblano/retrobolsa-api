-- ============================================================
-- V11 — Backfill das conquistas
-- ============================================================
-- As conquistas nasceram no V9, mas já havia jogadores com carteiras
-- simuladas e aulas concluídas. Os gatilhos só avaliam eventos novos,
-- então sem este backfill quem já jogou veria tudo bloqueado — e quem
-- já tinha concluído todas as aulas nunca mais concluiria uma nova
-- para disparar o "Formado".
--
-- Cada INSERT reproduz uma regra de AchievementService. Os limiares
-- estão repetidos aqui de propósito: este script roda uma única vez,
-- sobre o estado do banco no momento do deploy.
--
-- unlocked_at usa a data real do feito sempre que o banco a tem
-- (envio da carteira, fim da rodada, conclusão da aula), não a data
-- do deploy. ON CONFLICT DO NOTHING deixa o script idempotente.
-- Contas ADMIN ficam de fora, como nos gatilhos.
-- ============================================================

-- ------------------------------------------------------------
-- Montagem de carteira
-- ------------------------------------------------------------

-- Primeira Carteira: qualquer carteira enviada.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(p.submitted_at)
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN achievements a ON a.code = 'PRIMEIRA_CARTEIRA'
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Tudo Investido: soma das alocações igual ao orçamento da rodada.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(p.submitted_at)
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN competitions c ON c.id = p.competition_id
JOIN achievements a ON a.code = 'TUDO_INVESTIDO'
WHERE (SELECT COALESCE(SUM(al.amount_invested), 0) FROM allocations al WHERE al.portfolio_id = p.id) = c.budget
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Diversificador: 5 ativos ou mais na mesma carteira.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(p.submitted_at)
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN achievements a ON a.code = 'DIVERSIFICADOR'
WHERE (SELECT COUNT(*) FROM allocations al WHERE al.portfolio_id = p.id) >= 5
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Equilibrista: ação e título na mesma carteira.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(p.submitted_at)
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN achievements a ON a.code = 'EQUILIBRISTA'
WHERE EXISTS (SELECT 1 FROM allocations al JOIN assets s ON s.id = al.asset_id
              WHERE al.portfolio_id = p.id AND s.type = 'stock')
  AND EXISTS (SELECT 1 FROM allocations al JOIN assets s ON s.id = al.asset_id
              WHERE al.portfolio_id = p.id AND s.type = 'bond')
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Veterano: 5ª carteira enviada (a data é a da 5ª).
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT ranked.user_id, a.id, ranked.submitted_at
FROM (
    SELECT p.user_id, p.submitted_at,
           ROW_NUMBER() OVER (PARTITION BY p.user_id ORDER BY p.submitted_at) AS nth
    FROM portfolios p
) ranked
JOIN users u ON u.id = ranked.user_id AND u.role <> 'ADMIN'
JOIN achievements a ON a.code = 'VETERANO'
WHERE ranked.nth = 5
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- ------------------------------------------------------------
-- Resultado da rodada (só carteiras já simuladas: rank definido)
-- ------------------------------------------------------------

-- Campeão da Rodada: 1º lugar numa rodada com pelo menos 2 jogadores.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(COALESCE(c.ends_at, p.submitted_at))
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN competitions c ON c.id = p.competition_id
JOIN achievements a ON a.code = 'CAMPEAO_RODADA'
WHERE p.rank = 1
  AND (SELECT COUNT(*) FROM portfolios other WHERE other.competition_id = p.competition_id) >= 2
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Pódio: top 3 numa rodada com pelo menos 4 jogadores.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(COALESCE(c.ends_at, p.submitted_at))
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN competitions c ON c.id = p.competition_id
JOIN achievements a ON a.code = 'PODIO'
WHERE p.rank <= 3
  AND (SELECT COUNT(*) FROM portfolios other WHERE other.competition_id = p.competition_id) >= 4
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- No Azul: rentabilidade positiva (total_return já está em pontos percentuais).
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(COALESCE(c.ends_at, p.submitted_at))
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN competitions c ON c.id = p.competition_id
JOIN achievements a ON a.code = 'NO_AZUL'
WHERE p.rank IS NOT NULL AND p.total_return > 0
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Dois Dígitos: 10% ou mais numa rodada.
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT p.user_id, a.id, MIN(COALESCE(c.ends_at, p.submitted_at))
FROM portfolios p
JOIN users u ON u.id = p.user_id AND u.role <> 'ADMIN'
JOIN competitions c ON c.id = p.competition_id
JOIN achievements a ON a.code = 'DOIS_DIGITOS'
WHERE p.rank IS NOT NULL AND p.total_return >= 10
GROUP BY p.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- ------------------------------------------------------------
-- Aulas
-- ------------------------------------------------------------

-- Estudante: ao menos uma aula concluída (data da primeira).
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT pr.user_id, a.id, MIN(pr.completed_at)
FROM user_article_progress pr
JOIN users u ON u.id = pr.user_id AND u.role <> 'ADMIN'
JOIN achievements a ON a.code = 'PRIMEIRA_AULA'
GROUP BY pr.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Módulo Concluído: todas as aulas de algum módulo (data em que o primeiro fechou).
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT done.user_id, a.id, MIN(done.finished_at)
FROM (
    SELECT pr.user_id, ar.module_id, COUNT(*) AS completed, MAX(pr.completed_at) AS finished_at
    FROM user_article_progress pr
    JOIN articles ar ON ar.id = pr.article_id
    GROUP BY pr.user_id, ar.module_id
) done
JOIN (SELECT module_id, COUNT(*) AS total FROM articles GROUP BY module_id) m ON m.module_id = done.module_id
JOIN users u ON u.id = done.user_id AND u.role <> 'ADMIN'
JOIN achievements a ON a.code = 'MODULO_COMPLETO'
WHERE done.completed >= m.total
GROUP BY done.user_id, a.id
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Formado: todas as aulas de todos os módulos (data da última).
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
SELECT pr.user_id, a.id, MAX(pr.completed_at)
FROM user_article_progress pr
JOIN users u ON u.id = pr.user_id AND u.role <> 'ADMIN'
JOIN achievements a ON a.code = 'FORMADO'
GROUP BY pr.user_id, a.id
HAVING COUNT(*) >= (SELECT COUNT(*) FROM articles)
   AND (SELECT COUNT(*) FROM articles) > 0
ON CONFLICT (user_id, achievement_id) DO NOTHING;
