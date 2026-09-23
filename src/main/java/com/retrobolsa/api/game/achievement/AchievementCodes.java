package com.retrobolsa.api.game.achievement;

import java.util.Set;

/**
 * Códigos das conquistas. Precisam bater com o catálogo semeado em
 * {@code V9__create_achievements.sql} — {@code AchievementCatalogIntegrationTest}
 * garante que os dois lados não se desencontrem.
 */
public final class AchievementCodes {

    private AchievementCodes() {
    }

    public static final String PRIMEIRA_CARTEIRA = "PRIMEIRA_CARTEIRA";
    public static final String TUDO_INVESTIDO = "TUDO_INVESTIDO";
    public static final String PRIMEIRA_AULA = "PRIMEIRA_AULA";
    public static final String NO_AZUL = "NO_AZUL";
    public static final String EQUILIBRISTA = "EQUILIBRISTA";
    public static final String DIVERSIFICADOR = "DIVERSIFICADOR";
    public static final String DOIS_DIGITOS = "DOIS_DIGITOS";
    public static final String VETERANO = "VETERANO";
    public static final String PODIO = "PODIO";
    public static final String MODULO_COMPLETO = "MODULO_COMPLETO";
    public static final String CAMPEAO_RODADA = "CAMPEAO_RODADA";
    public static final String FORMADO = "FORMADO";

    /**
     * Conquistas que nascem de carteiras e rodadas. O reset do jogo apaga carteiras
     * e zera pontos, então apaga também estas — mas não as de aulas, porque o
     * progresso das aulas sobrevive ao reset.
     */
    public static final Set<String> GAME = Set.of(
            PRIMEIRA_CARTEIRA, TUDO_INVESTIDO, NO_AZUL, EQUILIBRISTA, DIVERSIFICADOR,
            DOIS_DIGITOS, VETERANO, PODIO, CAMPEAO_RODADA);
}
