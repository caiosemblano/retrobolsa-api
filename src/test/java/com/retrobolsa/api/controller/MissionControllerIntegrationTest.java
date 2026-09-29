package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.mission.MissionEvent;
import com.retrobolsa.api.game.mission.MissionService;
import com.retrobolsa.api.game.mission.MissionTemplateRepository;
import com.retrobolsa.api.game.mission.Missions;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Missões pelo caminho real, com o relógio parado numa quarta-feira cuja semana
 * sorteou "Conclua 2 aulas" e "Volte em 2 dias".
 */
@TestPropertySource(properties = "retrobolsa.missions.enabled=true")
class MissionControllerIntegrationTest extends AbstractIntegrationTest {

    static final LocalDate HOJE = quartaComAsMissoes("AULAS_2", "DOIS_DIAS");
    static final String SEMANA = Missions.isoWeek(HOJE);
    private static final String AULA_1 = "bbbbbbbb-0001-0000-0000-000000000001";
    private static final String AULA_2 = "bbbbbbbb-0002-0000-0000-000000000002";

    @TestConfiguration
    static class RelogioParado {
        @Bean
        @Primary
        Clock relogioDoTeste() {
            ZoneId zona = ZoneId.systemDefault();
            return Clock.fixed(HOJE.atTime(15, 0).atZone(zona).toInstant(), zona);
        }
    }

    /** A primeira quarta-feira a partir de 2026 cuja semana sorteia todas as missões pedidas. */
    static LocalDate quartaComAsMissoes(String... codigos) {
        for (LocalDate dia = LocalDate.of(2026, 1, 7); dia.getYear() < 2031; dia = dia.plusWeeks(1)) {
            if (Missions.draw(Missions.CODES, Missions.isoWeek(dia)).containsAll(List.of(codigos))) return dia;
        }
        throw new IllegalStateException("Nenhuma semana sorteia " + List.of(codigos));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private MissionService missionService;
    @Autowired private MissionTemplateRepository templateRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private User ana;

    @BeforeEach
    void preparar() {
        userRepository.deleteAll();
        ana = userRepository.save(User.builder().username("ana").email("ana@retrobolsa.com").passwordHash("hash").build());
    }

    @Test
    @DisplayName("o catálogo do banco tem os mesmos códigos do sorteio")
    void catalogo() {
        assertThat(templateRepository.findAllByOrderByCodeAsc()).extracting("code").containsExactlyElementsOf(Missions.CODES);
    }

    @Test
    @DisplayName("a semana traz as 3 missões sorteadas, sem progresso no começo")
    void semana() throws Exception {
        mockMvc.perform(get("/api/missions/week").header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.week").value(SEMANA))
                .andExpect(jsonPath("$.missions", hasSize(3)))
                .andExpect(jsonPath("$.missions[*].code", containsInRelativeOrder(
                        Missions.draw(Missions.CODES, SEMANA).toArray())))
                .andExpect(jsonPath("$.missions[*].progress", everyItem(is(0))));
        mockMvc.perform(get("/api/missions/week")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("concluir 2 aulas diferentes cumpre a missão e dá o XP uma vez; repetir a mesma aula não conta")
    void duasAulas() throws Exception {
        passarNoQuiz(AULA_1);
        passarNoQuiz(AULA_1);
        assertThat(missao("AULAS_2")).containsEntry("progress", 1).containsEntry("completed", false);

        passarNoQuiz(AULA_2);
        assertThat(missao("AULAS_2")).containsEntry("progress", 2).containsEntry("completed", true);
        assertThat(jdbc.queryForList("SELECT ref_id, amount FROM xp_events WHERE user_id = ? AND source = 'MISSION'", ana.getId()))
                .containsExactly(Map.of("ref_id", "AULAS_2:" + SEMANA, "amount", 30));

        mockMvc.perform(get("/api/progress/news").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.items[*].label", hasItem("Missão da semana: Conclua 2 aulas")));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$[0].type").value("MISSAO_CUMPRIDA"))
                .andExpect(jsonPath("$[0].title").value("Missão cumprida: Conclua 2 aulas"))
                .andExpect(jsonPath("$[0].body").value("+30 XP"));
    }

    @Test
    @DisplayName("voltar no mesmo dia conta uma vez; num segundo dia, a missão se cumpre")
    void doisDias() throws Exception {
        visitar();
        visitar();
        assertThat(missao("DOIS_DIAS")).containsEntry("progress", 1);

        missionService.record(ana, MissionEvent.VISIT, HOJE.plusDays(1).toString());
        assertThat(missao("DOIS_DIAS")).containsEntry("progress", 2).containsEntry("completed", true);
    }

    @Test
    @DisplayName("evento de missão que não caiu nesta semana não mexe em nada, e admin não participa")
    void foraDaSemanaEAdmin() {
        String foraDoSorteio = Missions.CODES.stream()
                .filter(c -> !Missions.draw(Missions.CODES, SEMANA).contains(c)).findFirst().orElseThrow();
        MissionEvent evento = templateRepository.findById(foraDoSorteio).orElseThrow().getEvent();
        boolean eventoTambemSorteado = Missions.draw(Missions.CODES, SEMANA).stream()
                .anyMatch(c -> templateRepository.findById(c).orElseThrow().getEvent() == evento);
        missionService.record(ana, evento, "qualquer");
        if (!eventoTambemSorteado) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_missions WHERE user_id = ?", Integer.class, ana.getId()))
                    .isZero();
        }

        User admin = userRepository.save(User.builder().username("root").email("root@retrobolsa.com")
                .passwordHash("hash").role("ADMIN").build());
        missionService.record(admin, MissionEvent.VISIT, HOJE.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_missions WHERE user_id = ?", Integer.class, admin.getId()))
                .isZero();
    }

    private void passarNoQuiz(String aula) throws Exception {
        mockMvc.perform(post("/api/articles/" + aula + "/quiz").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content(QuizRespostas.certas(jdbc, aula)))
                .andExpect(status().isOk());
    }

    private void visitar() throws Exception {
        mockMvc.perform(post("/api/missions/visit").header("Authorization", bearer(ana))).andExpect(status().isNoContent());
    }

    private Map<String, Object> missao(String codigo) throws Exception {
        String json = mockMvc.perform(get("/api/missions/week").header("Authorization", bearer(ana)))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> missoes = com.jayway.jsonpath.JsonPath.read(json, "$.missions[?(@.code == '" + codigo + "')]");
        return missoes.get(0);
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }
}
