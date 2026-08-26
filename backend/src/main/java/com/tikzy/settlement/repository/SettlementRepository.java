package com.tikzy.settlement.repository;

import com.tikzy.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Optional<Settlement> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}
