package com.retrobolsa.api.game.notification;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notificações dentro do app. Cada evento vira uma linha por destinatário, gravada
 * com um único INSERT ... SELECT, qualquer que seja o número de jogadores.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String ROUND_OPENED = "RODADA_ABERTA";
    public static final String RESULT_REVEALED = "RESULTADO_REVELADO";
    public static final String NEW_ASSIGNMENT = "TAREFA_NOVA";
    public static final String MISSION_COMPLETED = "MISSAO_CUMPRIDA";

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    // -------------------------------------------------------------------------
    // Eventos
    // -------------------------------------------------------------------------

    /** Rodada aberta: para todos os jogadores (contas ADMIN não jogam). */
    @Transactional
    public void roundOpened(Competition competition) {
        String body = (competition.getScenarioTitle() != null ? competition.getScenarioTitle() + ". " : "")
                + (competition.getEndsAt() != null
                ? "Monte sua carteira até " + competition.getEndsAt().format(DAY) + "."
                : "Monte sua carteira.");
        insertFor("SELECT id FROM users WHERE role <> 'ADMIN'", Map.of(),
                ROUND_OPENED, "Rodada " + competition.getRoundNumber() + " aberta", body, "/rodada/contexto");
    }

    /** Resultado revelado: para quem enviou carteira na rodada. */
    @Transactional
    public void resultRevealed(Competition competition) {
        insertFor("SELECT user_id FROM portfolios WHERE competition_id = :competitionId",
                Map.of("competitionId", competition.getId()),
                RESULT_REVEALED, "Resultado da rodada " + competition.getRoundNumber(),
                "Os nomes das empresas foram revelados: veja como a sua carteira foi.", "/rodada/resultado");
    }

    /** Tarefa nova: para os alunos da turma. */
    @Transactional
    public void newAssignment(UUID classroomId, String classroomName, String articleTitle, UUID moduleId,
                              UUID articleId, LocalDateTime dueAt) {
        insertFor("SELECT user_id FROM classroom_members WHERE classroom_id = :classroomId",
                Map.of("classroomId", classroomId),
                NEW_ASSIGNMENT, "Tarefa nova: " + articleTitle,
                classroomName + " · prazo " + dueAt.format(DAY_TIME),
                "/aprender/" + moduleId + "/" + articleId);
    }

    /** Missão da semana cumprida: para o próprio jogador. */
    @Transactional
    public void missionCompleted(UUID userId, String missionTitle, int xp) {
        insertFor("SELECT CAST(:userId AS uuid)", Map.of("userId", userId),
                MISSION_COMPLETED, "Missão cumprida: " + missionTitle, "+" + xp + " XP", "/");
    }

    private void insertFor(String recipients, Map<String, ?> params, String type, String title, String body, String link) {
        MapSqlParameterSource source = new MapSqlParameterSource(params)
                .addValue("type", type)
                .addValue("title", title)
                .addValue("body", body)
                .addValue("link", link)
                .addValue("now", Timestamp.valueOf(LocalDateTime.now(clock)));
        jdbc.update("INSERT INTO notifications (user_id, type, title, body, link, created_at) "
                + "SELECT r.id, :type, :title, :body, :link, :now FROM (" + recipients + ") AS r(id)", source);
    }

    // -------------------------------------------------------------------------
    // Leitura
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationDto> list(UUID userId, Integer page, Integer size) {
        int limit = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = (page == null || page < 0 ? 0 : page) * limit;
        return jdbc.query("""
                SELECT id, type, title, body, link, read_at, created_at FROM notifications
                WHERE user_id = :userId
                ORDER BY created_at DESC, id
                LIMIT :limit OFFSET :offset
                """, Map.of("userId", userId, "limit", limit, "offset", offset), (rs, i) -> NotificationDto.builder()
                .id(rs.getObject("id", UUID.class).toString())
                .type(rs.getString("type"))
                .title(rs.getString("title"))
                .body(rs.getString("body"))
                .link(rs.getString("link"))
                .read(rs.getTimestamp("read_at") != null)
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build());
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read_at IS NULL",
                Map.of("userId", userId), Long.class);
        return count == null ? 0 : count;
    }

    /** Marca como lidas as notificações informadas (só as do próprio usuário) ou, sem ids, todas. */
    @Transactional
    public void markRead(UUID userId, Collection<UUID> ids) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId)
                .addValue("now", Timestamp.valueOf(LocalDateTime.now(clock)));
        if (ids == null || ids.isEmpty()) {
            jdbc.update("UPDATE notifications SET read_at = :now WHERE user_id = :userId AND read_at IS NULL", params);
        } else {
            jdbc.update("UPDATE notifications SET read_at = :now WHERE user_id = :userId AND read_at IS NULL AND id IN (:ids)",
                    params.addValue("ids", ids));
        }
    }
}
