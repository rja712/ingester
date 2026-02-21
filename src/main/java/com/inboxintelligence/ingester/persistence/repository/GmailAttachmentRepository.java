package com.inboxintelligence.ingester.persistence.repository;

import com.inboxintelligence.ingester.model.entity.GmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailAttachmentRepository extends JpaRepository<GmailAttachment, Long> {

}