package com.retrobolsa.api.game.competition;

import com.retrobolsa.api.game.portfolio.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CompetitionLifecycleScheduler {
    private final CompetitionRepository competitionRepository;
    private final PortfolioService portfolioService;

    @Scheduled(fixedDelayString = "${retrobolsa.lifecycle.interval-ms:60000}")
    @Transactional
    public void processExpiredCompetitions() {
        for (Competition competition : competitionRepository
                .findAllByStatusAndEndsAtLessThanEqual("open", LocalDateTime.now())) {
            competition.setStatus("closed");
            competitionRepository.save(competition);
            portfolioService.simulateCompetition(competition);
        }
    }
}
