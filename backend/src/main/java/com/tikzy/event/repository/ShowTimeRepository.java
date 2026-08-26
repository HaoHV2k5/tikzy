package com.tikzy.event.repository;

import com.tikzy.event.entity.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShowTimeRepository extends JpaRepository<ShowTime, UUID> {

    List<ShowTime> findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(UUID eventId);
}
