package com.tikzy.event.repository;

import com.tikzy.event.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    List<TicketType> findAllByEventIdAndIsActiveTrue(UUID eventId);

    /**
     * Lớp chặn overselling thứ 2 (sau Redis Lock):
     * DB update có điều kiện — chỉ tăng sold_quantity khi còn đủ kho.
     * Trả về số row được update (0 = hết vé / không đủ số lượng).
     */
    @Modifying
    @Query("UPDATE TicketType tt SET tt.soldQuantity = tt.soldQuantity + :quantity " +
            "WHERE tt.id = :id AND tt.soldQuantity + :quantity <= tt.totalQuantity")
    int increaseSoldQuantityIfAvailable(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE TicketType tt SET tt.soldQuantity = tt.soldQuantity - :quantity " +
            "WHERE tt.id = :id AND tt.soldQuantity >= :quantity")
    int decreaseSoldQuantity(@Param("id") UUID id, @Param("quantity") int quantity);
}
