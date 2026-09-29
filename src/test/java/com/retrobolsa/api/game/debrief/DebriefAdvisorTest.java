package com.retrobolsa.api.game.debrief;

import com.retrobolsa.api.game.debrief.DebriefAdvisor.Input;
import com.retrobolsa.api.game.debrief.DebriefAdvisor.Position;
import com.retrobolsa.api.game.dto.TipDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DebriefAdvisorTest {

    private static final BigDecimal ORCAMENTO = new BigDecimal("100000");

    private final DebriefAdvisor advisor = new DebriefAdvisor();

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    private static Position acao(String nome, String investido, String finalValue) {
        return new Position(nome, "stock", n(investido), n(finalValue));
    }

    private static Position titulo(String nome, String investido, String finalValue) {
        return new Position(nome, "bond", n(investido), n(finalValue));
    }

    private List<TipDto> dicas(List<Position> posicoes, String retorno, String cdi, String ipca, String selic) {
        return advisor.advise(new Input(ORCAMENTO, posicoes, n(retorno),
                cdi == null ? null : n(cdi), ipca == null ? null : n(ipca), selic == null ? null : n(selic)));
    }

    private List<String> codigos(List<TipDto> dicas) {
        return dicas.stream().map(TipDto::getCode).toList();
    }

    private final List<Position> equilibrada = List.of(
            acao("Empresa A", "35000", "40000"), acao("Empresa B", "35000", "38000"), titulo("Título 1", "30000", "36000"));

    @Test
    void perdeuParaAInflacaoVemPrimeiroEApontaAAulaDeIpca() {
        List<TipDto> dicas = dicas(equilibrada, "3.5", "20", "10", "8");

        assertThat(dicas.get(0).getCode()).isEqualTo("PERDEU_PARA_INFLACAO");
        assertThat(dicas.get(0).getMessage())
                .isEqualTo("Sua carteira rendeu 3,5%, menos que a inflação do período (10,0%): no fim, o dinheiro comprava menos do que no começo.");
        assertThat(dicas.get(0).getArticleId()).isEqualTo("bbbbbbbb-0008-0000-0000-000000000008");
    }

    @Test
    void comparaComOCdiNosDoisSentidos() {
        assertThat(codigos(dicas(equilibrada, "25", "20", "10", "8"))).containsExactly("VENCEU_CDI");

        List<TipDto> perdeu = dicas(equilibrada, "15", "20", "10", "8");
        assertThat(codigos(perdeu)).containsExactly("PERDEU_PARA_CDI");
        assertThat(perdeu.get(0).getMessage()).startsWith("O CDI rendeu 20,0% no período");
        assertThat(perdeu.get(0).getArticleId()).isEqualTo("bbbbbbbb-0012-0000-0000-000000000012"); // risco e volatilidade
    }

    @Test
    void carteiraConcentradaDizQuantoEstavaNoAtivoEOQueEleFez() {
        List<TipDto> dicas = dicas(List.of(acao("Empresa A", "70000", "35000"), titulo("Título 1", "30000", "33000")),
                "-32", "20", "10", "8");

        TipDto concentrada = dicas.stream().filter(d -> d.getCode().equals("CONCENTRADA")).findFirst().orElseThrow();
        assertThat(concentrada.getMessage()).startsWith("70% do que você investiu estava em Empresa A, que caiu 50,0%");
        assertThat(concentrada.getModuleId()).isEqualTo("aaaaaaaa-0005-0000-0000-000000000005");
        assertThat(concentrada.getArticleId()).isEqualTo("bbbbbbbb-0013-0000-0000-000000000013"); // por que diversificar
    }

    @Test
    void tudoNumAtivoSo() {
        List<TipDto> dicas = dicas(List.of(acao("Empresa A", "100000", "150000")), "50", "20", "10", "8");

        assertThat(dicas).extracting(TipDto::getMessage)
                .anyMatch(m -> m.startsWith("Tudo o que você investiu estava em Empresa A, que subiu 50,0%"));
    }

    @Test
    void sessentaPorCentoCravadosNaoContaComoConcentrada() {
        List<TipDto> dicas = dicas(List.of(acao("Empresa A", "60000", "60000"), titulo("Título 1", "40000", "40000")),
                "0", null, null, null);

        assertThat(codigos(dicas)).doesNotContain("CONCENTRADA");
    }

    @Test
    void dinheiroParadoMostraQuantoTeriaRendidoNoCdi() {
        List<Position> parcial = List.of(
                acao("Empresa A", "30000", "33000"), acao("Empresa B", "30000", "33000"), titulo("Título 1", "30000", "33000"));

        TipDto parado = dicas(parcial, "9", "20", "5", "8").stream()
                .filter(d -> d.getCode().equals("DINHEIRO_PARADO")).findFirst().orElseThrow();

        assertThat(parado.getMessage()).isEqualTo("R$ 10.000,00 ficaram parados, rendendo 0%. No CDI, teriam virado R$ 12.000,00.");
        assertThat(parado.getArticleId()).isEqualTo("bbbbbbbb-0002-0000-0000-000000000002");
    }

    @Test
    void soAcoesComSelicAltaLembraDaRendaFixa() {
        List<Position> soAcoes = List.of(acao("Empresa A", "50000", "60000"), acao("Empresa B", "50000", "60000"));

        assertThat(dicas(soAcoes, "20", "15", "5", "16.5")).filteredOn(d -> d.getCode().equals("SO_ACOES_COM_SELIC_ALTA"))
                .singleElement().extracting(TipDto::getArticleId).isEqualTo("bbbbbbbb-0009-0000-0000-000000000009"); // Tesouro Direto
        assertThat(codigos(dicas(soAcoes, "20", "15", "5", "6.5"))).doesNotContain("SO_ACOES_COM_SELIC_ALTA");
        assertThat(codigos(dicas(equilibrada, "20", "15", "5", "16.5"))).doesNotContain("SO_ACOES_COM_SELIC_ALTA");
    }

    @Test
    void semDadosMacroSoSobramAsDicasDaPropriaCarteira() {
        List<TipDto> dicas = dicas(List.of(acao("Empresa A", "90000", "80000")), "-10", null, null, null);

        assertThat(codigos(dicas)).containsExactly("CONCENTRADA");
    }

    @Test
    void noMaximoQuatroDicas() {
        // Perdeu para a inflação e para o CDI, concentrou, deixou dinheiro parado e só tinha ações com Selic alta.
        List<TipDto> dicas = dicas(List.of(acao("Empresa A", "80000", "60000")), "-20", "20", "10", "16");

        assertThat(dicas).hasSize(4);
        assertThat(codigos(dicas)).containsExactly("PERDEU_PARA_INFLACAO", "PERDEU_PARA_CDI", "CONCENTRADA", "DINHEIRO_PARADO");
    }
}
