package com.retrobolsa.api.game.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistoricalQuoteRepository extends JpaRepository<HistoricalQuote, UUID> {

    Optional<HistoricalQuote> findFirstByAssetIdAndDateGreaterThanEqualOrderByDateAsc(UUID assetId, LocalDate date);

    Optional<HistoricalQuote> findFirstByAssetIdAndDateLessThanEqualOrderByDateDesc(UUID assetId, LocalDate date);

    List<HistoricalQuote> findAllByAssetIdAndDateBetweenOrderByDateAsc(UUID assetId, LocalDate start, LocalDate end);

    List<HistoricalQuote> findAllByAssetIdInAndDateBetweenOrderByAssetIdAscDateAsc(
            Collection<UUID> assetIds, LocalDate start, LocalDate end);
}
