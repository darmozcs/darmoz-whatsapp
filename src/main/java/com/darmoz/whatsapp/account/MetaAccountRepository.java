package com.darmoz.whatsapp.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MetaAccountRepository extends JpaRepository<MetaAccount, UUID> {
}
