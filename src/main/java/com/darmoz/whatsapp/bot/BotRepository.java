package com.darmoz.whatsapp.bot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BotRepository extends JpaRepository<Bot, UUID> {

    List<Bot> findByApplicationIdOrderByNameAsc(UUID applicationId);

    List<Bot> findAllByOrderByNameAsc();
}
