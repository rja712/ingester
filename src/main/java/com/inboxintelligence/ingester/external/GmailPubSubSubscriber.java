package com.inboxintelligence.ingester.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.inboxintelligence.ingester.common.GmailAPIProperties;
import com.inboxintelligence.ingester.model.GmailEvent;
import com.inboxintelligence.ingester.service.GmailMailboxService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class GmailPubSubSubscriber {

    private final ObjectMapper objectMapper;
    private final GmailAPIProperties gmailAPIProperties;
    private final GmailMailboxService gmailMailboxService;
    private final GmailSyncService gmailSyncService;

    private Subscriber subscriber;

    @PostConstruct
    public void start() {
        var projectSubscriptionName = ProjectSubscriptionName.of(gmailAPIProperties.projectId(), gmailAPIProperties.pubsubSubscriptionId());
        subscriber = Subscriber.newBuilder(projectSubscriptionName, this::handleMessage).build();
        subscriber.startAsync().awaitRunning();
        log.info("Gmail Pub/Sub subscriber started for subscription={}", projectSubscriptionName);
    }

    @PreDestroy
    public void stop() {
        if (subscriber != null) {
            subscriber.stopAsync();
            log.info("Gmail Pub/Sub subscriber stopped");
        }
    }

    public void handleMessage(PubsubMessage message, AckReplyConsumer consumer) {
        try {
            String payload = message.getData().toStringUtf8();
            log.info("Received Gmail Pub/Sub payload: {}", payload);

            var event = objectMapper.readValue(payload, GmailEvent.class);

            var gmailMailboxOptional = gmailMailboxService.findByEmailAddress(event.emailAddress());

            if (gmailMailboxOptional.isEmpty()) {
                log.warn("Mailbox not found for email {}", event.emailAddress());
                consumer.ack();
                return;
            }

            var mailbox = gmailMailboxOptional.get();

            if (mailbox.getHistoryId() > event.historyId()) {
                log.info("Ignoring stale Gmail event");
                consumer.ack();
                return;
            }

            gmailSyncService.triggerSyncJob(mailbox);
            consumer.ack();

        } catch (Exception e) {
            log.error("Failed to process Gmail Pub/Sub message", e);
            consumer.nack();
        }
    }
}
