package com.retrobolsa.api.game.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, UUID> {

    List<AssetSnapshot> findByAssetIdAndYearBetweenOrderByYearAsc(UUID assetId, int startYear, int endYear);

    List<AssetSnapshot> findByAssetIdAndYearOrderByYearAsc(UUID assetId, int year);
}
