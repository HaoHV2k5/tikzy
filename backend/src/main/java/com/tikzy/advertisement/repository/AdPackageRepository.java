package com.tikzy.advertisement.repository;

import com.tikzy.advertisement.entity.AdPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdPackageRepository extends JpaRepository<AdPackage, UUID> {

    Optional<AdPackage> findByCode(String code);
}
