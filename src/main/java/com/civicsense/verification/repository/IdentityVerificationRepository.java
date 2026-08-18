package com.civicsense.verification.repository;

import com.civicsense.verification.entity.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentityVerificationRepository
        extends JpaRepository<IdentityVerification, UUID> {

    Optional<IdentityVerification> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}