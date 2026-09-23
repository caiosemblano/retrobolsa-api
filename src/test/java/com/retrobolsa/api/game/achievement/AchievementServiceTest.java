package com.retrobolsa.api.game.achievement;

import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.game.portfolio.Allocation;
import com.retrobolsa.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.retrobolsa.api.game.achievement.AchievementCodes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AchievementService — Regras, Desbloqueio e Listagem")
class AchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;

    @InjectMocks private AchievementService achievementService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final BigDecimal ORCAMENTO = new BigDecimal("100000.00");

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

    /** Catálogo que conhece qualquer código pedido, e banco onde tudo é desbloqueio novo. */
    private void stubCatalogoETudoNovo() {
        when(achievementRepository.findAllByCodeIn(anyCollection())).thenAnswer(invocation -> {
            Collection<String> codes = invocation.getArgument(0);
            return codes.stream().map(code -> conquista(code, 0)).toList();
        });
        when(userAchievementRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
    }

    private Allocation alocacao(String tipo, String valor) {
        return Allocation.builder()
                .asset(Asset.builder().id(UUID.randomUUID()).type(tipo).build())
                .amountInvested(new BigDecimal(valor))
                .build();
    }

    // -------------------------------------------------------------------------
    // Regras de montagem de carteira
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("regras ao enviar carteira")
    class RegrasDeEnvio {

        @Test
        @DisplayName("toda carteira enviada vale Primeira Carteira")
        void todaCarteiraValePrimeira() {
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, new BigDecimal("40000"), 1, Set.of("stock"), 1))
                    .containsExactly(PRIMEIRA_CARTEIRA);
        }

        @Test
        @DisplayName("Tudo Investido só com 100% exato do orçamento, independente da escala do BigDecimal")
        void tudoInvestidoSoComOrcamentoExato() {
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, new BigDecimal("100000"), 1, Set.of("stock"), 1))
                    .contains(TUDO_INVESTIDO);
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, new BigDecimal("99999.99"), 1, Set.of("stock"), 1))
                    .doesNotContain(TUDO_INVESTIDO);
        }

        @Test
        @DisplayName("Diversificador a partir de 5 ativos")
        void diversificadorAPartirDeCinco() {
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, BigDecimal.ONE, 4, Set.of("stock"), 1))
                    .doesNotContain(DIVERSIFICADOR);
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, BigDecimal.ONE, 5, Set.of("stock"), 1))
                    .contains(DIVERSIFICADOR);
        }

        @Test
        @DisplayName("Equilibrista exige ação e título na mesma carteira")
        void equilibristaExigeOsDoisTipos() {
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, BigDecimal.ONE, 2, Set.of("stock", "bond"), 1))
                    .contains(EQUILIBRISTA);
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, BigDecimal.ONE, 3, Set.of("stock"), 1))
                    .doesNotContain(EQUILIBRISTA);
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, BigDecimal.ONE, 3, Set.of("bond"), 1))
                    .doesNotContain(EQUILIBRISTA);
        }

        @Test
        @DisplayName("Veterano a partir da 5ª rodada jogada")
        void veteranoNaQuintaRodada() {
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, BigDecimal.ONE, 1, Set.of("stock"), 4))
                    .doesNotContain(VETERANO);
            assertThat(AchievementService.codesForSubmit(ORCAMENTO, BigDecimal.ONE, 1, Set.of("stock"), 5))
                    .contains(VETERANO);
        }
    }

    // -------------------------------------------------------------------------
    // Regras de resultado da rodada
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("regras de resultado da rodada")
    class RegrasDeResultado {

        @Test
        @DisplayName("Campeão exige 1º lugar e pelo menos 2 jogadores — vencer sozinho não conta")
        void campeaoExigeAdversario() {
            assertThat(AchievementService.codesForRoundResult(1, BigDecimal.ONE, 2)).contains(CAMPEAO_RODADA);
            assertThat(AchievementService.codesForRoundResult(1, BigDecimal.ONE, 1)).doesNotContain(CAMPEAO_RODADA);
            assertThat(AchievementService.codesForRoundResult(2, BigDecimal.ONE, 10)).doesNotContain(CAMPEAO_RODADA);
        }

        @Test
        @DisplayName("Pódio exige top 3 numa rodada com pelo menos 4 jogadores")
        void podioExigeQuatroJogadores() {
            assertThat(AchievementService.codesForRoundResult(3, BigDecimal.ONE, 4)).contains(PODIO);
            assertThat(AchievementService.codesForRoundResult(3, BigDecimal.ONE, 3)).doesNotContain(PODIO);
            assertThat(AchievementService.codesForRoundResult(4, BigDecimal.ONE, 10)).doesNotContain(PODIO);
        }

        @Test
        @DisplayName("o campeão de uma rodada grande também leva o pódio")
        void campeaoTambemLevaPodio() {
            assertThat(AchievementService.codesForRoundResult(1, BigDecimal.ONE, 5))
                    .contains(CAMPEAO_RODADA, PODIO);
        }

        @Test
        @DisplayName("No Azul só com rentabilidade estritamente positiva")
        void noAzulSoComPositivo() {
            assertThat(AchievementService.codesForRoundResult(9, new BigDecimal("0.01"), 10)).contains(NO_AZUL);
            assertThat(AchievementService.codesForRoundResult(9, BigDecimal.ZERO, 10)).doesNotContain(NO_AZUL);
            assertThat(AchievementService.codesForRoundResult(9, new BigDecimal("-5.00"), 10)).doesNotContain(NO_AZUL);
        }

        @Test
        @DisplayName("Dois Dígitos a partir de 10% (a rentabilidade já vem em pontos percentuais)")
        void doisDigitosAPartirDeDez() {
            assertThat(AchievementService.codesForRoundResult(9, new BigDecimal("10.00"), 10)).contains(DOIS_DIGITOS, NO_AZUL);
            assertThat(AchievementService.codesForRoundResult(9, new BigDecimal("9.99"), 10)).doesNotContain(DOIS_DIGITOS);
        }

        @Test
        @DisplayName("rentabilidade nula não quebra nem concede nada de rentabilidade")
        void rentabilidadeNula() {
            assertThat(AchievementService.codesForRoundResult(9, null, 10)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Regras de aulas
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("regras de aulas")
    class RegrasDeAulas {

        @Test
        @DisplayName("a primeira aula concluída vale Estudante")
        void primeiraAula() {
            assertThat(AchievementService.codesForLessons(1, 3, 1, 8)).containsExactly(PRIMEIRA_AULA);
        }

        @Test
        @DisplayName("Módulo Concluído só ao fechar todas as aulas do módulo")
        void moduloCompleto() {
            assertThat(AchievementService.codesForLessons(2, 3, 2, 8)).doesNotContain(MODULO_COMPLETO);
            assertThat(AchievementService.codesForLessons(3, 3, 3, 8)).contains(MODULO_COMPLETO);
        }

        @Test
        @DisplayName("Formado só ao concluir todas as aulas de todos os módulos")
        void formado() {
            assertThat(AchievementService.codesForLessons(2, 2, 7, 8)).doesNotContain(FORMADO);
            assertThat(AchievementService.codesForLessons(2, 2, 8, 8)).contains(PRIMEIRA_AULA, MODULO_COMPLETO, FORMADO);
        }

        @Test
        @DisplayName("catálogo vazio (0 de 0 aulas) não concede Módulo Concluído nem Formado")
        void catalogoVazioNaoConcede() {
            assertThat(AchievementService.codesForLessons(0, 0, 0, 0)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Gatilhos
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("gatilhos")
    class Gatilhos {

        @Test
        @DisplayName("evaluateOnSubmit soma as alocações e lê os tipos dos ativos da carteira")
        void avaliaEnvioAPartirDasAlocacoes() {
            stubCatalogoETudoNovo();
            List<Allocation> carteira = List.of(
                    alocacao("stock", "30000.00"), alocacao("stock", "20000.00"), alocacao("stock", "10000.00"),
                    alocacao("bond", "25000.00"), alocacao("bond", "15000.00"));

            List<String> novas = achievementService.evaluateOnSubmit(jogador, ORCAMENTO, carteira, 1);

            assertThat(novas).containsExactlyInAnyOrder(PRIMEIRA_CARTEIRA, TUDO_INVESTIDO, DIVERSIFICADOR, EQUILIBRISTA);
        }

        @Test
        @DisplayName("evaluateOnRoundResult desbloqueia o que a regra de resultado conceder")
        void avaliaResultado() {
            stubCatalogoETudoNovo();

            List<String> novas = achievementService.evaluateOnRoundResult(jogador, 1, new BigDecimal("12.00"), 6);

            assertThat(novas).containsExactlyInAnyOrder(CAMPEAO_RODADA, PODIO, NO_AZUL, DOIS_DIGITOS);
        }

        @Test
        @DisplayName("resultado sem nenhuma conquista não toca o banco")
        void resultadoSemConquistaNaoTocaBanco() {
            List<String> novas = achievementService.evaluateOnRoundResult(jogador, 7, new BigDecimal("-2.00"), 10);

            assertThat(novas).isEmpty();
            verifyNoInteractions(achievementRepository, userAchievementRepository);
        }

        @Test
        @DisplayName("reset apaga só as conquistas de jogo, nunca as de aulas")
        void resetApagaSoConquistasDeJogo() {
            achievementService.resetGameAchievements();

            verify(userAchievementRepository).deleteAllByAchievementCodeIn(AchievementCodes.GAME);
            assertThat(AchievementCodes.GAME).doesNotContain(PRIMEIRA_AULA, MODULO_COMPLETO, FORMADO);
        }
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
            Achievement primeira = conquista(PRIMEIRA_CARTEIRA, 1);
            Achievement azul = conquista(NO_AZUL, 4);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(primeira, azul));
            when(userAchievementRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);

            List<String> novas = achievementService.unlock(jogador, List.of(PRIMEIRA_CARTEIRA, NO_AZUL));

            assertThat(novas).containsExactlyInAnyOrder(PRIMEIRA_CARTEIRA, NO_AZUL);
            verify(userAchievementRepository).insertIfAbsent(eq(jogador.getId()), eq(primeira.getId()), any());
            verify(userAchievementRepository).insertIfAbsent(eq(jogador.getId()), eq(azul.getId()), any());
        }

        @Test
        @DisplayName("é idempotente: o que o banco já tinha (insert devolve 0) não conta como novo")
        void deveIgnorarConquistasJaDesbloqueadas() {
            Achievement primeira = conquista(PRIMEIRA_CARTEIRA, 1);
            Achievement azul = conquista(NO_AZUL, 4);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(primeira, azul));
            when(userAchievementRepository.insertIfAbsent(any(), eq(primeira.getId()), any())).thenReturn(0);
            when(userAchievementRepository.insertIfAbsent(any(), eq(azul.getId()), any())).thenReturn(1);

            List<String> novas = achievementService.unlock(jogador, List.of(PRIMEIRA_CARTEIRA, NO_AZUL));

            assertThat(novas).containsExactly(NO_AZUL);
        }

        @Test
        @DisplayName("conta ADMIN nunca ganha conquista e nem consulta o banco")
        void adminNaoGanhaConquista() {
            User admin = User.builder().id(UUID.randomUUID()).username("admin").role("ADMIN").build();

            List<String> novas = achievementService.unlock(admin, List.of(CAMPEAO_RODADA));

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
        @DisplayName("código fora do catálogo é erro de programação e não grava nada")
        void codigoInexistenteLancaErro() {
            when(achievementRepository.findAllByCodeIn(anyCollection()))
                    .thenReturn(List.of(conquista(PRIMEIRA_CARTEIRA, 1)));

            assertThatThrownBy(() -> achievementService.unlock(jogador, List.of(PRIMEIRA_CARTEIRA, "CODIGO_INVENTADO")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CODIGO_INVENTADO");
            verify(userAchievementRepository, never()).insertIfAbsent(any(), any(), any());
        }

        @Test
        @DisplayName("códigos repetidos na chamada geram um único insert")
        void codigosRepetidosContamUmaVez() {
            Achievement azul = conquista(NO_AZUL, 4);
            when(achievementRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(azul));
            when(userAchievementRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);

            List<String> novas = achievementService.unlock(jogador, List.of(NO_AZUL, NO_AZUL));

            assertThat(novas).containsExactly(NO_AZUL);
            verify(userAchievementRepository, times(1)).insertIfAbsent(any(), any(), any());
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
            Achievement primeira = conquista(PRIMEIRA_CARTEIRA, 1);
            Achievement aula = conquista(PRIMEIRA_AULA, 3);
            Achievement campeao = conquista(CAMPEAO_RODADA, 11);
            LocalDateTime quando = LocalDateTime.of(2026, 9, 20, 14, 30);
            when(achievementRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(primeira, aula, campeao));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId()))
                    .thenReturn(List.of(desbloqueio(jogador, aula, quando)));

            List<AchievementResponseDto> lista = achievementService.listForUser(jogador.getId());

            assertThat(lista).extracting(AchievementResponseDto::getCode)
                    .containsExactly(PRIMEIRA_CARTEIRA, PRIMEIRA_AULA, CAMPEAO_RODADA);
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
                    .thenReturn(List.of(conquista(PRIMEIRA_CARTEIRA, 1), conquista(FORMADO, 12)));
            when(userAchievementRepository.findAllByIdUserId(jogador.getId())).thenReturn(List.of());

            List<AchievementResponseDto> lista = achievementService.listForUser(jogador.getId());

            assertThat(lista).hasSize(2).noneMatch(AchievementResponseDto::isUnlocked);
        }
    }
}
