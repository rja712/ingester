package com.inboxintelligence.ingester.internal;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.inboxintelligence.ingester.common.GmailAPIProperties;
import com.inboxintelligence.ingester.model.EmailMetadata;
import com.inboxintelligence.ingester.service.EmailMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailAPITokenService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private final GmailAPIProperties gmailAPIProperties;
    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();
    private final EmailMetadataService emailMetadataService;

    public void processTokenCallbackCode(String code) {
        try {
            var request = new GoogleAuthorizationCodeTokenRequest(
                    httpTransport,
                    gsonFactory,
                    TOKEN_URL,
                    gmailAPIProperties.clientId(),
                    gmailAPIProperties.clientSecret(),
                    code,
                    gmailAPIProperties.redirectUri());

            var googleTokenResponse = request.execute();

            var verifier = new GoogleIdTokenVerifier.Builder(httpTransport, gsonFactory)
                    .setAudience(Collections.singletonList(gmailAPIProperties.clientId()))
                    .build();

            var googleIdToken = verifier.verify(googleTokenResponse.getIdToken());

            var emailMetadata = EmailMetadata.builder()
                    .emailAddress(googleIdToken.getPayload().getEmail())
                    .accessToken(googleTokenResponse.getAccessToken())
                    .refreshToken(googleTokenResponse.getRefreshToken())
                    .build();

            emailMetadataService.save(emailMetadata);

            log.info("Saved Email MetaData {}", emailMetadata.getEmailAddress());

        } catch (Exception e) {
            log.error("Failed to exchange authorization code for token", e);
            throw new IllegalStateException("OAuth token exchange failed");
        }
    }
}