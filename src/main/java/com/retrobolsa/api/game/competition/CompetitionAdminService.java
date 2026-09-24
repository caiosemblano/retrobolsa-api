package com.retrobolsa.api.game.competition;

import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.dto.AdminAssetDto;
import com.retrobolsa.api.game.dto.CreateCompetitionRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Montagem de rodadas pelo painel administrativo. */
@Service
@RequiredArgsConstructor
public class CompetitionAdminService {

    private final CompetitionRepository competitionRepository;
    private final AssetRepository assetRepository;
    private final AssetSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public List<AdminAssetDto> listAssets() {
        Map<UUID, List<Integer>> yearsByAsset = yearsWithReturnByAsset();
        return assetRepository.findAll().stream()
                .sorted(Comparator.comparing(Asset::getType)
                        .thenComparing(asset -> yearsByAsset.getOrDefault(asset.getId(), List.of()).stream()
                                .findFirst().orElse(0))
                        .thenComparing(Asset::getAnonymousName))
                .map(asset -> AdminAssetDto.builder()
                        .id(asset.getId().toString())
                        .anonymousName(asset.getAnonymousName())
                        .realName(asset.getRealName())
                        .ticker(asset.getTicker())
                        .type(asset.getType())
                        .sector(asset.getSector())
                        .bondType(asset.getBondType())
                        .years(yearsByAsset.getOrDefault(asset.getId(), List.of()))
                        .build())
                .toList();
    }

    @Transactional
    public Competition create(CreateCompetitionRequestDto request) {
        if (request.getEndYear() <= request.getStartYear()) {
            throw new IllegalArgumentException("O ano final deve ser posterior ao ano inicial");
        }
        if (competitionRepository.existsByRoundNumber(request.getRoundNumber())) {
            throw new IllegalArgumentException("Ja existe uma rodada com o numero " + request.getRoundNumber());
        }
        List<Asset> assets = assetRepository.findAllById(new LinkedHashSet<>(request.getAssetIds()));
        if (assets.size() != new HashSet<>(request.getAssetIds()).size()) {
            throw new IllegalArgumentException("Um ou mais ativos nao foram encontrados");
        }
        validateAnonymousNames(assets);
        validateYears(assets, request.getStartYear(), request.getEndYear());

        Competition competition = Competition.builder()
                .roundNumber(request.getRoundNumber())
                .budget(request.getBudget())
                .scenarioTitle(request.getScenarioTitle())
                .scenarioDescription(request.getScenarioDescription())
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .endsAt(request.getEndsAt())
                .assets(assets)
                .build();
        return competitionRepository.save(competition);
    }

    /** O jogador só vê o nome anônimo; dois "Empresa A" na mesma rodada seriam indistinguíveis. */
    private void validateAnonymousNames(List<Asset> assets) {
        Map<String, List<Asset>> byName = assets.stream()
                .collect(Collectors.groupingBy(Asset::getAnonymousName, LinkedHashMap::new, Collectors.toList()));
        byName.forEach((name, sameName) -> {
            if (sameName.size() > 1) {
                throw new IllegalArgumentException("Ativos com o mesmo nome anonimo (" + name + "): "
                        + sameName.stream().map(this::label).collect(Collectors.joining(", ")));
            }
        });
    }

    /**
     * A simulação aplica o retorno de start_year até end_year - 1. Um ano sem
     * retorno seria contado como 0% sem aviso, então a rodada é recusada.
     */
    private void validateYears(List<Asset> assets, int startYear, int endYear) {
        Map<UUID, List<Integer>> yearsByAsset = yearsWithReturnByAsset();
        List<String> problems = new ArrayList<>();
        for (Asset asset : assets) {
            Set<Integer> available = new HashSet<>(yearsByAsset.getOrDefault(asset.getId(), List.of()));
            List<Integer> missing = IntStream.range(startYear, endYear)
                    .filter(year -> !available.contains(year))
                    .boxed()
                    .toList();
            if (!missing.isEmpty()) {
                problems.add(label(asset) + " (faltam " + missing.stream()
                        .map(String::valueOf).collect(Collectors.joining(", ")) + ")");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Sem dados historicos para o periodo " + startYear + "-" + endYear
                    + ": " + String.join("; ", problems));
        }
    }

    private Map<UUID, List<Integer>> yearsWithReturnByAsset() {
        Map<UUID, List<Integer>> result = new HashMap<>();
        for (AssetSnapshotRepository.AssetYear row : snapshotRepository.findYearsWithReturn()) {
            result.computeIfAbsent(row.getAssetId(), id -> new ArrayList<>()).add(row.getYear());
        }
        return result;
    }

    private String label(Asset asset) {
        return asset.getRealName() != null ? asset.getRealName() : asset.getAnonymousName();
    }
}
