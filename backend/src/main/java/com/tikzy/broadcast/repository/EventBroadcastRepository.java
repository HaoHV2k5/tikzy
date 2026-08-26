package com.tikzy.broadcast.repository;

import com.tikzy.broadcast.entity.EventBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventBroadcastRepository extends JpaRepository<EventBroadcast, UUID> {

    List<EventBroadcast> findAllByEventIdOrderByCreatedAtDesc(UUID eventId);
}
