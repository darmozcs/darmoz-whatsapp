package com.darmoz.whatsapp.phonenumber;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, UUID> {

    @EntityGraph(attributePaths = "account")
    Optional<PhoneNumber> findByPhoneNumberId(String phoneNumberId);

    @EntityGraph(attributePaths = "account")
    Optional<PhoneNumber> findWithAccountById(UUID id);

    @EntityGraph(attributePaths = "account")
    List<PhoneNumber> findByAccountId(UUID accountId);

    @EntityGraph(attributePaths = "account")
    List<PhoneNumber> findByBotId(UUID botId);

    @EntityGraph(attributePaths = "account")
    List<PhoneNumber> findAllByOrderByCreatedAtAsc();

    @EntityGraph(attributePaths = "account")
    List<PhoneNumber> findByBotIdInAndActiveTrue(Collection<UUID> botIds);
}
