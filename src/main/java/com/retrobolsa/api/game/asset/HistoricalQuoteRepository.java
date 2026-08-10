package com.retrobolsa.api.game.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface HistoricalQuoteRepository extends JpaRepository<HistoricalQuote, UUID> {

    Optional<HistoricalQuote> findFirstByAssetIdAndDateGreaterThanEqualOrderByDateAsc(UUID assetId, LocalDate date);

    Optional<HistoricalQuote> findFirstByAssetIdAndDateLessThanEqualOrderByDateDesc(UUID assetId, LocalDate date);

    java.util.List<HistoricalQuote> findAllByAssetIdAndDateBetweenOrderByDateAsc(UUID assetId, LocalDate start, LocalDate end);
}
