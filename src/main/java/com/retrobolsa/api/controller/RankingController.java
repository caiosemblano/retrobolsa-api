package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.GlobalRankingResponseDto;
import com.retrobolsa.api.game.dto.RankingResponseDto;
import com.retrobolsa.api.game.dto.SeasonInfoDto;
import com.retrobolsa.api.game.dto.UserRankSummaryDto;
import com.retrobolsa.api.game.classroom.ClassroomService;
import com.retrobolsa.api.game.ranking.RankingService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Controlador REST para rankings.
 *
 * <ul>
 *   <li>{@code GET /api/rankings?type=global}     — ranking global paginado</li>
 *   <li>{@code GET /api/rankings?type=quinzenal}  — ranking da rodada ativa/mais recente</li>
 *   <li>{@code GET /api/rankings?type=season}     — ranking da temporada atual</li>
 *   <li>{@code GET /api/rankings?competitionId=X} — ranking de rodada específica por ID</li>
 *   <li>{@code GET /api/rankings?roundNumber=N}   — ranking de rodada específica por número</li>
 *   <li>{@code GET /api/rankings/global}          — ranking global dedicado com paginação</li>
 *   <li>{@code GET /api/rankings/season/current}  — número e faixa de rodadas da temporada atual</li>
 *   <li>{@code GET /api/rankings/me}              — posição do usuário autenticado</li>
 * </ul>
 *
 * <p>{@code turma={id}} filtra os rankings da rodada e da temporada pelos alunos de uma
 * turma. Só o professor dela e os próprios alunos podem usar.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;
    private final ClassroomService classroomService;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // GET /api/rankings (rota principal com type e filtros de rodada)
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<?> getRankings(
            @RequestParam(required = false) UUID competitionId,
            @RequestParam(required = false) Integer roundNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) UUID turma,
            Authentication authentication) {

        Set<UUID> alunos = turma == null ? null : alunosDaTurma(turma, authentication);

        if ("global".equalsIgnoreCase(type) || "general".equalsIgnoreCase(type)) {
            if (alunos != null) {
                throw new IllegalArgumentException("O ranking da turma é por rodada ou por temporada.");
            }
            return ResponseEntity.ok(rankingService.getGlobalRanking(limit, page));
        }

        if ("quinzenal".equalsIgnoreCase(type) || "active".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(rankingService.getQuinzenalRanking(alunos));
        }

        if ("season".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(rankingService.getSeasonRanking(limit, page, alunos));
        }

        // Sem type → ranking de rodada específica (requer competitionId ou roundNumber)
        List<RankingResponseDto> ranking = rankingService.getRanking(competitionId, roundNumber, alunos);
        return ResponseEntity.ok(ranking);
    }

    private Set<UUID> alunosDaTurma(UUID turma, Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Entre na sua conta para ver o ranking da turma.");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Entre na sua conta para ver o ranking da turma."));
        if (!classroomService.canSeeRanking(user.getId(), turma)) {
            throw new AccessDeniedException("Só o professor e os alunos da turma veem o ranking dela.");
        }
        return classroomService.memberIds(turma);
    }

    // -------------------------------------------------------------------------
    // GET /api/rankings/global (rota dedicada com paginação)
    // -------------------------------------------------------------------------

    @GetMapping("/global")
    public ResponseEntity<List<GlobalRankingResponseDto>> getGlobalRanking(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page) {
        return ResponseEntity.ok(rankingService.getGlobalRanking(limit, page));
    }

    // -------------------------------------------------------------------------
    // GET /api/rankings/season/current (limites da temporada atual)
    // -------------------------------------------------------------------------

    @GetMapping("/season/current")
    public ResponseEntity<SeasonInfoDto> getCurrentSeason() {
        return ResponseEntity.ok(rankingService.getCurrentSeasonInfo());
    }

    // -------------------------------------------------------------------------
    // GET /api/rankings/me (posição do usuário autenticado)
    // -------------------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<UserRankSummaryDto> getMyRank(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserRankSummaryDto summary = rankingService.getUserRankSummary(userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }
}
