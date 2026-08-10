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

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final AssetSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public CompetitionResponseDto getActiveCompetition() {
        Competition competition = competitionRepository.findByStatus("open")
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rodada ativa encontrada"));

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
                .assets(assetDtos)
                .build();
    }
    @Transactional
    public void nextRound() {
        Competition current = competitionRepository.findByStatus("open")
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma rodada ativa para avancar"));
        
        current.setStatus("closed");
        competitionRepository.save(current);

        int nextRoundNumber = current.getRoundNumber() + 1;
        competitionRepository.findAll().stream()
                .filter(c -> c.getRoundNumber() == nextRoundNumber)
                .findFirst()
                .ifPresent(next -> {
                    next.setStatus("open");
                    competitionRepository.save(next);
                });
    }

    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public void resetGame() {
        // Apaga todas as carteiras e alocacoes do banco
        entityManager.createNativeQuery("DELETE FROM allocations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM portfolios").executeUpdate();

        // Volta todas as competições para 'closed' e a rodada 1 para 'open'
        List<Competition> comps = competitionRepository.findAll();
        for (Competition c : comps) {
            if (c.getRoundNumber() == 1) {
                c.setStatus("open");
            } else {
                c.setStatus("closed");
            }
        }
        competitionRepository.saveAll(comps);
    }
}
