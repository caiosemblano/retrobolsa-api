package com.retrobolsa.api.game.competition;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.dto.AssetDto;
import com.retrobolsa.api.game.dto.CompetitionResponseDto;
import com.retrobolsa.api.game.dto.EconomicIndicatorDto;
import com.retrobolsa.api.game.macro.MacroIndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final AssetSnapshotRepository snapshotRepository;
    private final AchievementService achievementService;
    private final MacroIndicatorRepository macroIndicatorRepository;

    @Transactional(readOnly = true)
    public CompetitionResponseDto getActiveCompetition() {
        Competition competition = competitionRepository.findByStatus("open")
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rodada ativa encontrada"));
        return buildDto(competition);
    }

    @Transactional(readOnly = true)
    public CompetitionResponseDto getLatestCompetition() {
        List<Competition> comps = competitionRepository.findAllByOrderByRoundNumberAsc();
        if (comps.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma rodada encontrada");
        }

        // Prioriza rodada aberta, simulando, simulada ou revelada
        Competition target = comps.stream()
                .filter(c -> "open".equals(c.getStatus()) || "simulating".equals(c.getStatus()) || "simulated".equals(c.getStatus()) || "revealed".equals(c.getStatus()) || "closed".equals(c.getStatus()))
                .reduce((first, second) -> second) // pega a mais recente entre as ativas/jogadas
                .orElse(comps.get(comps.size() - 1)); // fallback para a última existente

        return buildDto(target);
    }

    private CompetitionResponseDto buildDto(Competition competition) {

        List<AssetDto> assetDtos = new ArrayList<>();
        for (Asset asset : competition.getAssets()) {
            List<AssetSnapshot> startSnapshots = snapshotRepository
                    .findByAssetIdAndYearOrderByYearAsc(asset.getId(), competition.getStartYear());

            AssetSnapshot startSnapshot = startSnapshots.isEmpty() ? null : startSnapshots.get(0);

            AssetDto.IndicatorsDto indicators = null;
            if (startSnapshot != null && "stock".equals(asset.getType())) {
                indicators = AssetDto.IndicatorsDto.builder()
                        .pl(startSnapshot.getPl())
                        .roe(startSnapshot.getRoe())
                        .dividendYield(startSnapshot.getDividendYield())
                        .lvp(startSnapshot.getLvp())
                        .lucroPositivo(startSnapshot.getLucroPositivo())
                        .cagrLucro(startSnapshot.getCagrLucro())
                        .cagrReceita(startSnapshot.getCagrReceita())
                        .margemEbitda(startSnapshot.getMargemEbitda())
                        .build();
            }

            assetDtos.add(AssetDto.builder()
                    .id(asset.getId().toString())
                    .type(asset.getType())
                    .anonymousName(asset.getAnonymousName())
                    .sector(asset.getSector())
                    .bondType(asset.getBondType())
                    .rate(startSnapshot != null ? startSnapshot.getRate() : null)
                    .indicators(indicators)
                    .build());
        }

        return CompetitionResponseDto.builder()
                .id(competition.getId().toString())
                .round(competition.getRoundNumber())
                .status(competition.getStatus())
                .daysLeft(computeDaysLeft(competition))
                .budget(competition.getBudget())
                .scenarioTitle(competition.getScenarioTitle())
                .scenarioDescription(competition.getScenarioDescription())
                .startYear(competition.getStartYear())
                .endYear(competition.getEndYear())
                .endsAt(competition.getEndsAt())
                .economicIndicators(economicIndicators(competition.getStartYear() - 1))
                .assets(assetDtos)
                .build();
    }

    /**
     * O cenário que o investidor conhecia ao montar a carteira: os números do ano
     * anterior ao início da rodada. Lista vazia se o ano não está na base.
     */
    private List<EconomicIndicatorDto> economicIndicators(int year) {
        return macroIndicatorRepository.findById(year)
                .map(macro -> List.of(
                        indicator("SELIC", "Taxa Selic",
                                // A meta só existe desde 1999; antes disso, a Selic efetiva do ano.
                                macro.getSelicMeta() != null ? macro.getSelicMeta() : macro.getSelic(),
                                "% a.a.", year),
                        indicator("IPCA", "Inflação (IPCA)", macro.getIpca(), "% no ano", year),
                        indicator("DOLAR", "Dólar", macro.getDolar(), "R$", year),
                        indicator("PIB", "Crescimento do PIB", macro.getPib(), "% no ano", year)))
                .orElse(List.of());
    }

    private EconomicIndicatorDto indicator(String code, String label, BigDecimal value, String unit, int year) {
        return EconomicIndicatorDto.builder()
                .code(code).label(label).value(value).unit(unit).year(year).build();
    }
    /**
     * daysLeft é calculado a partir de endsAt a cada leitura (em vez de depender
     * de um valor gravado uma vez na criação), para nunca ficar desatualizado.
     */
    private Integer computeDaysLeft(Competition competition) {
        if (!"open".equals(competition.getStatus()) || competition.getEndsAt() == null) {
            return competition.getDaysLeft();
        }
        long days = Duration.between(LocalDateTime.now(), competition.getEndsAt()).toDays();
        return (int) Math.max(0, days);
    }

    @Transactional
    public void nextRound() {
        Competition current = competitionRepository.findByStatus("open")
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rodada ativa para avancar"));

        Competition next = competitionRepository.findAllByOrderByRoundNumberAsc().stream()
                .filter(c -> c.getRoundNumber() == current.getRoundNumber() + 1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nao existe uma proxima rodada cadastrada"));
        if (!"draft".equals(next.getStatus()) && !"closed".equals(next.getStatus())) {
            throw new IllegalArgumentException("A proxima rodada nao pode ser iniciada no status atual");
        }

        current.setStatus("closed");
        next.setStatus("open");
        competitionRepository.save(current);
        competitionRepository.save(next);
    }

    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public void startRound(UUID id) {
        Competition target = competitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rodada nao encontrada"));

        if ("open".equals(target.getStatus())) {
            return;
        }
        if (!"draft".equals(target.getStatus()) && !"closed".equals(target.getStatus())) {
            throw new IllegalArgumentException("A rodada nao pode ser iniciada no status atual");
        }

        competitionRepository.findByStatus("open").ifPresent(current -> {
            current.setStatus("closed");
            competitionRepository.save(current);
        });
        target.setStatus("open");
        competitionRepository.save(target);
    }

    @Transactional(readOnly = true)
    public List<Competition> listCompetitions() {
        return competitionRepository.findAllByOrderByRoundNumberAsc();
    }

    @Transactional
    public void resetGame() {
        entityManager.createNativeQuery("DELETE FROM allocations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM portfolios").executeUpdate();
        entityManager.createNativeQuery("UPDATE users SET total_score = 0").executeUpdate();
        // Sem carteiras e sem pontos, as conquistas de jogo ficariam sem nada por trás.
        // As de aulas continuam: o progresso das aulas não é apagado pelo reset.
        achievementService.resetGameAchievements();

        List<Competition> comps = competitionRepository.findAll();
        boolean foundRoundOne = false;
        for (Competition c : comps) {
            if (c.getRoundNumber() == 1) {
                c.setStatus("open");
                foundRoundOne = true;
            } else {
                c.setStatus("closed");
            }
        }
        if (!foundRoundOne) {
            throw new IllegalArgumentException("Nao existe uma rodada 1 cadastrada");
        }
        competitionRepository.saveAll(comps);
    }
}
