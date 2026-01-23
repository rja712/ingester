package com.inboxintelligence.ingester.contoller;

import com.inboxintelligence.ingester.external.GmailAPIOAuthLoginService;
import com.inboxintelligence.ingester.internal.GmailAPITokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/gmail-api")
@RequiredArgsConstructor
public class GmailAPIController {

    public final GmailAPIOAuthLoginService gmailAPIOAuthLoginService;
    public final GmailAPITokenService gmailAPITokenService;

    @GetMapping("/login")
    public void invokeOAuthRedirectURI(HttpServletResponse response) {
        try {
            var authUrl = gmailAPIOAuthLoginService.invokeOAuthRedirectURI();
            response.sendRedirect(authUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/token-callback")
    public void processTokenCallbackCode(@RequestParam String code) {
        gmailAPITokenService.processTokenCallbackCode(code);
    }
}
