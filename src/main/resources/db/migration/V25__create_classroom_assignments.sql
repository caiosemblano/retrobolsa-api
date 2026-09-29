-- ============================================================
-- V25: tarefas da turma
-- ============================================================
-- O professor atribui uma aula à turma, com prazo. A tarefa está feita
-- quando o aluno conclui a aula (a de quiz se conclui passando nele),
-- mesmo que já a tivesse concluído antes; sem conclusão, fica pendente
-- até o prazo e atrasada depois dele.
-- ============================================================

CREATE TABLE classroom_assignments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    classroom_id UUID NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    article_id   UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    due_at       TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (classroom_id, article_id)
);
