-- ============================================================
-- V23: turmas e professor
-- ============================================================
-- Professor é um papel novo (TEACHER), dado pelo ADMIN; não há
-- autocadastro. Cada turma tem um código de 6 caracteres, sem os
-- que se confundem na lousa (0/O, 1/I), que o aluno digita para entrar.
-- O professor vê só o username dos alunos, nunca o e-mail.
-- ============================================================

ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('PLAYER', 'TEACHER', 'ADMIN'));

CREATE TABLE classrooms (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(80) NOT NULL,
    institution  VARCHAR(120),
    teacher_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    join_code    CHAR(6) NOT NULL UNIQUE,
    archived     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_classrooms_teacher ON classrooms(teacher_id);

CREATE TABLE classroom_members (
    classroom_id UUID NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (classroom_id, user_id)
);

CREATE INDEX idx_classroom_members_user ON classroom_members(user_id);
