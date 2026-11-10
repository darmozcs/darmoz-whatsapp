package com.darmoz.whatsapp.flow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowRepository extends JpaRepository<Flow, UUID> {

    List<Flow> findByBotIdOrderByCreatedAtAsc(UUID botId);

    Optional<Flow> findByIdAndBotId(UUID id, UUID botId);

    boolean existsByIdAndBotId(UUID id, UUID botId);
}
