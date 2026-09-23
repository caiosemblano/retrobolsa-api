package com.retrobolsa.api.game.achievement;

import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AchievementService — Desbloqueio e Listagem")
class AchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;

    @InjectMocks private AchievementService achievementService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private final User jogador = User.builder().id(UUID.randomUUID()).username("ana").role("PLAYER").build();

    private Achievement conquista(String code, int ordem) {
        Achievement achievement = new Achievement();
        achievement.setId(UUID.randomUUID());
        achievement.setCode(code);
        achievement.setTitle("Título " + code);
        achievement.setDescription("Descrição " + code);
        achievement.setRarity("comum");
        achievement.setDisplayOrder(ordem);
        return achievement;
    }

    private UserAchievement desbloqueio(User user, Achievement achievement, LocalDateTime quando) {
        UserAchievement unlocked = new UserAchievement();
        unlocked.setId(new UserAchievementId(user.getId(), achievement.getId()));
        unlocked.setUnlockedAt(quando);
        return unlocked;
    }

    @SuppressWarnings("unchecked")
    private List<UserAchievement> capturarSalvos() {
        ArgumentCaptor<List<UserAchievement>> captor = ArgumentCaptor.forClass(List.class);
        verify(userAchievementRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // -------------------------------------------------------------------------
    // unlock
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("unlock")
    class Unlock {

        @Test
        @DisplayName("desbloqueia conquistas novas e devolve os códigos desbloqueados")
        void deveDesbloquearConquistasNovas() {
            Achievement primeira = conquista(AchievementCodes.PRIMEIRA_CARTEIRA, 1);
            Achievement azul = conquista(AchievementCodes.NO_AZUL, 4);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(primeira, azul));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId())).thenReturn(List.of());

            List<String> novas = achievementService.unlock(jogador,
                    List.of(AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.NO_AZUL));

            assertThat(novas).containsExactlyInAnyOrder(AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.NO_AZUL);
            List<UserAchievement> salvos = capturarSalvos();
            assertThat(salvos).hasSize(2);
            assertThat(salvos).allSatisfy(salvo -> {
                assertThat(salvo.getId().getUserId()).isEqualTo(jogador.getId());
                assertThat(salvo.getUnlockedAt()).isNotNull();
            });
            assertThat(salvos).extracting(salvo -> salvo.getId().getAchievementId())
                    .containsExactlyInAnyOrder(primeira.getId(), azul.getId());
        }

        @Test
        @DisplayName("é idempotente: não salva de novo o que o usuário já tem")
        void deveIgnorarConquistasJaDesbloqueadas() {
            Achievement primeira = conquista(AchievementCodes.PRIMEIRA_CARTEIRA, 1);
            Achievement azul = conquista(AchievementCodes.NO_AZUL, 4);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(primeira, azul));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId()))
                    .thenReturn(List.of(desbloqueio(jogador, primeira, LocalDateTime.now().minusDays(3))));

            List<String> novas = achievementService.unlock(jogador,
                    List.of(AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.NO_AZUL));

            assertThat(novas).containsExactly(AchievementCodes.NO_AZUL);
            assertThat(capturarSalvos()).singleElement()
                    .satisfies(salvo -> assertThat(salvo.getId().getAchievementId()).isEqualTo(azul.getId()));
        }

        @Test
        @DisplayName("não salva nada quando todas já estavam desbloqueadas")
        void naoDeveSalvarQuandoNadaENovo() {
            Achievement primeira = conquista(AchievementCodes.PRIMEIRA_CARTEIRA, 1);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(primeira));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId()))
                    .thenReturn(List.of(desbloqueio(jogador, primeira, LocalDateTime.now())));

            List<String> novas = achievementService.unlock(jogador, List.of(AchievementCodes.PRIMEIRA_CARTEIRA));

            assertThat(novas).isEmpty();
            assertThat(capturarSalvos()).isEmpty();
        }

        @Test
        @DisplayName("conta ADMIN nunca ganha conquista e nem consulta o banco")
        void adminNaoGanhaConquista() {
            User admin = User.builder().id(UUID.randomUUID()).username("admin").role("ADMIN").build();

            List<String> novas = achievementService.unlock(admin, List.of(AchievementCodes.CAMPEAO_RODADA));

            assertThat(novas).isEmpty();
            verifyNoInteractions(achievementRepository, userAchievementRepository);
        }

        @Test
        @DisplayName("lista vazia de códigos é um no-op")
        void listaVaziaNaoFazNada() {
            assertThat(achievementService.unlock(jogador, List.of())).isEmpty();
            verifyNoInteractions(achievementRepository, userAchievementRepository);
        }

        @Test
        @DisplayName("código fora do catálogo é erro de programação e não salva nada")
        void codigoInexistenteLancaErro() {
            Achievement primeira = conquista(AchievementCodes.PRIMEIRA_CARTEIRA, 1);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(primeira));

            assertThatThrownBy(() -> achievementService.unlock(jogador,
                    List.of(AchievementCodes.PRIMEIRA_CARTEIRA, "CODIGO_INVENTADO")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CODIGO_INVENTADO");
            verify(userAchievementRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("códigos repetidos na chamada contam uma vez só")
        void codigosRepetidosContamUmaVez() {
            Achievement azul = conquista(AchievementCodes.NO_AZUL, 4);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(azul));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId())).thenReturn(List.of());

            List<String> novas = achievementService.unlock(jogador,
                    List.of(AchievementCodes.NO_AZUL, AchievementCodes.NO_AZUL));

            assertThat(novas).containsExactly(AchievementCodes.NO_AZUL);
            assertThat(capturarSalvos()).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // listForUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listForUser")
    class ListForUser {

        @Test
        @DisplayName("devolve o catálogo inteiro na ordem, marcando só as desbloqueadas com data")
        void deveMarcarDesbloqueadas() {
            Achievement primeira = conquista(AchievementCodes.PRIMEIRA_CARTEIRA, 1);
            Achievement aula = conquista(AchievementCodes.PRIMEIRA_AULA, 3);
            Achievement campeao = conquista(AchievementCodes.CAMPEAO_RODADA, 11);
            LocalDateTime quando = LocalDateTime.of(2026, 9, 20, 14, 30);
            when(achievementRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(primeira, aula, campeao));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId()))
                    .thenReturn(List.of(desbloqueio(jogador, aula, quando)));

            List<AchievementResponseDto> lista = achievementService.listForUser(jogador.getId());

            assertThat(lista).extracting(AchievementResponseDto::getCode).containsExactly(
                    AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.PRIMEIRA_AULA, AchievementCodes.CAMPEAO_RODADA);
            assertThat(lista.get(0).isUnlocked()).isFalse();
            assertThat(lista.get(0).getUnlockedAt()).isNull();
            assertThat(lista.get(1).isUnlocked()).isTrue();
            assertThat(lista.get(1).getUnlockedAt()).isEqualTo(quando);
            assertThat(lista.get(2).isUnlocked()).isFalse();
        }

        @Test
        @DisplayName("usuário sem conquistas recebe o catálogo inteiro bloqueado")
        void usuarioNovoTudoBloqueado() {
            when(achievementRepository.findAllByOrderByDisplayOrderAsc())
                    .thenReturn(List.of(conquista(AchievementCodes.PRIMEIRA_CARTEIRA, 1), conquista(AchievementCodes.FORMADO, 12)));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId())).thenReturn(List.of());

            List<AchievementResponseDto> lista = achievementService.listForUser(jogador.getId());

            assertThat(lista).hasSize(2).noneMatch(AchievementResponseDto::isUnlocked);
        }
    }
}
