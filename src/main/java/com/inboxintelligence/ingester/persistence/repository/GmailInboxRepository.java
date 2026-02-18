package com.inboxintelligence.ingester.persistence.repository;

import com.inboxintelligence.ingester.model.GmailInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailInboxRepository extends JpaRepository<GmailInbox, Long> {


}