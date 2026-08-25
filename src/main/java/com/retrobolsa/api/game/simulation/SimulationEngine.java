package com.retrobolsa.api.game.simulation;

import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.HistoricalQuote;
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

    public SimulationResult calculate(
            BigDecimal budget, List<AllocationInput> inputs, int startYear, int endYear) {
        int years = endYear - startYear;
        BigDecimal[] shares = new BigDecimal[inputs.size()];
        BigDecimal[] values = new BigDecimal[inputs.size()];
        BigDecimal allocated = inputs.stream()
                .map(AllocationInput::amountInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (int i = 0; i < inputs.size(); i++) {
            HistoricalQuote quote = findQuoteForYearEnd(inputs.get(i).quotes(), startYear - 1);
            if (quote == null) quote = inputs.get(i).quotes().get(0);
            BigDecimal price = BigDecimal.valueOf(quote.getClosePrice());
            shares[i] = price.signum() == 0
                    ? BigDecimal.ZERO
                    : inputs.get(i).amountInvested().divide(price, MC);
        }

        List<PortfolioResultDto.ChartPoint> chart = new ArrayList<>();
        chart.add(point(startYear, budget));
        BigDecimal cash = budget.subtract(allocated);
        for (int year = startYear; year < endYear; year++) {
            BigDecimal total = cash;
            for (int i = 0; i < inputs.size(); i++) {
                HistoricalQuote quote = findQuoteForYearEnd(inputs.get(i).quotes(), year);
                if (quote == null) quote = inputs.get(i).quotes().get(inputs.get(i).quotes().size() - 1);
                values[i] = shares[i].multiply(BigDecimal.valueOf(quote.getClosePrice()), MC);
                total = total.add(values[i]);
            }
            chart.add(point(year + 1, total));
        }
        return result(budget, years, chart, finalValues(inputs, values));
    }

    public SimulationResult calculateSnapshots(
            BigDecimal budget, List<SnapshotAllocationInput> inputs, int startYear, int endYear) {
        int years = endYear - startYear;
        BigDecimal allocated = inputs.stream()
                .map(SnapshotAllocationInput::amountInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal[] values = inputs.stream()
                .map(SnapshotAllocationInput::amountInvested)
                .toArray(BigDecimal[]::new);

        List<PortfolioResultDto.ChartPoint> chart = new ArrayList<>();
        chart.add(point(startYear, budget));
        BigDecimal cash = budget.subtract(allocated);
        for (int year = startYear; year < endYear; year++) {
            BigDecimal total = cash;
            for (int i = 0; i < inputs.size(); i++) {
                AssetSnapshot snapshot = findSnapshotForYear(inputs.get(i).snapshots(), year);
                if (snapshot != null && snapshot.getAnnualReturn() != null) {
                    values[i] = values[i].multiply(
                            BigDecimal.ONE.add(snapshot.getAnnualReturn()), MC);
                }
                total = total.add(values[i]);
            }
            chart.add(point(year + 1, total));
        }

        List<AssetFinalValue> finalValues = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            finalValues.add(new AssetFinalValue(
                    inputs.get(i).assetId(), inputs.get(i).amountInvested(),
                    values[i].setScale(2, RoundingMode.HALF_UP)));
        }
        return result(budget, years, chart, finalValues);
    }

    private PortfolioResultDto.ChartPoint point(int year, BigDecimal value) {
        return PortfolioResultDto.ChartPoint.builder()
                .year(year).value(value.setScale(2, RoundingMode.HALF_UP)).build();
    }

    private SimulationResult result(
            BigDecimal budget, int years, List<PortfolioResultDto.ChartPoint> chart,
            List<AssetFinalValue> finalValues) {
        BigDecimal finalValue = chart.get(chart.size() - 1).getValue();
        BigDecimal totalReturn = finalValue.subtract(budget).divide(budget, MC)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal annualReturn = annualReturn(finalValue.divide(budget, MC), years)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return new SimulationResult(finalValue, totalReturn, annualReturn, chart, finalValues);
    }

    private List<AssetFinalValue> finalValues(
            List<AllocationInput> inputs, BigDecimal[] values) {
        List<AssetFinalValue> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            result.add(new AssetFinalValue(
                    inputs.get(i).assetId(), inputs.get(i).amountInvested(),
                    values[i].setScale(2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    private HistoricalQuote findQuoteForYearEnd(List<HistoricalQuote> quotes, int year) {
        HistoricalQuote result = null;
        for (HistoricalQuote quote : quotes) {
            if (quote.getDate().getYear() <= year
                    && (result == null || quote.getDate().isAfter(result.getDate()))) {
                result = quote;
            }
        }
        return result;
    }

    private AssetSnapshot findSnapshotForYear(List<AssetSnapshot> snapshots, int year) {
        return snapshots.stream()
                .filter(snapshot -> snapshot.getYear() == year)
                .findFirst().orElse(null);
    }

    private BigDecimal annualReturn(BigDecimal ratio, int years) {
        if (years <= 0 || ratio.signum() <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(Math.pow(ratio.doubleValue(), 1.0 / years) - 1.0);
    }

    public record AllocationInput(UUID assetId, BigDecimal amountInvested, List<HistoricalQuote> quotes) {}
    public record SnapshotAllocationInput(UUID assetId, BigDecimal amountInvested, List<AssetSnapshot> snapshots) {}
    public record AssetFinalValue(UUID assetId, BigDecimal amountInvested, BigDecimal finalValue) {}
    public record SimulationResult(
            BigDecimal finalValue, BigDecimal totalReturn, BigDecimal annualReturn,
            List<PortfolioResultDto.ChartPoint> chartData, List<AssetFinalValue> assetFinalValues) {}
}
