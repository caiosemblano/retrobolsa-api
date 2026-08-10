package com.retrobolsa.api.game.simulation;

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

    public SimulationResult calculate(BigDecimal budget, List<AllocationInput> inputs, int startYear, int endYear) {
        int years = endYear - startYear;
        List<PortfolioResultDto.ChartPoint> chartData = new ArrayList<>();

        chartData.add(PortfolioResultDto.ChartPoint.builder()
                .year(startYear)
                .value(budget)
                .build());

        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (AllocationInput input : inputs) {
            totalAllocated = totalAllocated.add(input.amountInvested);
        }
        BigDecimal cashReserve = budget.subtract(totalAllocated);

        // Armazenamos a quantidade de "cotas" que conseguimos comprar no startYear
        BigDecimal[] initialShares = new BigDecimal[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            AllocationInput input = inputs.get(i);
            HistoricalQuote startQuote = findQuoteForYearEnd(input.quotes, startYear - 1); // pegamos fim do ano anterior ou começo desse ano
            if (startQuote == null) {
                startQuote = input.quotes.get(0); // fallback pro primeiro preço
            }
            BigDecimal startPrice = BigDecimal.valueOf(startQuote.getClosePrice());
            if (startPrice.compareTo(BigDecimal.ZERO) == 0) {
                initialShares[i] = BigDecimal.ZERO;
            } else {
                initialShares[i] = input.amountInvested.divide(startPrice, MC);
            }
        }

        BigDecimal[] currentValues = new BigDecimal[inputs.size()];

        for (int y = startYear; y < endYear; y++) {
            BigDecimal yearTotal = cashReserve;

            for (int i = 0; i < inputs.size(); i++) {
                AllocationInput input = inputs.get(i);
                HistoricalQuote yearQuote = findQuoteForYearEnd(input.quotes, y);
                if (yearQuote == null) {
                    yearQuote = input.quotes.get(input.quotes.size() - 1); // fallback
                }
                
                BigDecimal currentPrice = BigDecimal.valueOf(yearQuote.getClosePrice());
                currentValues[i] = initialShares[i].multiply(currentPrice, MC);
                yearTotal = yearTotal.add(currentValues[i]);
            }

            chartData.add(PortfolioResultDto.ChartPoint.builder()
                    .year(y + 1)
                    .value(yearTotal.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        BigDecimal finalValue = chartData.get(chartData.size() - 1).getValue();
        BigDecimal totalGrowth = finalValue.subtract(budget).divide(budget, MC);
        BigDecimal totalReturn = totalGrowth.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        
        // Para calcular CAGR usamos (Final/Initial)
        BigDecimal ratio = finalValue.divide(budget, MC);
        BigDecimal annualReturn = calculateAnnualReturn(ratio, years).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

        List<AssetFinalValue> assetFinalValues = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            assetFinalValues.add(new AssetFinalValue(
                inputs.get(i).assetId,
                inputs.get(i).amountInvested,
                currentValues[i] != null ? currentValues[i].setScale(2, RoundingMode.HALF_UP) : inputs.get(i).amountInvested
            ));
        }

        return new SimulationResult(
            finalValue.setScale(2, RoundingMode.HALF_UP),
            totalReturn,
            annualReturn,
            chartData,
            assetFinalValues
        );
    }

    private HistoricalQuote findQuoteForYearEnd(List<HistoricalQuote> quotes, int year) {
        HistoricalQuote result = null;
        for (HistoricalQuote q : quotes) {
            if (q.getDate().getYear() <= year) {
                if (result == null || q.getDate().isAfter(result.getDate())) {
                    result = q;
                }
            }
        }
        return result;
    }

    private BigDecimal calculateAnnualReturn(BigDecimal ratio, int years) {
        if (years <= 0 || ratio.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        double tr = ratio.doubleValue();
        double annualized = Math.pow(tr, 1.0 / years) - 1.0;
        return BigDecimal.valueOf(annualized);
    }

    public record AllocationInput(UUID assetId, BigDecimal amountInvested, List<HistoricalQuote> quotes) {}
    public record AssetFinalValue(UUID assetId, BigDecimal amountInvested, BigDecimal finalValue) {}
    public record SimulationResult(
        BigDecimal finalValue,
        BigDecimal totalReturn,
        BigDecimal annualReturn,
        List<PortfolioResultDto.ChartPoint> chartData,
        List<AssetFinalValue> assetFinalValues
    ) {}
}
