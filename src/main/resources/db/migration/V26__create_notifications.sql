-- ============================================================
-- V26: notificações no app (sem e-mail)
-- ============================================================
-- Rodada aberta (para todos os jogadores), resultado revelado (para
-- quem jogou), tarefa nova (para os alunos da turma) e missão cumprida.
-- link é o endereço do app que a notificação abre.
-- ============================================================

CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(30) NOT NULL
               CHECK (type IN ('RODADA_ABERTA', 'RESULTADO_REVELADO', 'TAREFA_NOVA', 'MISSAO_CUMPRIDA')),
    title      VARCHAR(120) NOT NULL,
    body       VARCHAR(300),
    link       VARCHAR(200),
    read_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id) WHERE read_at IS NULL;
