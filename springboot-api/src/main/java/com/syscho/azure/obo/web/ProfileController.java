package com.syscho.azure.obo.web;

import com.syscho.azure.obo.service.GraphProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final GraphProfileService graphProfileService;

    public ProfileController(GraphProfileService graphProfileService) {
        this.graphProfileService = graphProfileService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/token/debug")
    public Map<String, Object> tokenDebug(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "subject", jwt.getSubject(),
                "issuer", jwt.getIssuer().toString(),
                "audience", jwt.getAudience(),
                "scopes", jwt.getClaimAsString("scp"),
                "name", jwt.getClaimAsString("name"),
                "preferredUsername", jwt.getClaimAsString("preferred_username")
        );
    }

    @GetMapping("/profile/me")
    public GraphProfileResponse getMyProfile(HttpServletRequest request) {
        String incomingAccessToken = extractBearerToken(request);
        return graphProfileService.getMyProfileUsingObo(incomingAccessToken);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization Bearer token.");
        }

        return authorization.substring("Bearer ".length());
    }
}
