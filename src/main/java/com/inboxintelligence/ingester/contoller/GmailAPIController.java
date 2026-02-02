package com.inboxintelligence.ingester.contoller;

import com.inboxintelligence.ingester.external.GmailAPIOAuthLoginService;
import com.inboxintelligence.ingester.internal.GmailAPITokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/gmail-api")
@RequiredArgsConstructor
public class GmailAPIController {

    public final GmailAPIOAuthLoginService gmailAPIOAuthLoginService;
    public final GmailAPITokenService gmailAPITokenService;

    @GetMapping("/login")
    public void invokeOAuthRedirectURI(HttpServletResponse response) {
        log.info("Invoking oAuth Redirect URI");
        try {
            var authUrl = gmailAPIOAuthLoginService.invokeOAuthRedirectURI();
            log.info("Generated Gmail Oauth Redirect URI");
            response.sendRedirect(authUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/token-callback")
    public void processTokenCallbackCode(@RequestParam String code) {
        log.info("Received Authorization Code");
        gmailAPITokenService.processTokenCallbackCode(code);
    }
}
