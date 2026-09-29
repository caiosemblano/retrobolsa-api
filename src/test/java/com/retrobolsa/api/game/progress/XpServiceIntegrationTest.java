package com.retrobolsa.api.game.progress;

import com.retrobolsa.api.controller.AbstractIntegrationTest;
import com.retrobolsa.api.game.dto.ProgressDto;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class XpServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private XpService xpService;
    @Autowired private XpEventRepository repository;
    @Autowired private UserRepository userRepository;

    private User ana;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        userRepository.deleteAll();
        ana = userRepository.save(User.builder().username("ana").email("ana@retrobolsa.com").passwordHash("hash").build());
    }

    @Test
    void mesmaAulaNuncaDaXpDuasVezes() {
        assertThat(xpService.award(ana, XpSource.LESSON, "aula-1", XpService.LESSON_XP)).isTrue();
        assertThat(xpService.award(ana, XpSource.LESSON, "aula-1", XpService.LESSON_XP)).isFalse();
        // Mesma referência, fonte diferente: é outro ganho.
        assertThat(xpService.award(ana, XpSource.QUIZ_PERFECT, "aula-1", XpService.QUIZ_PERFECT_XP)).isTrue();

        assertThat(xpService.summary(ana.getId()).getXp()).isEqualTo(35);
    }

    @Test
    void adminNaoGanhaXp() {
        User admin = userRepository.save(User.builder()
                .username("root").email("root@retrobolsa.com").passwordHash("hash").role("ADMIN").build());

        assertThat(xpService.award(admin, XpSource.PORTFOLIO, "rodada-1", XpService.PORTFOLIO_XP)).isFalse();
        assertThat(repository.count()).isZero();
    }

    @Test
    void resumoTrazNivelProximoNivelESequencia() {
        xpService.award(ana, XpSource.PORTFOLIO, "rodada-1", 30);
        xpService.award(ana, XpSource.LESSON, "aula-1", 20);

        ProgressDto progresso = xpService.summary(ana.getId());

        assertThat(progresso.getXp()).isEqualTo(50);
        assertThat(progresso.getLevel()).isEqualTo(2);
        assertThat(progresso.getLevelTitle()).isEqualTo("Aprendiz");
        assertThat(progresso.getLevelMinXp()).isEqualTo(50);
        assertThat(progresso.getNextLevelTitle()).isEqualTo("Estagiário");
        assertThat(progresso.getNextLevelMinXp()).isEqualTo(150);
        assertThat(progresso.getStreakWeeks()).isEqualTo(1);
    }

    @Test
    void novidadesAteSeremVistas() {
        xpService.award(ana, XpSource.LESSON, "aula-1", 20);
        xpService.award(ana, XpSource.LESSON, "aula-2", 20);

        List<XpEvent> novas = xpService.unseen(ana.getId());
        assertThat(novas).hasSize(2);

        xpService.markSeen(ana.getId(), Set.of(novas.get(0).getId()));
        assertThat(xpService.unseen(ana.getId())).extracting(XpEvent::getRefId).containsExactly("aula-2");
    }

    @Test
    void naoDaParaMarcarComoVistoOEventoDeOutraPessoa() {
        User bia = userRepository.save(User.builder().username("bia").email("bia@retrobolsa.com").passwordHash("hash").build());
        xpService.award(bia, XpSource.LESSON, "aula-1", 20);
        XpEvent daBia = xpService.unseen(bia.getId()).get(0);

        xpService.markSeen(ana.getId(), Set.of(daBia.getId()));

        assertThat(xpService.unseen(bia.getId())).hasSize(1);
    }

    @Test
    void resetDoJogoApagaSoOXpDeJogo() {
        xpService.award(ana, XpSource.LESSON, "aula-1", 20);
        xpService.award(ana, XpSource.PORTFOLIO, "rodada-1", 30);
        xpService.award(ana, XpSource.BEAT_CDI, "rodada-1", 20);
        xpService.award(ana, XpSource.ACHIEVEMENT, "PRIMEIRA_CARTEIRA", 10);
        xpService.award(ana, XpSource.ACHIEVEMENT, "PRIMEIRA_AULA", 10);

        xpService.resetGameXp(Set.of("PRIMEIRA_CARTEIRA"));

        assertThat(repository.findAll()).extracting(XpEvent::getRefId).containsExactlyInAnyOrder("aula-1", "PRIMEIRA_AULA");
    }
}
