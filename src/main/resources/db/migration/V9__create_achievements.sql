-- ============================================================
-- V9 — Conquistas
-- ============================================================
-- Catálogo fixo de conquistas + desbloqueios por usuário.
-- Os códigos devem bater com AchievementCodes (Java): um code
-- ausente aqui faz o desbloqueio falhar com IllegalStateException.
--
-- rarity é texto + CHECK, no mesmo padrão de competitions.status
-- e users.role (o projeto não usa enums).
-- ============================================================

CREATE TABLE achievements (
    id            UUID         PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    title         VARCHAR(80)  NOT NULL,
    description   VARCHAR(200) NOT NULL,
    rarity        VARCHAR(20)  NOT NULL CHECK (rarity IN ('comum', 'raro', 'epico', 'lendario')),
    display_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE user_achievements (
    user_id        UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id UUID      NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    unlocked_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, achievement_id)
);

-- A PK já cobre buscas por (user_id, ...), mas o perfil lista por usuário
-- com frequência; o índice explícito deixa a intenção clara.
CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);

INSERT INTO achievements (id, code, title, description, rarity, display_order) VALUES
    ('eeeeeeee-0001-0000-0000-000000000001', 'PRIMEIRA_CARTEIRA', 'Primeira Carteira', 'Montou e enviou sua primeira carteira de investimentos.', 'comum', 1),
    ('eeeeeeee-0002-0000-0000-000000000002', 'TUDO_INVESTIDO', 'Tudo Investido', 'Alocou 100% do orçamento de uma rodada, sem deixar dinheiro parado em caixa.', 'comum', 2),
    ('eeeeeeee-0003-0000-0000-000000000003', 'PRIMEIRA_AULA', 'Estudante', 'Concluiu sua primeira aula.', 'comum', 3),
    ('eeeeeeee-0004-0000-0000-000000000004', 'NO_AZUL', 'No Azul', 'Terminou uma rodada com rentabilidade positiva.', 'comum', 4),
    ('eeeeeeee-0005-0000-0000-000000000005', 'EQUILIBRISTA', 'Equilibrista', 'Montou uma carteira combinando ações e títulos de renda fixa.', 'raro', 5),
    ('eeeeeeee-0006-0000-0000-000000000006', 'DIVERSIFICADOR', 'Diversificador', 'Montou uma carteira com 5 ativos ou mais.', 'raro', 6),
    ('eeeeeeee-0007-0000-0000-000000000007', 'DOIS_DIGITOS', 'Dois Dígitos', 'Alcançou 10% ou mais de rentabilidade em uma rodada.', 'raro', 7),
    ('eeeeeeee-0008-0000-0000-000000000008', 'VETERANO', 'Veterano', 'Participou de 5 rodadas.', 'raro', 8),
    ('eeeeeeee-0009-0000-0000-000000000009', 'PODIO', 'Pódio', 'Terminou entre os 3 primeiros de uma rodada com pelo menos 4 jogadores.', 'epico', 9),
    ('eeeeeeee-0010-0000-0000-000000000010', 'MODULO_COMPLETO', 'Módulo Concluído', 'Concluiu todas as aulas de um módulo.', 'epico', 10),
    ('eeeeeeee-0011-0000-0000-000000000011', 'CAMPEAO_RODADA', 'Campeão da Rodada', 'Venceu uma rodada disputada com pelo menos 2 jogadores.', 'lendario', 11),
    ('eeeeeeee-0012-0000-0000-000000000012', 'FORMADO', 'Formado', 'Concluiu todas as aulas de todos os módulos.', 'lendario', 12);
