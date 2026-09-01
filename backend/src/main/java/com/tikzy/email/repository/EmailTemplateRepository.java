package com.tikzy.email.repository;

import com.tikzy.email.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    @Query("SELECT t FROM EmailTemplate t WHERE t.code = :code AND t.isActive = true")
    Optional<EmailTemplate> findActiveByCode(@Param("code") String code);
}
