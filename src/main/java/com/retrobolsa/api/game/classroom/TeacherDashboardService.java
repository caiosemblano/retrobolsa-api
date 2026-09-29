package com.retrobolsa.api.game.classroom;

import com.retrobolsa.api.game.dto.ClassroomStudentDto;
import com.retrobolsa.api.game.dto.QuestionStatDto;
import com.retrobolsa.api.game.progress.Levels;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * O que o professor vê da turma: como cada aluno está (aulas, quizzes, rodadas,
 * XP e quando apareceu pela última vez) e as perguntas que a turma mais erra.
 * Só o username dos alunos, nunca o e-mail. Consultas agregadas no banco: uma
 * por tela, qualquer que seja o tamanho da turma.
 */
@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

    static final int MOST_MISSED_LIMIT = 10;
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClassroomService classroomService;
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<ClassroomStudentDto> students(UUID teacherId, UUID classroomId) {
        classroomService.owned(teacherId, classroomId);
        return jdbc.query("""
                SELECT u.username, m.joined_at,
                       (SELECT COUNT(*) FROM user_article_progress p WHERE p.user_id = u.id) AS lessons,
                       (SELECT COUNT(DISTINCT a.article_id) FROM user_quiz_attempts a WHERE a.user_id = u.id) AS quizzes,
                       (SELECT AVG(best) FROM (
                            SELECT MAX(a.score * 100.0 / a.total) AS best FROM user_quiz_attempts a
                            WHERE a.user_id = u.id AND a.total > 0 GROUP BY a.article_id) melhores) AS quiz_avg,
                       (SELECT COUNT(*) FROM portfolios pf WHERE pf.user_id = u.id) AS rounds,
                       (SELECT COALESCE(SUM(x.amount), 0) FROM xp_events x WHERE x.user_id = u.id) AS xp,
                       GREATEST(
                           (SELECT MAX(x.created_at) FROM xp_events x WHERE x.user_id = u.id),
                           (SELECT MAX(a.created_at) FROM user_quiz_attempts a WHERE a.user_id = u.id),
                           (SELECT MAX(pf.submitted_at) FROM portfolios pf WHERE pf.user_id = u.id),
                           (SELECT MAX(p.completed_at) FROM user_article_progress p WHERE p.user_id = u.id),
                           (SELECT MAX(r.created_at) FROM practice_runs r WHERE r.user_id = u.id)
                       ) AS last_activity
                FROM classroom_members m
                JOIN users u ON u.id = m.user_id
                WHERE m.classroom_id = :classroomId
                ORDER BY LOWER(u.username)
                """, Map.of("classroomId", classroomId), (rs, i) -> {
            int xp = rs.getInt("xp");
            Levels.Level level = Levels.of(xp);
            BigDecimal average = rs.getBigDecimal("quiz_avg");
            return ClassroomStudentDto.builder()
                    .username(rs.getString("username"))
                    .joinedAt(toLocal(rs.getTimestamp("joined_at")))
                    .lessonsCompleted(rs.getLong("lessons"))
                    .quizzesTaken(rs.getLong("quizzes"))
                    .quizAverage(average == null ? null : average.setScale(1, RoundingMode.HALF_UP))
                    .roundsPlayed(rs.getLong("rounds"))
                    .xp(xp)
                    .level(level.number())
                    .levelTitle(level.title())
                    .lastActivity(toLocal(rs.getTimestamp("last_activity")))
                    .build();
        });
    }

    /**
     * As perguntas com mais erros entre os alunos da turma, pela taxa de erro
     * (todas as tentativas contam), com a alternativa errada mais escolhida:
     * é ela que mostra qual confusão a turma está fazendo.
     */
    @Transactional(readOnly = true)
    public List<QuestionStatDto> mostMissedQuestions(UUID teacherId, UUID classroomId) {
        classroomService.owned(teacherId, classroomId);
        Map<String, Object> params = Map.of("classroomId", classroomId, "limit", MOST_MISSED_LIMIT);

        Map<UUID, String> commonWrong = new HashMap<>();
        jdbc.query("""
                SELECT DISTINCT ON (ans.question_id) ans.question_id, o.text, COUNT(*) AS vezes
                FROM user_quiz_answers ans
                JOIN user_quiz_attempts at ON at.id = ans.attempt_id
                JOIN classroom_members m ON m.user_id = at.user_id AND m.classroom_id = :classroomId
                JOIN quiz_options o ON o.id = ans.option_id
                WHERE NOT ans.correct
                GROUP BY ans.question_id, o.text
                ORDER BY ans.question_id, vezes DESC, o.text
                """, params, rs -> {
            commonWrong.put(rs.getObject("question_id", UUID.class), rs.getString("text"));
        });

        return jdbc.query("""
                SELECT q.id, q.prompt, ar.id AS article_id, ar.module_id, ar.title AS article_title,
                       COUNT(*) AS answers,
                       SUM(CASE WHEN ans.correct THEN 0 ELSE 1 END) AS wrong,
                       COUNT(DISTINCT at.user_id) AS students
                FROM user_quiz_answers ans
                JOIN user_quiz_attempts at ON at.id = ans.attempt_id
                JOIN classroom_members m ON m.user_id = at.user_id AND m.classroom_id = :classroomId
                JOIN quiz_questions q ON q.id = ans.question_id
                JOIN articles ar ON ar.id = q.article_id
                GROUP BY q.id, q.prompt, ar.id, ar.module_id, ar.title
                HAVING SUM(CASE WHEN ans.correct THEN 0 ELSE 1 END) > 0
                ORDER BY SUM(CASE WHEN ans.correct THEN 0 ELSE 1 END) * 1.0 / COUNT(*) DESC, COUNT(*) DESC, q.prompt
                LIMIT :limit
                """, params, (rs, i) -> {
            UUID questionId = rs.getObject("id", UUID.class);
            long answers = rs.getLong("answers");
            long wrong = rs.getLong("wrong");
            return QuestionStatDto.builder()
                    .questionId(questionId.toString())
                    .prompt(rs.getString("prompt"))
                    .articleId(rs.getObject("article_id", UUID.class).toString())
                    .moduleId(rs.getObject("module_id", UUID.class).toString())
                    .articleTitle(rs.getString("article_title"))
                    .answers(answers)
                    .wrong(wrong)
                    .errorRate(BigDecimal.valueOf(wrong * 100.0 / answers).setScale(1, RoundingMode.HALF_UP))
                    .students(rs.getLong("students"))
                    .commonWrongAnswer(commonWrong.get(questionId))
                    .build();
        });
    }

    /**
     * A tabela dos alunos em CSV para abrir no Excel ou no Google Planilhas em
     * português: separador ";", vírgula decimal e BOM para os acentos saírem certos.
     */
    @Transactional(readOnly = true)
    public String studentsCsv(UUID teacherId, UUID classroomId) {
        List<ClassroomStudentDto> students = students(teacherId, classroomId);
        DecimalFormat decimal = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Aluno;Aulas concluídas;Quizzes feitos;Média nos quizzes (%);Rodadas jogadas;XP;Nível;Última atividade\r\n");
        for (ClassroomStudentDto s : students) {
            csv.append(String.join(";",
                    cell(s.getUsername()),
                    String.valueOf(s.getLessonsCompleted()),
                    String.valueOf(s.getQuizzesTaken()),
                    s.getQuizAverage() == null ? "" : decimal.format(s.getQuizAverage()),
                    String.valueOf(s.getRoundsPlayed()),
                    String.valueOf(s.getXp()),
                    cell(s.getLevel() + " - " + s.getLevelTitle()),
                    s.getLastActivity() == null ? "" : s.getLastActivity().format(CSV_DATE)));
            csv.append("\r\n");
        }
        return csv.toString();
    }

    /** Aspas quando o texto tem separador, aspas ou quebra de linha (RFC 4180). */
    static String cell(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
