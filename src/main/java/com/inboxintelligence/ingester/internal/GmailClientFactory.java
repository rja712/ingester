package com.inboxintelligence.ingester.internal;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.inboxintelligence.ingester.common.GmailAPIProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

@Component
@Slf4j
public class GmailClientFactory {

    private final GmailAPIProperties gmailAPIProperties;
    private final NetHttpTransport httpTransport;
    private final GsonFactory gsonFactory;

    public GmailClientFactory(GmailAPIProperties gmailAPIProperties) throws Exception {
        this.gmailAPIProperties = gmailAPIProperties;
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        this.gsonFactory = GsonFactory.getDefaultInstance();
    }

    public Gmail createUsingGoogleTokenResponse(GoogleTokenResponse googleTokenResponse) {
        var expiresAt = Instant.now().plusSeconds(googleTokenResponse.getExpiresInSeconds());
        var accesstoken = new AccessToken(googleTokenResponse.getAccessToken(), Date.from(expiresAt));
        var credentials = GoogleCredentials.create(accesstoken);
        return this.create(credentials);
    }

    public Gmail createUsingRefreshToken(String refreshToken) {
        var credentials = UserCredentials.newBuilder()
                .setClientId(gmailAPIProperties.clientId())
                .setClientSecret(gmailAPIProperties.clientSecret())
                .setRefreshToken(refreshToken)
                .build();
        return this.create(credentials);
    }

    private Gmail create(GoogleCredentials credentials) {
        return new Gmail.Builder(httpTransport, gsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(gmailAPIProperties.applicationName())
                .build();
    }

    public GoogleAuthorizationCodeTokenRequest createAuthorizationCodeTokenRequest(String authorizationCode) {
        return new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                gsonFactory,
                gmailAPIProperties.tokenUrl(),
                gmailAPIProperties.clientId(),
                gmailAPIProperties.clientSecret(),
                authorizationCode,
                gmailAPIProperties.redirectUri()
        );
    }

    public GoogleIdTokenVerifier createIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(httpTransport, gsonFactory)
                .setAudience(Collections.singletonList(gmailAPIProperties.clientId()))
                .build();
    }


}
