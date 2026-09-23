package com.retrobolsa.api.game.achievement;

import com.retrobolsa.api.controller.AbstractIntegrationTest;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conquistas contra o Postgres real: o Flyway aplica o V9 de verdade, então aqui
 * se pega o que um teste com mocks não pega — seed desencontrado das constantes
 * Java, mapeamento errado da chave composta, cascade ausente.
 */
class AchievementCatalogIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AchievementService achievementService;
    @Autowired private AchievementRepository achievementRepository;
    @Autowired private UserAchievementRepository userAchievementRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void limparBanco() {
        // Todas as FKs para users têm ON DELETE CASCADE (V2 e V9).
        userRepository.deleteAll();
    }

    private User criarJogador(String username) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@retrobolsa.com")
                .passwordHash("hash")
                .build());
    }

    private static Set<String> codigosDeclaradosEmJava() throws IllegalAccessException {
        Set<String> codes = new HashSet<>();
        for (Field field : AchievementCodes.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                codes.add((String) field.get(null));
            }
        }
        return codes;
    }

    @Test
    @DisplayName("o seed do V9 tem exatamente as conquistas declaradas em AchievementCodes")
    void seedBateComAsConstantes() throws IllegalAccessException {
        Set<String> noBanco = new HashSet<>();
        achievementRepository.findAll().forEach(achievement -> noBanco.add(achievement.getCode()));

        assertThat(noBanco).hasSize(12).isEqualTo(codigosDeclaradosEmJava());
    }

    @Test
    @DisplayName("o catálogo sai na ordem de exibição 1..12, com título e descrição preenchidos")
    void catalogoOrdenadoECompleto() {
        List<Achievement> catalogo = achievementRepository.findAllByOrderByDisplayOrderAsc();

        assertThat(catalogo).extracting(Achievement::getDisplayOrder)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(catalogo).allSatisfy(achievement -> {
            assertThat(achievement.getTitle()).isNotBlank();
            assertThat(achievement.getDescription()).isNotBlank();
            assertThat(achievement.getRarity()).isIn("comum", "raro", "epico", "lendario");
        });
    }

    @Test
    @DisplayName("desbloqueio persiste e aparece na listagem com data; repetir não duplica")
    void desbloqueioPersisteEEIdempotente() {
        User ana = criarJogador("ana");

        List<String> primeiraVez = achievementService.unlock(ana,
                List.of(AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.NO_AZUL));
        List<String> segundaVez = achievementService.unlock(ana,
                List.of(AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.NO_AZUL));

        assertThat(primeiraVez).containsExactlyInAnyOrder(AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.NO_AZUL);
        assertThat(segundaVez).isEmpty();
        assertThat(userAchievementRepository.findAllByIdUserId(ana.getId())).hasSize(2);

        List<AchievementResponseDto> lista = achievementService.listForUser(ana.getId());
        assertThat(lista).hasSize(12);
        assertThat(lista).filteredOn(AchievementResponseDto::isUnlocked)
                .extracting(AchievementResponseDto::getCode)
                .containsExactlyInAnyOrder(AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.NO_AZUL);
        assertThat(lista).filteredOn(AchievementResponseDto::isUnlocked)
                .allSatisfy(achievement -> assertThat(achievement.getUnlockedAt()).isNotNull());
    }

    @Test
    @DisplayName("conquistas de um jogador não vazam para outro")
    void conquistasSaoPorUsuario() {
        User ana = criarJogador("ana");
        User bia = criarJogador("bia");

        achievementService.unlock(ana, List.of(AchievementCodes.CAMPEAO_RODADA));

        assertThat(achievementService.listForUser(bia.getId())).noneMatch(AchievementResponseDto::isUnlocked);
    }

    @Test
    @DisplayName("apagar o usuário apaga os desbloqueios dele (ON DELETE CASCADE)")
    void apagarUsuarioApagaDesbloqueios() {
        User ana = criarJogador("ana");
        achievementService.unlock(ana, List.of(AchievementCodes.PRIMEIRA_AULA));

        userRepository.delete(ana);

        assertThat(userAchievementRepository.count()).isZero();
    }
}
