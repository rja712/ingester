package com.inboxintelligence.ingester.internal;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import com.inboxintelligence.ingester.common.GmailAPIProperties;
import com.inboxintelligence.ingester.model.GmailMailbox;
import com.inboxintelligence.ingester.model.SyncStatus;
import com.inboxintelligence.ingester.service.GmailMailboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailAPITokenService {


    private final GmailAPIProperties gmailAPIProperties;
    private final GmailMailboxService gmailMailboxService;
    private final GmailClientFactory gmailClientFactory;

    public void processTokenCallbackCode(String authorizationCode) {
        try {

            log.info("Processing Gmail OAuth callback");
            var tokenResponse = gmailClientFactory.createAuthorizationCodeTokenRequest(authorizationCode).execute();
            var emailAddress = verifyAndExtractEmail(tokenResponse);
            var watchResponse = startMailboxWatch(tokenResponse);

            saveGmailMailbox(tokenResponse, watchResponse, emailAddress);
            log.info("Gmail mailbox onboarding completed for {}", emailAddress);

        } catch (Exception e) {
            log.error("Gmail OAuth onboarding failed", e);
            throw new IllegalStateException("OAuth token exchange failed", e);
        }
    }


    private String verifyAndExtractEmail(GoogleTokenResponse tokenResponse) throws Exception {

        log.debug("Verifying Google ID token");
        var idToken = gmailClientFactory.createIdTokenVerifier().verify(tokenResponse.getIdToken());

        if (idToken == null) {
            throw new IllegalStateException("Invalid Google ID token");
        }

        String email = idToken.getPayload().getEmail();
        log.info("Authenticated Gmail user {}", email);

        return email;
    }


    private WatchResponse startMailboxWatch(GoogleTokenResponse tokenResponse) throws Exception {

        log.debug("Starting Gmail mailbox watch (Pub/Sub)");
        var gmail = gmailClientFactory.createUsingGoogleTokenResponse(tokenResponse);

        var watchRequest = new WatchRequest()
                .setTopicName(gmailAPIProperties.pubsubTopic())
                .setLabelIds(List.of("INBOX"));

        var response = gmail.users().watch("me", watchRequest).execute();
        log.info("Mailbox watch started. historyId={}, expiresAt={}", response.getHistoryId(), response.getExpiration());

        return response;
    }


    private void saveGmailMailbox(GoogleTokenResponse tokenResponse, WatchResponse watchResponse, String email) {

        var gmailMailbox = gmailMailboxService.findByEmailAddress(email).orElseGet(GmailMailbox::new);
        var accessTokenExpiresAt = Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds());

        gmailMailbox.setEmailAddress(email);
        gmailMailbox.setAccessToken(tokenResponse.getAccessToken());
        gmailMailbox.setRefreshToken(tokenResponse.getRefreshToken());
        gmailMailbox.setAccessTokenExpiresAt(accessTokenExpiresAt);
        gmailMailbox.setHistoryId(watchResponse.getHistoryId().longValue());
        gmailMailbox.setWatchExpiresAt(watchResponse.getExpiration());
        gmailMailbox.setSyncStatus(SyncStatus.ACTIVE);

        gmailMailboxService.save(gmailMailbox);
        log.debug("Persisted Gmail mailbox entity for {}", email);

    }
}
