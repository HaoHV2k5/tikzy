package com.tikzy.advertisement.repository;

import com.tikzy.advertisement.entity.Banner;
import com.tikzy.advertisement.enums.BannerPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    /**
     * Lấy banner đang active trong khoảng thời gian hiển thị, theo vị trí,
     * sắp xếp theo thứ tự ưu tiên (Hero Slider trang chủ...).
     */
    @Query("SELECT b FROM Banner b WHERE b.position = :position AND b.isActive = true " +
            "AND (b.startDate IS NULL OR b.startDate <= :now) " +
            "AND (b.endDate IS NULL OR b.endDate >= :now) " +
            "ORDER BY b.sortOrder ASC")
    List<Banner> findActiveBanners(@Param("position") BannerPosition position,
                                   @Param("now") LocalDateTime now);
}
