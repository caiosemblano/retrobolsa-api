package com.retrobolsa.api.game.mission;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.dto.MissionWeekDto;
import com.retrobolsa.api.game.progress.XpService;
import com.retrobolsa.api.game.progress.XpSource;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Missões semanais: três por semana, iguais para todos. Os ganchos (aula, quiz,
 * carteira, treino e visita) chamam {@link #record}; quando a missão chega ao alvo,
 * vira XP (fonte MISSION), uma vez só.
 */
@Service
@RequiredArgsConstructor
public class MissionService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final MissionTemplateRepository templateRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final XpService xpService;
    private final AchievementService achievementService;
    private final Clock clock;

    /** As missões que valem na semana da data, na ordem do sorteio. */
    @Transactional(readOnly = true)
    public List<MissionTemplate> drawn(LocalDate date) {
        List<MissionTemplate> templates = templateRepository.findAllByOrderByCodeAsc();
        Map<String, MissionTemplate> byCode = templates.stream()
                .collect(Collectors.toMap(MissionTemplate::getCode, Function.identity()));
        return Missions.draw(templates.stream().map(MissionTemplate::getCode).toList(), Missions.isoWeek(date))
                .stream().map(byCode::get).toList();
    }

    /**
     * Conta o evento nas missões da semana que dependem dele. A mesma referência (a
     * mesma aula, a mesma rodada, o mesmo dia) conta uma vez por missão e semana.
     *
     * @return as missões concluídas agora (vazia na maioria das vezes)
     */
    @Transactional
    public List<MissionTemplate> record(User user, MissionEvent event, String ref) {
        if (ADMIN_ROLE.equals(user.getRole())) return List.of();
        LocalDateTime now = LocalDateTime.now(clock);
        String week = Missions.isoWeek(now.toLocalDate());

        List<MissionTemplate> completedNow = new ArrayList<>();
        for (MissionTemplate mission : drawn(now.toLocalDate())) {
            if (mission.getEvent() != event) continue;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", user.getId())
                    .addValue("code", mission.getCode())
                    .addValue("week", week)
                    .addValue("ref", ref)
                    .addValue("now", Timestamp.valueOf(now));
            int counted = jdbc.update("""
                    INSERT INTO user_mission_refs (user_id, mission_code, iso_week, ref_id, created_at)
                    VALUES (:userId, :code, :week, :ref, :now)
                    ON CONFLICT DO NOTHING
                    """, params);
            if (counted == 0) continue;

            Integer progress = jdbc.queryForObject("""
                    INSERT INTO user_missions (user_id, mission_code, iso_week, progress)
                    VALUES (:userId, :code, :week, 1)
                    ON CONFLICT (user_id, mission_code, iso_week)
                    DO UPDATE SET progress = user_missions.progress + 1
                    RETURNING progress
                    """, params, Integer.class);
            // Exatamente no alvo: a conclusão (e o XP) acontece uma vez, mesmo que o progresso passe dele.
            if (progress != null && progress == mission.getTarget()) {
                jdbc.update("""
                        UPDATE user_missions SET completed_at = :now
                        WHERE user_id = :userId AND mission_code = :code AND iso_week = :week AND completed_at IS NULL
                        """, params);
                if (xpService.award(user, XpSource.MISSION, mission.getCode() + ":" + week, mission.getXp())) {
                    achievementService.evaluateProgress(user);
                }
                completedNow.add(mission);
            }
        }
        return completedNow;
    }

    /** O jogador abriu o app: conta para as missões de "volte em N dias". */
    @Transactional
    public void recordVisit(User user) {
        record(user, MissionEvent.VISIT, LocalDate.now(clock).toString());
    }

    @Transactional(readOnly = true)
    public MissionWeekDto week(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        String week = Missions.isoWeek(today);
        Map<String, Progress> progress = new HashMap<>();
        jdbc.query("""
                SELECT mission_code, progress, completed_at FROM user_missions
                WHERE user_id = :userId AND iso_week = :week
                """, Map.of("userId", userId, "week", week), rs -> {
            progress.put(rs.getString("mission_code"),
                    new Progress(rs.getInt("progress"), rs.getTimestamp("completed_at") != null));
        });

        List<MissionWeekDto.Mission> missions = drawn(today).stream()
                .map(m -> {
                    Progress p = progress.getOrDefault(m.getCode(), new Progress(0, false));
                    return MissionWeekDto.Mission.builder()
                            .code(m.getCode())
                            .title(m.getTitle())
                            .description(m.getDescription())
                            .target(m.getTarget())
                            .progress(Math.min(p.value(), m.getTarget()))
                            .completed(p.completed())
                            .xp(m.getXp())
                            .build();
                })
                .toList();
        return MissionWeekDto.builder().week(week).endsAt(Missions.weekEnd(today)).missions(missions).build();
    }

    private record Progress(int value, boolean completed) {}
}
