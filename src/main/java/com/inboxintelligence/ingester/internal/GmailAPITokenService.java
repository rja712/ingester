package com.inboxintelligence.ingester.internal;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.inboxintelligence.ingester.common.GmailAPIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailAPITokenService {

    private final GmailAPIProperties gmailAPIProperties;
    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    public GoogleTokenResponse callback(String code) {
        try {
            var request = new GoogleAuthorizationCodeTokenRequest(
                    httpTransport,
                    gsonFactory,
                    TOKEN_URL,
                    gmailAPIProperties.clientId(),
                    gmailAPIProperties.clientSecret(),
                    code,
                    gmailAPIProperties.redirectUri());
            return request.execute();
        } catch (Exception e) {
            log.error("Failed to exchange authorization code for token", e);
            throw new IllegalStateException("OAuth token exchange failed");
        }
    }
}