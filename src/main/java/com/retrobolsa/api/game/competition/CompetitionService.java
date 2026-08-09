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
}
