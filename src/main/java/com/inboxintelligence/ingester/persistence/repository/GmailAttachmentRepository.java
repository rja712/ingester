package com.inboxintelligence.ingester.persistence.repository;

import com.inboxintelligence.ingester.model.GmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailAttachmentRepository extends JpaRepository<GmailAttachment, Long> {

}