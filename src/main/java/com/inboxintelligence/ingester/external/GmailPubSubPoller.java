package com.inboxintelligence.ingester.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.pubsub.v1.*;

import com.inboxintelligence.ingester.model.GmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;


@Slf4j
@RequiredArgsConstructor
public class GmailPubSubPoller {

    private final String projectId;
    private final String subscriptionId;
    private final ObjectMapper objectMapper;

    public boolean poll() throws Exception {

        var subscriptionName = SubscriptionName.of(projectId, subscriptionId);

        try (var subscriber = SubscriberStubSettings.newBuilder().build().createStub()) {

            var pullRequest = PullRequest.newBuilder()
                    .setSubscription(subscriptionName.toString())
                    .setMaxMessages(10)
                    .build();

            var response = subscriber.pullCallable().call(pullRequest);

            var recievedMessageList = response.getReceivedMessagesList();

            if (CollectionUtils.isEmpty(recievedMessageList)) {
                return false;
            }

            recievedMessageList.forEach(message  -> processMessage(message, subscriber, subscriptionName));

            return true;
        }
    }

    private void processMessage(ReceivedMessage message, SubscriberStub subscriber, SubscriptionName subscriptionName) {

        String payload = message.getMessage().getData().toStringUtf8();

        try {

            var gmailevent = objectMapper.readValue(payload, GmailEvent.class);

            handleGmailEvent(event);

            var ackRequest = AcknowledgeRequest.newBuilder()
                            .setSubscription(subscriptionName.toString())
                            .addAckIds(message.getAckId())
                            .build();

            subscriber.acknowledgeCallable().call(ackRequest);

        } catch (Exception e) {
            log.error("Failed to process Pub/Sub message, will retry", e);
            // NO ACK here → Pub/Sub will redeliver
        }
    }

    private void handleGmailEvent(GmailEvent event) {
        log.info(
                "Received Gmail event: email={}, historyId={}",
                event.emailAddress(),
                event.historyId()
        );

        // TODO:
        // 1. Load lastProcessedHistoryId from DB
        // 2. Call Gmail History API
        // 3. Fetch new messages
        // 4. Persist results
    }
}
