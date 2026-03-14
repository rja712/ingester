package com.inboxintelligence.ingester.persistence.service;

import com.inboxintelligence.ingester.model.entity.EmailAttachment;
import com.inboxintelligence.ingester.persistence.repository.EmailAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Persistence service for email attachment entities.
 */
@Service
@RequiredArgsConstructor
public class EmailAttachmentService {

    private final EmailAttachmentRepository repository;

    public EmailAttachment save(EmailAttachment attachment) {
        return repository.save(attachment);
    }
}
