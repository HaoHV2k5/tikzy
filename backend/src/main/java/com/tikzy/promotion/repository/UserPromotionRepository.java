package com.tikzy.promotion.repository;

import com.tikzy.promotion.entity.UserPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, UUID> {

    List<UserPromotion> findAllByUserId(UUID userId);

    Optional<UserPromotion> findByUserIdAndPromotionId(UUID userId, UUID promotionId);
}
