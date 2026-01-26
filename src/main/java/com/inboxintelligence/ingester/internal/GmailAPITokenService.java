package com.inboxintelligence.ingester.internal;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.inboxintelligence.ingester.common.GmailAPIProperties;
import com.inboxintelligence.ingester.model.GmailMailbox;
import com.inboxintelligence.ingester.model.SyncStatus;
import com.inboxintelligence.ingester.service.GmailMailboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailAPITokenService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final GmailAPIProperties gmailAPIProperties;
    private final GmailMailboxService gmailMailboxService;

    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

    public void processTokenCallbackCode(String code) {
        try {
            log.info("Processing Gmail OAuth callback");

            var tokenResponse = exchangeAuthCode(code);
            var email = verifyAndExtractEmail(tokenResponse);
            var watchResponse = startMailboxWatch(tokenResponse);

            saveGmailMailbox(tokenResponse, watchResponse, email);

            log.info("Gmail mailbox onboarding completed for {}", email);
        } catch (Exception e) {
            log.error("Gmail OAuth onboarding failed", e);
            throw new IllegalStateException("OAuth token exchange failed", e);
        }
    }


    private GoogleTokenResponse exchangeAuthCode(String code) throws Exception {

        log.debug("Exchanging authorization code for tokens");

        var request = new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                gsonFactory,
                TOKEN_URL,
                gmailAPIProperties.clientId(),
                gmailAPIProperties.clientSecret(),
                code,
                gmailAPIProperties.redirectUri()
        );

        return request.execute();
    }


    private String verifyAndExtractEmail(GoogleTokenResponse tokenResponse) throws Exception {

        log.debug("Verifying Google ID token");

        var verifier = new GoogleIdTokenVerifier.Builder(httpTransport, gsonFactory)
                .setAudience(Collections.singletonList(gmailAPIProperties.clientId()))
                .build();

        var idToken = verifier.verify(tokenResponse.getIdToken());

        if (idToken == null) {
            throw new IllegalStateException("Invalid Google ID token");
        }

        String email = idToken.getPayload().getEmail();
        log.info("Authenticated Gmail user {}", email);

        return email;
    }


    private WatchResponse startMailboxWatch(GoogleTokenResponse tokenResponse) throws Exception {

        var expiresAt = Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds());

        var accessToken = new AccessToken(tokenResponse.getAccessToken(), Date.from(expiresAt));

        var credentials = GoogleCredentials.create(accessToken);

        var gmailClient = new Gmail.Builder(httpTransport, gsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName("InboxIntelligence")
                .build();

        log.debug("Starting Gmail mailbox watch (Pub/Sub)");

        var watchRequest = new WatchRequest()
                .setTopicName(gmailAPIProperties.pubsubTopic())
                .setLabelIds(List.of("INBOX"));

        var response = gmailClient.users().watch("me", watchRequest).execute();

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
