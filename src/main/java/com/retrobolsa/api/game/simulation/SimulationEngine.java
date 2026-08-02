package com.retrobolsa.api.game.simulation;

import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.dto.PortfolioResultDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SimulationEngine {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    public SimulationResult calculate(BigDecimal budget, List<AllocationInput> inputs, int startYear, int endYear) {
        int years = endYear - startYear;
        List<PortfolioResultDto.ChartPoint> chartData = new ArrayList<>();

        chartData.add(PortfolioResultDto.ChartPoint.builder()
                .year(startYear)
                .value(budget)
                .build());

        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal[] currentValues = new BigDecimal[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            currentValues[i] = inputs.get(i).amountInvested;
            totalAllocated = totalAllocated.add(inputs.get(i).amountInvested);
        }

        BigDecimal cashReserve = budget.subtract(totalAllocated);

        for (int y = startYear; y < endYear; y++) {
            BigDecimal yearTotal = cashReserve;

            for (int i = 0; i < inputs.size(); i++) {
                AllocationInput input = inputs.get(i);
                AssetSnapshot snapshot = findSnapshotForYear(input.snapshots, y);

                if (snapshot != null && snapshot.getAnnualReturn() != null) {
                    BigDecimal returnFactor = BigDecimal.ONE.add(snapshot.getAnnualReturn());
                    currentValues[i] = currentValues[i].multiply(returnFactor, MC);
                }

                yearTotal = yearTotal.add(currentValues[i]);
            }

            chartData.add(PortfolioResultDto.ChartPoint.builder()
                    .year(y + 1)
                    .value(yearTotal.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        BigDecimal finalValue = chartData.get(chartData.size() - 1).getValue();
        BigDecimal totalReturn = finalValue.divide(budget, MC);
        BigDecimal annualReturn = calculateAnnualReturn(totalReturn, years);

        List<AssetFinalValue> assetFinalValues = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            assetFinalValues.add(new AssetFinalValue(
                inputs.get(i).assetId,
                inputs.get(i).amountInvested,
                currentValues[i].setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return new SimulationResult(
            finalValue.setScale(2, RoundingMode.HALF_UP),
            totalReturn.setScale(4, RoundingMode.HALF_UP),
            annualReturn,
            chartData,
            assetFinalValues
        );
    }

    private AssetSnapshot findSnapshotForYear(List<AssetSnapshot> snapshots, int year) {
        for (AssetSnapshot s : snapshots) {
            if (s.getYear() == year) return s;
        }
        return null;
    }

    private BigDecimal calculateAnnualReturn(BigDecimal totalReturn, int years) {
        if (years <= 0 || totalReturn.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        double tr = totalReturn.doubleValue();
        double annualized = Math.pow(tr, 1.0 / years) - 1.0;
        return BigDecimal.valueOf(annualized).setScale(4, RoundingMode.HALF_UP);
    }

    public record AllocationInput(UUID assetId, BigDecimal amountInvested, List<AssetSnapshot> snapshots) {}
    public record AssetFinalValue(UUID assetId, BigDecimal amountInvested, BigDecimal finalValue) {}
    public record SimulationResult(
        BigDecimal finalValue,
        BigDecimal totalReturn,
        BigDecimal annualReturn,
        List<PortfolioResultDto.ChartPoint> chartData,
        List<AssetFinalValue> assetFinalValues
    ) {}
}
