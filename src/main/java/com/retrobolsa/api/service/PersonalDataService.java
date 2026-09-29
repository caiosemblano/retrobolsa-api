package com.retrobolsa.api.service;

import com.retrobolsa.api.game.progress.XpService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LGPD: baixar os próprios dados e excluir a conta. A exclusão apaga tudo o que é
 * da pessoa (carteiras, XP, conquistas, aulas, quizzes, treinos, turmas, missões e
 * notificações) pelo ON DELETE CASCADE de cada tabela.
 */
@Service
@RequiredArgsConstructor
public class PersonalDataService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbc;
    private final XpService xpService;

    @Transactional
    public void deleteAccount(User user, String password) {
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("A senha não confere.");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("Conta de administrador não é excluída por aqui.");
        }
        userRepository.deleteById(user.getId());
    }

    /** Tudo o que o RetroBolsa guarda sobre a pessoa, em JSON legível. */
    @Transactional(readOnly = true)
    public Map<String, Object> export(User user) {
        Map<String, Object> id = Map.of("userId", user.getId());
        var progress = xpService.summary(user.getId());

        Map<String, Object> dados = new LinkedHashMap<>();
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("username", user.getUsername());
        perfil.put("email", user.getEmail());
        perfil.put("papel", user.getRole());
        perfil.put("pontos", user.getTotalScore());
        perfil.put("criadoEm", texto(user.getCreatedAt()));
        perfil.put("aceiteDeIdadeOuAutorizacaoEm", texto(user.getConsentedAt()));
        dados.put("perfil", perfil);
        dados.put("progresso", Map.of("xp", progress.getXp(), "nivel", progress.getLevel(),
                "tituloDoNivel", progress.getLevelTitle(), "semanasSeguidas", progress.getStreakWeeks()));
        dados.put("xp", lista("""
                SELECT source AS origem, ref_id AS referencia, amount AS xp, created_at AS em
                FROM xp_events WHERE user_id = :userId ORDER BY created_at""", id));
        dados.put("conquistas", lista("""
                SELECT a.code AS codigo, a.title AS titulo, ua.unlocked_at AS em
                FROM user_achievements ua JOIN achievements a ON a.id = ua.achievement_id
                WHERE ua.user_id = :userId ORDER BY ua.unlocked_at""", id));
        dados.put("aulasConcluidas", lista("""
                SELECT ar.title AS aula, p.completed_at AS em
                FROM user_article_progress p JOIN articles ar ON ar.id = p.article_id
                WHERE p.user_id = :userId ORDER BY p.completed_at""", id));
        dados.put("quizzes", lista("""
                SELECT ar.title AS aula, a.score AS acertos, a.total AS perguntas, a.created_at AS em
                FROM user_quiz_attempts a JOIN articles ar ON ar.id = a.article_id
                WHERE a.user_id = :userId ORDER BY a.created_at""", id));
        dados.put("carteiras", lista("""
                SELECT c.round_number AS rodada, c.scenario_title AS cenario, p.submitted_at AS "enviadaEm",
                       p.total_return AS rentabilidade, p.final_value AS "valorFinal", p.rank AS posicao,
                       (SELECT string_agg(COALESCE(s.real_name, s.anonymous_name) || ': R$ ' || al.amount_invested, '; ')
                        FROM allocations al JOIN assets s ON s.id = al.asset_id WHERE al.portfolio_id = p.id) AS alocacoes
                FROM portfolios p JOIN competitions c ON c.id = p.competition_id
                WHERE p.user_id = :userId ORDER BY c.round_number""", id));
        dados.put("treinos", lista("""
                SELECT c.round_number AS rodada, r.total_return AS rentabilidade, r.final_value AS "valorFinal", r.created_at AS em
                FROM practice_runs r JOIN competitions c ON c.id = r.competition_id
                WHERE r.user_id = :userId ORDER BY r.created_at""", id));
        dados.put("turmas", lista("""
                SELECT c.name AS turma, c.institution AS instituicao, m.joined_at AS "entrouEm"
                FROM classroom_members m JOIN classrooms c ON c.id = m.classroom_id
                WHERE m.user_id = :userId ORDER BY m.joined_at""", id));
        dados.put("turmasComoProfessor", lista("""
                SELECT name AS turma, institution AS instituicao, created_at AS "criadaEm", archived AS arquivada
                FROM classrooms WHERE teacher_id = :userId ORDER BY created_at""", id));
        dados.put("missoes", lista("""
                SELECT iso_week AS semana, mission_code AS missao, progress AS progresso, completed_at AS "cumpridaEm"
                FROM user_missions WHERE user_id = :userId ORDER BY iso_week""", id));
        dados.put("notificacoes", lista("""
                SELECT title AS titulo, body AS texto, created_at AS em, read_at AS "lidaEm"
                FROM notifications WHERE user_id = :userId ORDER BY created_at""", id));
        return dados;
    }

    private List<Map<String, Object>> lista(String sql, Map<String, Object> params) {
        return jdbc.queryForList(sql, params).stream()
                .map(linha -> {
                    Map<String, Object> legivel = new LinkedHashMap<>();
                    linha.forEach((coluna, valor) -> legivel.put(coluna, valor(valor)));
                    return legivel;
                })
                .toList();
    }

    private static Object valor(Object valor) {
        if (valor instanceof Timestamp t) return t.toLocalDateTime().toString();
        if (valor instanceof UUID u) return u.toString();
        return valor;
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
