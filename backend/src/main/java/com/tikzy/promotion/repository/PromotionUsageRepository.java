package com.tikzy.promotion.repository;

import com.tikzy.promotion.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {

    Optional<PromotionUsage> findByOrderId(UUID orderId);

    List<PromotionUsage> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
