package com.retrobolsa.api.game.macro;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Indicadores macroeconômicos do Brasil num ano (Banco Central e B3).
 * Taxas em % no ano; dólar em R$ no fim do ano. Veja V14__create_macro_indicators.sql.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "macro_indicators")
public class MacroIndicator {

    @Id
    private Integer year;

    /** Selic acumulada no ano. */
    @Column(nullable = false)
    private BigDecimal selic;

    /** Meta da Selic no fim do ano (% a.a.); nula antes de 1999, quando não havia meta. */
    @Column(name = "selic_meta")
    private BigDecimal selicMeta;

    @Column(nullable = false)
    private BigDecimal cdi;

    @Column(nullable = false)
    private BigDecimal ipca;

    @Column(nullable = false)
    private BigDecimal poupanca;

    @Column(nullable = false)
    private BigDecimal ibovespa;

    @Column(nullable = false)
    private BigDecimal dolar;

    @Column(nullable = false)
    private BigDecimal pib;
}
