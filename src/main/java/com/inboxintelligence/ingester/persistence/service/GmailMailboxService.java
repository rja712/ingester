package com.inboxintelligence.ingester.persistence.service;

import com.inboxintelligence.ingester.model.GmailMailbox;
import com.inboxintelligence.ingester.persistence.repository.GmailMailboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailMailboxService {

    private final GmailMailboxRepository repository;

    public void save(GmailMailbox gmailMailbox) {
        repository.save(gmailMailbox);
    }

    public Optional<GmailMailbox> findByEmailAddress(String email) {
        return repository.findByEmailAddress(email);
    }
}
