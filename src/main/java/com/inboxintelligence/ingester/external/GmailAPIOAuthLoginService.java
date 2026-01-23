package com.inboxintelligence.ingester.external;

import com.inboxintelligence.ingester.common.GmailAPIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailAPIOAuthLoginService {

    private final GmailAPIProperties gmailAPIProperties;

    public String invokeOAuthRedirectURI() {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", gmailAPIProperties.clientId())
                .queryParam("redirect_uri", gmailAPIProperties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", gmailAPIProperties.scope())
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();
    }


}