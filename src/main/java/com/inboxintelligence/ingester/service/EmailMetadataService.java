package com.inboxintelligence.ingester.service;

import com.inboxintelligence.ingester.model.EmailMetadata;
import com.inboxintelligence.ingester.repository.EmailMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMetadataService {

    private final EmailMetadataRepository repository;

    public void save(EmailMetadata emailMetadata) {
        repository.save(emailMetadata);
    }
}
