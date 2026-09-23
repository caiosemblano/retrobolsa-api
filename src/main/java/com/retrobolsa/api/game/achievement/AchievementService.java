package com.retrobolsa.api.game.achievement;

import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.game.portfolio.Allocation;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    /** Contas administrativas não são jogadores: não ganham conquistas, como já não entram nos rankings. */
    private static final String ADMIN_ROLE = "ADMIN";

    /** Rentabilidade da rodada, em pontos percentuais, a partir da qual vale "Dois Dígitos". */
    static final BigDecimal DOUBLE_DIGIT_RETURN = BigDecimal.TEN;
    static final int DIVERSIFIED_ASSET_COUNT = 5;
    static final int VETERAN_ROUNDS = 5;
    /** Vencer sozinho não é vencer: o título exige ao menos um adversário. */
    static final int MIN_PLAYERS_FOR_CHAMPION = 2;
    /** Com 3 jogadores ou menos, todo mundo estaria no pódio. */
    static final int MIN_PLAYERS_FOR_PODIUM = 4;

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    // -------------------------------------------------------------------------
    // Gatilhos
    // -------------------------------------------------------------------------

    /** Avalia as conquistas de montagem de carteira, logo após o envio. */
    @Transactional
    public List<String> evaluateOnSubmit(User user, BigDecimal budget, List<Allocation> allocations, long roundsPlayed) {
        BigDecimal totalAllocated = allocations.stream()
                .map(Allocation::getAmountInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<String> assetTypes = allocations.stream()
                .map(allocation -> allocation.getAsset().getType())
                .collect(Collectors.toSet());
        return unlock(user, codesForSubmit(budget, totalAllocated, allocations.size(), assetTypes, roundsPlayed));
    }

    /** Avalia as conquistas de resultado, quando a rodada é simulada e o rank fica definido. */
    @Transactional
    public List<String> evaluateOnRoundResult(User user, int rank, BigDecimal totalReturn, int fieldSize) {
        return unlock(user, codesForRoundResult(rank, totalReturn, fieldSize));
    }

    /** Apaga as conquistas de jogo de todos os usuários (reset do admin); as de aulas ficam. */
    @Transactional
    public void resetGameAchievements() {
        userAchievementRepository.deleteAllByAchievementCodeIn(AchievementCodes.GAME);
    }

    // -------------------------------------------------------------------------
    // Regras (puras — sem banco, testadas diretamente)
    // -------------------------------------------------------------------------

    static List<String> codesForSubmit(BigDecimal budget, BigDecimal totalAllocated, int assetCount,
                                       Set<String> assetTypes, long roundsPlayed) {
        List<String> codes = new ArrayList<>();
        codes.add(AchievementCodes.PRIMEIRA_CARTEIRA);
        if (totalAllocated.compareTo(budget) == 0) {
            codes.add(AchievementCodes.TUDO_INVESTIDO);
        }
        if (assetCount >= DIVERSIFIED_ASSET_COUNT) {
            codes.add(AchievementCodes.DIVERSIFICADOR);
        }
        if (assetTypes.contains("stock") && assetTypes.contains("bond")) {
            codes.add(AchievementCodes.EQUILIBRISTA);
        }
        if (roundsPlayed >= VETERAN_ROUNDS) {
            codes.add(AchievementCodes.VETERANO);
        }
        return codes;
    }

    static List<String> codesForRoundResult(int rank, BigDecimal totalReturn, int fieldSize) {
        List<String> codes = new ArrayList<>();
        if (rank == 1 && fieldSize >= MIN_PLAYERS_FOR_CHAMPION) {
            codes.add(AchievementCodes.CAMPEAO_RODADA);
        }
        if (rank <= 3 && fieldSize >= MIN_PLAYERS_FOR_PODIUM) {
            codes.add(AchievementCodes.PODIO);
        }
        if (totalReturn != null && totalReturn.signum() > 0) {
            codes.add(AchievementCodes.NO_AZUL);
        }
        if (totalReturn != null && totalReturn.compareTo(DOUBLE_DIGIT_RETURN) >= 0) {
            codes.add(AchievementCodes.DOIS_DIGITOS);
        }
        return codes;
    }

    // -------------------------------------------------------------------------
    // Persistência
    // -------------------------------------------------------------------------

    /**
     * Desbloqueia as conquistas informadas, ignorando as que o usuário já tem.
     * Idempotente e atômico (ON CONFLICT no banco): os gatilhos podem reavaliar
     * todas as regras a cada evento sem guardar estado próprio, e eventos
     * simultâneos do mesmo jogador não colidem.
     *
     * @return os códigos desbloqueados nesta chamada (vazio se nenhum era novo)
     * @throws IllegalStateException se algum código não existir no catálogo — é erro
     *                               de programação (constante sem linha no seed), não do usuário
     */
    @Transactional
    public List<String> unlock(User user, Collection<String> codes) {
        if (codes.isEmpty() || ADMIN_ROLE.equals(user.getRole())) {
            return List.of();
        }

        Set<String> requested = new LinkedHashSet<>(codes);
        List<Achievement> achievements = achievementRepository.findAllByCodeIn(requested);
        if (achievements.size() != requested.size()) {
            Set<String> found = achievements.stream().map(Achievement::getCode).collect(Collectors.toSet());
            requested.removeAll(found);
            throw new IllegalStateException("Conquista ausente do catálogo: " + requested);
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> newlyUnlocked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (userAchievementRepository.insertIfAbsent(user.getId(), achievement.getId(), now) > 0) {
                newlyUnlocked.add(achievement.getCode());
            }
        }
        return newlyUnlocked;
    }

    // -------------------------------------------------------------------------
    // Leitura
    // -------------------------------------------------------------------------

    /**
     * Todas as conquistas do catálogo, na ordem de exibição, marcando as que o
     * usuário já desbloqueou. Duas queries no total, independente do tamanho do catálogo.
     */
    @Transactional(readOnly = true)
    public List<AchievementResponseDto> listForUser(UUID userId) {
        Map<UUID, LocalDateTime> unlockedAt = userAchievementRepository.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(
                        unlocked -> unlocked.getId().getAchievementId(),
                        UserAchievement::getUnlockedAt));

        return achievementRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(achievement -> AchievementResponseDto.builder()
                        .code(achievement.getCode())
                        .title(achievement.getTitle())
                        .description(achievement.getDescription())
                        .rarity(achievement.getRarity())
                        .unlocked(unlockedAt.containsKey(achievement.getId()))
                        .unlockedAt(unlockedAt.get(achievement.getId()))
                        .build())
                .toList();
    }
}
