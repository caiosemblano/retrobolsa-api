package com.retrobolsa.api.game.competition;

import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.dto.AssetDto;
import com.retrobolsa.api.game.dto.CompetitionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final AssetSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public CompetitionResponseDto getActiveCompetition() {
        Competition competition = competitionRepository.findByStatus("open")
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rodada ativa encontrada"));
        return buildDto(competition);
    }

    @Transactional(readOnly = true)
    public CompetitionResponseDto getLatestCompetition() {
        Competition competition = competitionRepository.findTopByOrderByRoundNumberDesc()
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rodada encontrada"));
        return buildDto(competition);
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
                .daysLeft(competition.getDaysLeft())
                .budget(competition.getBudget())
                .scenarioTitle(competition.getScenarioTitle())
                .scenarioDescription(competition.getScenarioDescription())
                .startYear(competition.getStartYear())
                .endYear(competition.getEndYear())
                .endsAt(competition.getEndsAt())
                .assets(assetDtos)
                .build();
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
