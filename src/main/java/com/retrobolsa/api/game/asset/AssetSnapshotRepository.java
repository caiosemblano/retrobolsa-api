package com.retrobolsa.api.game.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, UUID> {

    List<AssetSnapshot> findByAssetIdAndYearBetweenOrderByYearAsc(UUID assetId, int startYear, int endYear);

    List<AssetSnapshot> findByAssetIdAndYearOrderByYearAsc(UUID assetId, int year);

    /** Pares (ativo, ano) que têm retorno anual — é o que a simulação consegue calcular. */
    @Query("select s.asset.id as assetId, s.year as year from AssetSnapshot s "
            + "where s.annualReturn is not null order by s.year")
    List<AssetYear> findYearsWithReturn();

    interface AssetYear {
        UUID getAssetId();
        int getYear();
    }
}
