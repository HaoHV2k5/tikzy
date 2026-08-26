package com.tikzy.order.repository;

import com.tikzy.order.entity.Order;
import com.tikzy.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderCode(String orderCode);

    Page<Order> findAllByUserId(UUID userId, Pageable pageable);

    List<Order> findAllByEventIdAndStatus(UUID eventId, OrderStatus status);

    /**
     * OrderExpirationScheduler: tìm các đơn PENDING đã hết 15 phút giữ chỗ
     * để hủy và trả vé về kho.
     */
    List<Order> findAllByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime now);

    boolean existsByOrderCode(String orderCode);
}
