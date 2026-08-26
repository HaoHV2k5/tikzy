package com.tikzy.advertisement.repository;

import com.tikzy.advertisement.entity.AdCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdCampaignRepository extends JpaRepository<AdCampaign, UUID> {

    Page<AdCampaign> findAllByOrganizerId(UUID organizerId, Pageable pageable);

    List<AdCampaign> findAllByEventId(UUID eventId);
}
