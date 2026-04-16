package com.inboxintelligence.ingester.inbound;

import com.inboxintelligence.ingester.outbound.EmailEventPublisher;
import com.inboxintelligence.persistence.service.EmailContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO: Delete this controller — temporary dev utility for replaying emails to RabbitMQ.
 */
@Slf4j
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

    private final EmailContentService emailContentService;
    private final EmailEventPublisher emailEventPublisher;

    @PostMapping("/republish-all")
    public ResponseEntity<String> republishAll() {

        var emails = emailContentService.findAll();
        log.info("Republishing {} emails to RabbitMQ", emails.size());

        for (var email : emails) {
            emailEventPublisher.publishEmailProcessed(email);
        }

        return ResponseEntity.ok("Republished " + emails.size() + " emails to RabbitMQ");
    }
}
