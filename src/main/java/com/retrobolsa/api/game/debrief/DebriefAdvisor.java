package com.retrobolsa.api.game.debrief;

import com.retrobolsa.api.game.dto.TipDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tira da carteira do jogador as lições da rodada: comparou com a inflação e o
 * CDI, concentrou demais, deixou dinheiro parado, ignorou a renda fixa com juros altos.
 * Regras puras, sem banco: recebe os números e devolve as dicas em ordem de importância.
 */
@Component
public class DebriefAdvisor {

    static final int MAX_TIPS = 4;
    /** Acima desta fatia do investido num ativo só, a carteira é considerada concentrada. */
    static final BigDecimal CONCENTRATION = new BigDecimal("0.60");
    /** Selic (% a.a.) a partir da qual a renda fixa "pagava bem" no início da rodada. */
    static final BigDecimal HIGH_SELIC = BigDecimal.TEN;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    // Aulas do seed (V3/V10 e V17), cada dica apontando para o assunto dela.
    private static final String MOD_MATEMATICA = "aaaaaaaa-0001-0000-0000-000000000001";
    private static final String MOD_MACRO = "aaaaaaaa-0003-0000-0000-000000000003";
    private static final String MOD_RENDA_FIXA = "aaaaaaaa-0004-0000-0000-000000000004";
    private static final String MOD_RISCO = "aaaaaaaa-0005-0000-0000-000000000005";
    private static final String AULA_JUROS_COMPOSTOS = "bbbbbbbb-0002-0000-0000-000000000002";
    private static final String AULA_IPCA = "bbbbbbbb-0008-0000-0000-000000000008";
    private static final String AULA_TESOURO = "bbbbbbbb-0009-0000-0000-000000000009";
    private static final String AULA_RISCO = "bbbbbbbb-0012-0000-0000-000000000012";
    private static final String AULA_DIVERSIFICAR = "bbbbbbbb-0013-0000-0000-000000000013";

    public record Position(String anonymousName, String type, BigDecimal amountInvested, BigDecimal finalValue) {}

    /**
     * @param totalReturn  rentabilidade da carteira no período, em %
     * @param cdiReturn    CDI acumulado no período, em %; nulo se não há dados
     * @param ipcaReturn   inflação acumulada no período, em %; nula se não há dados
     * @param selicAtStart Selic (% a.a.) no ano anterior ao início; nula se não há dados
     */
    public record Input(BigDecimal budget, List<Position> positions, BigDecimal totalReturn,
                        BigDecimal cdiReturn, BigDecimal ipcaReturn, BigDecimal selicAtStart) {}

    public List<TipDto> advise(Input in) {
        List<TipDto> tips = new ArrayList<>();
        BigDecimal invested = in.positions().stream().map(Position::amountInvested).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (in.ipcaReturn() != null && in.totalReturn().compareTo(in.ipcaReturn()) < 0) {
            tips.add(tip("PERDEU_PARA_INFLACAO",
                    "Sua carteira rendeu " + pct(in.totalReturn()) + ", menos que a inflação do período ("
                            + pct(in.ipcaReturn()) + "): no fim, o dinheiro comprava menos do que no começo.",
                    MOD_MACRO, AULA_IPCA));
        }

        if (in.cdiReturn() != null) {
            if (in.totalReturn().compareTo(in.cdiReturn()) >= 0) {
                tips.add(tip("VENCEU_CDI",
                        "Sua carteira rendeu " + pct(in.totalReturn()) + ", acima do CDI (" + pct(in.cdiReturn())
                                + "), que é o que a renda fixa mais simples pagava no período.",
                        null, null));
            } else {
                tips.add(tip("PERDEU_PARA_CDI",
                        "O CDI rendeu " + pct(in.cdiReturn()) + " no período, sem o sobe e desce da bolsa, e sua carteira rendeu "
                                + pct(in.totalReturn()) + ". Quando o risco não traz mais retorno que o CDI, ele não compensou.",
                        MOD_RISCO, AULA_RISCO));
            }
        }

        if (invested.signum() > 0) {
            in.positions().stream()
                    .max(Comparator.comparing(Position::amountInvested))
                    .filter(maior -> maior.amountInvested().divide(invested, MC).compareTo(CONCENTRATION) > 0)
                    .ifPresent(maior -> tips.add(tip("CONCENTRADA", concentrationMessage(maior, invested),
                            MOD_RISCO, AULA_DIVERSIFICAR)));
        }

        BigDecimal idle = in.budget().subtract(invested);
        if (idle.signum() > 0 && invested.signum() > 0 && in.cdiReturn() != null) {
            BigDecimal inCdi = idle.multiply(BigDecimal.ONE.add(in.cdiReturn().divide(CEM, MC)), MC);
            tips.add(tip("DINHEIRO_PARADO",
                    reais(idle) + " ficaram parados, rendendo 0%. No CDI, teriam virado " + reais(inCdi) + ".",
                    MOD_MATEMATICA, AULA_JUROS_COMPOSTOS));
        }

        boolean hasBonds = in.positions().stream().anyMatch(p -> "bond".equals(p.type()));
        if (!hasBonds && !in.positions().isEmpty() && in.selicAtStart() != null
                && in.selicAtStart().compareTo(HIGH_SELIC) >= 0) {
            tips.add(tip("SO_ACOES_COM_SELIC_ALTA",
                    "A Selic estava em " + pct(in.selicAtStart()) + " ao ano no início, e você não tinha nenhum título:"
                            + " com juros altos, a renda fixa rende bem com pouco risco.",
                    MOD_RENDA_FIXA, AULA_TESOURO));
        }

        return tips.size() > MAX_TIPS ? tips.subList(0, MAX_TIPS) : tips;
    }

    private String concentrationMessage(Position maior, BigDecimal invested) {
        BigDecimal share = maior.amountInvested().divide(invested, MC).multiply(CEM);
        BigDecimal assetReturn = maior.finalValue().subtract(maior.amountInvested())
                .divide(maior.amountInvested(), MC).multiply(CEM);
        String movimento = assetReturn.signum() >= 0 ? "subiu " + pct(assetReturn) : "caiu " + pct(assetReturn.abs());
        String inicio = share.compareTo(CEM) >= 0
                ? "Tudo o que você investiu estava em " + maior.anonymousName()
                : pct(share, 0) + " do que você investiu estava em " + maior.anonymousName();
        return inicio + ", que " + movimento + ": o resultado dependeu quase só dele. Espalhar o dinheiro em"
                + " vários ativos diminui esse risco.";
    }

    private TipDto tip(String code, String message, String moduleId, String articleId) {
        return TipDto.builder().code(code).message(message).moduleId(moduleId).articleId(articleId).build();
    }

    private static String pct(BigDecimal valor) {
        return pct(valor, 1);
    }

    private static String pct(BigDecimal valor, int casas) {
        return String.format(PT_BR, "%." + casas + "f%%", valor.setScale(casas, RoundingMode.HALF_UP));
    }

    private static String reais(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor.setScale(2, RoundingMode.HALF_UP));
    }
}
