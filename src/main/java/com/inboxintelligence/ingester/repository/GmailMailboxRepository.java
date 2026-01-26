package com.inboxintelligence.ingester.repository;

import com.inboxintelligence.ingester.model.GmailMailbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GmailMailboxRepository extends JpaRepository<GmailMailbox, Long> {

    Optional<GmailMailbox> findByEmailAddress(String emailAddress);

}