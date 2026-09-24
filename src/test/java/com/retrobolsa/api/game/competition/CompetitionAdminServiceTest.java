package com.retrobolsa.api.game.competition;

import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.dto.AdminAssetDto;
import com.retrobolsa.api.game.dto.CreateCompetitionRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompetitionAdminService — Criação de rodadas pelo admin")
class CompetitionAdminServiceTest {

    @Mock private CompetitionRepository competitionRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetSnapshotRepository snapshotRepository;

    @InjectMocks private CompetitionAdminService service;

    private final Asset petrobras = asset("Empresa A", "Petrobras S.A.", "stock");
    private final Asset vale = asset("Empresa B", "Vale S.A.", "stock");
    private final Asset selic = asset("Título 1", "Tesouro Selic", "bond");

    @Test
    void criaRodadaQuandoTodosOsAtivosTemRetornoNoPeriodo() {
        comAnos(petrobras, 2014, 2015, 2016);
        comAnos(vale, 2014, 2015, 2016);
        when(assetRepository.findAllById(any())).thenReturn(List.of(petrobras, vale));
        when(competitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Competition created = service.create(request(2014, 2017, petrobras, vale));

        assertThat(created.getStatus()).isEqualTo("draft");
        assertThat(created.getAssets()).containsExactly(petrobras, vale);
        assertThat(created.getStartYear()).isEqualTo(2014);
        assertThat(created.getEndYear()).isEqualTo(2017);
    }

    @Test
    void recusaAtivoSemRetornoEmAlgumAnoDoPeriodo() {
        comAnos(petrobras, 2014, 2015, 2016);
        comAnos(vale, 2014);
        when(assetRepository.findAllById(any())).thenReturn(List.of(petrobras, vale));

        assertThatThrownBy(() -> service.create(request(2014, 2017, petrobras, vale)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vale S.A. (faltam 2015, 2016)")
                .hasMessageNotContaining("Petrobras");
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void naoExigeRetornoNoAnoFinalPorqueASimulacaoParaAntes() {
        comAnos(petrobras, 2014, 2015, 2016);
        when(assetRepository.findAllById(any())).thenReturn(List.of(petrobras));
        when(competitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(request(2014, 2017, petrobras))).isNotNull();
    }

    @Test
    void recusaDoisAtivosComOMesmoNomeAnonimo() {
        Asset outraEmpresaA = asset("Empresa A", "Itaú Unibanco S.A.", "stock");
        when(assetRepository.findAllById(any())).thenReturn(List.of(petrobras, outraEmpresaA));

        assertThatThrownBy(() -> service.create(request(2014, 2017, petrobras, outraEmpresaA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Empresa A")
                .hasMessageContaining("Petrobras S.A.")
                .hasMessageContaining("Itaú Unibanco S.A.");
    }

    @Test
    void recusaNumeroDeRodadaJaUsado() {
        when(competitionRepository.existsByRoundNumber(2)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(2014, 2017, petrobras)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numero 2");
    }

    @Test
    void recusaAnoFinalQueNaoVemDepoisDoInicial() {
        assertThatThrownBy(() -> service.create(request(2016, 2016, petrobras)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ano final");
    }

    @Test
    void listaAtivosComOsAnosDisponiveisAcoesAntesDosTitulos() {
        comAnos(selic, 2014, 2015);
        comAnos(petrobras, 2014, 2015, 2016);
        when(assetRepository.findAll()).thenReturn(List.of(selic, petrobras));

        List<AdminAssetDto> assets = service.listAssets();

        assertThat(assets).extracting(AdminAssetDto::getRealName)
                .containsExactly("Tesouro Selic", "Petrobras S.A.");
        assertThat(assets.get(1).getYears()).containsExactly(2014, 2015, 2016);
    }

    private final List<AssetSnapshotRepository.AssetYear> anos = new ArrayList<>();

    private void comAnos(Asset asset, int... years) {
        for (int year : years) {
            anos.add(new AssetSnapshotRepository.AssetYear() {
                public UUID getAssetId() { return asset.getId(); }
                public int getYear() { return year; }
            });
        }
        when(snapshotRepository.findYearsWithReturn()).thenReturn(anos);
    }

    private static Asset asset(String anonymousName, String realName, String type) {
        return Asset.builder()
                .id(UUID.randomUUID())
                .anonymousName(anonymousName)
                .realName(realName)
                .type(type)
                .build();
    }

    private static CreateCompetitionRequestDto request(int startYear, int endYear, Asset... assets) {
        CreateCompetitionRequestDto request = new CreateCompetitionRequestDto();
        request.setRoundNumber(2);
        request.setBudget(new BigDecimal("100000.00"));
        request.setScenarioTitle("Cenário de teste");
        request.setStartYear(startYear);
        request.setEndYear(endYear);
        request.setEndsAt(LocalDateTime.now().plusDays(7));
        request.setAssetIds(java.util.Arrays.stream(assets).map(Asset::getId).toList());
        return request;
    }
}
