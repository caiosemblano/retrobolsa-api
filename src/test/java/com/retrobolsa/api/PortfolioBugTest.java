package com.retrobolsa.api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.retrobolsa.api.game.portfolio.PortfolioService;
import com.retrobolsa.api.game.dto.SubmitPortfolioRequestDto;
import java.util.*;
import java.math.BigDecimal;
@SpringBootTest
public class PortfolioBugTest {
    @Autowired PortfolioService service;
    @Test
    public void test() {
        System.out.println("Starting test...");
        try {
            SubmitPortfolioRequestDto req = new SubmitPortfolioRequestDto();
            req.setCompetitionId("dddddddd-0001-0000-0000-000000000001");
            SubmitPortfolioRequestDto.AllocationRequestDto alloc = new SubmitPortfolioRequestDto.AllocationRequestDto();
            alloc.setAssetId("cccccccc-0002-0000-0000-000000000002");
            alloc.setAmount(new BigDecimal("100000"));
            req.setAllocations(List.of(alloc));
            // Just pass the first user
            service.submit(UUID.fromString("11111111-1111-1111-1111-111111111111"), req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
