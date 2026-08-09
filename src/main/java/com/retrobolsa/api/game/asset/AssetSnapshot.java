package com.retrobolsa.api.game.asset;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "asset_snapshots", uniqueConstraints = @UniqueConstraint(columnNames = {"asset_id", "year"}))
public class AssetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private int year;

    @Column
    private BigDecimal pl;

    @Column
    private BigDecimal roe;

    @Column(name = "dividend_yield")
    private BigDecimal dividendYield;

    @Column
    private BigDecimal rate;

    @Column(name = "annual_return")
    private BigDecimal annualReturn;

    @Column
    private BigDecimal lvp;

    @Column(name = "lucro_positivo")
    private Boolean lucroPositivo;

    @Column(name = "cagr_lucro")
    private BigDecimal cagrLucro;

    @Column(name = "cagr_receita")
    private BigDecimal cagrReceita;

    @Column(name = "margem_ebitda")
    private BigDecimal margemEbitda;
}
