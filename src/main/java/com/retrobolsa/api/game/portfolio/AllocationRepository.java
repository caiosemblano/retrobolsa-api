package com.retrobolsa.api.game.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AllocationRepository extends JpaRepository<Allocation, UUID> {
}
