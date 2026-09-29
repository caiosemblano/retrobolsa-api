-- ============================================================
-- V20: seis conquistas novas, ligadas a quiz, referências e constância
-- ============================================================
-- Mesmo formato do V9. As regras ficam no AchievementService; o
-- AchievementCatalogIntegrationTest garante que Java e banco batem.
-- ============================================================

INSERT INTO achievements (id, code, title, description, rarity, display_order) VALUES
    ('eeeeeeee-0013-0000-0000-000000000013', 'NOTA_DEZ', 'Nota Dez', 'Acertou todas as perguntas do quiz de uma aula.', 'comum', 13),
    ('eeeeeeee-0014-0000-0000-000000000014', 'VENCEU_INFLACAO', 'Venceu a Inflação', 'Terminou uma rodada com rentabilidade acima da inflação do período.', 'comum', 14),
    ('eeeeeeee-0015-0000-0000-000000000015', 'BATEU_CDI', 'Bateu o CDI', 'Terminou uma rodada com rentabilidade acima do CDI do período.', 'raro', 15),
    ('eeeeeeee-0016-0000-0000-000000000016', 'ESTUDIOSO', 'Estudioso', 'Tirou nota máxima no quiz de 10 aulas diferentes.', 'raro', 16),
    ('eeeeeeee-0017-0000-0000-000000000017', 'NIVEL_5', 'Analista', 'Chegou ao nível 5, Analista.', 'raro', 17),
    ('eeeeeeee-0018-0000-0000-000000000018', 'CONSTANCIA', 'Constância', 'Estudou ou jogou em 4 semanas seguidas.', 'epico', 18)
ON CONFLICT (code) DO NOTHING;
