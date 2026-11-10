package com.darmoz.whatsapp.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppIntegrationRepository extends JpaRepository<AppIntegration, UUID> {

    Optional<AppIntegration> findByApplicationId(UUID applicationId);

    boolean existsByApplicationId(UUID applicationId);
}
