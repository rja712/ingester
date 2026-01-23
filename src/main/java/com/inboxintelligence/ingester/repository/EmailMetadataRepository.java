package com.inboxintelligence.ingester.repository;

import com.inboxintelligence.ingester.model.EmailMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailMetadataRepository extends JpaRepository<EmailMetadata, Long> {
    Optional<EmailMetadata> findByEmailAddress(String emailAddress);
}