package com.retrobolsa.api.game.asset;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "historical_quotes", uniqueConstraints = @UniqueConstraint(columnNames = {"asset_id", "date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "close_price")
    private Double closePrice;
    
    @Column(name = "pe_ratio")
    private Double peRatio;
    
    @Column(name = "ev_ebitda")
    private Double evEbitda;
    
    private Double ebitda;
    
    @Column(name = "outstanding_shares")
    private Double outstandingShares;
    
    @Column(name = "debt_equity")
    private Double debtEquity;
    
    @Column(name = "net_margin")
    private Double netMargin;
    
    @Column(name = "ebitda_margin")
    private Double ebitdaMargin;
    
    private Double roa;

}
