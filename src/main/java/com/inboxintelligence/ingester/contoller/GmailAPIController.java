package com.inboxintelligence.ingester.contoller;

import com.inboxintelligence.ingester.internal.GmailAPITokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class GmailAPIController {

    public static GmailAPITokenService gmailAPITokenService;
    p
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent";

        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam String code) throws Exception {

        log.info("Google OAuth callback processed successfully"); // i have added @slf4j still this is giving error

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        return "OAuth successful. You can close this tab.";
    }
}
