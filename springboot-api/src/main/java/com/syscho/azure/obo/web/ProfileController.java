package com.syscho.azure.obo.web;

import com.syscho.azure.obo.service.GraphProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final GraphProfileService graphProfileService;
    private final AdoConnectionService adoConnectionService;

    public ProfileController(
            GraphProfileService graphProfileService,
            AdoConnectionService adoConnectionService
    ) {
        this.graphProfileService = graphProfileService;
        this.adoConnectionService = adoConnectionService;
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

    /**
     * Existing API:
     * SPA sends backend access token.
     * Backend uses OBO to call Microsoft Graph.
     */
    @GetMapping("/profile/me")
    public GraphProfileResponse getMyProfile(HttpServletRequest request) {
        String incomingAccessToken = extractBearerToken(request);
        return graphProfileService.getMyProfileUsingObo(incomingAccessToken);
    }

    /**
     * UI calls this after Okta / Entra login to check whether
     * Azure DevOps is already connected for this logged-in user.
     */
    @GetMapping("/ado/status")
    public AdoStatusResponse getAdoStatus(@AuthenticationPrincipal Jwt jwt) {
        String appUserId = getAppUserId(jwt);

        return adoConnectionService.getStatus(appUserId);
    }

    /**
     * UI calls this when backend returns ADO_CONNECT_REQUIRED.
     * Backend creates Microsoft authorization URL for Azure DevOps consent.
     */
    @GetMapping("/ado/connect")
    public AdoConnectResponse connectAdo(@AuthenticationPrincipal Jwt jwt) {
        String appUserId = getAppUserId(jwt);
        String email = getEmail(jwt);

        String authorizationUrl = adoConnectionService.createAuthorizationUrl(appUserId, email);

        return new AdoConnectResponse(authorizationUrl);
    }

    /**
     * Microsoft redirects back here after user grants Azure DevOps consent.
     * Backend exchanges authorization code for token and stores token cache.
     *
     * This endpoint is usually opened by browser redirect, so it redirects
     * user back to SPA success/failure page.
     */
    @GetMapping("/ado/callback")
    public void adoCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {

        adoConnectionService.handleCallback(code, state);

        response.sendRedirect("http://localhost:3000/ado-connected");
    }

    /**
     * UI can use this to disconnect Azure DevOps account.
     */
    @DeleteMapping("/ado/connection")
    public Map<String, Object> disconnectAdo(@AuthenticationPrincipal Jwt jwt) {
        String appUserId = getAppUserId(jwt);

        adoConnectionService.disconnect(appUserId);

        return Map.of(
                "connected", false,
                "message", "Azure DevOps disconnected successfully."
        );
    }

    /**
     * Example ADO API:
     * SPA sends only backend/Okta/Entra token.
     * Backend finds stored ADO token for this user and calls Azure DevOps.
     */
    @GetMapping("/ado/workitems")
    public Object getMyAdoWorkItems(@AuthenticationPrincipal Jwt jwt) {
        String appUserId = getAppUserId(jwt);

        if (!adoConnectionService.isConnected(appUserId)) {
            throw new AdoConnectRequiredException("ADO_CONNECT_REQUIRED", "Please connect Azure DevOps.");
        }

        try {
            return adoConnectionService.getMyWorkItems(appUserId);
        } catch (AdoTokenExpiredException ex) {
            throw new AdoConnectRequiredException("ADO_RECONNECT_REQUIRED", "Azure DevOps connection expired. Please reconnect.");
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization Bearer token.");
        }

        return authorization.substring("Bearer ".length());
    }

    private String getAppUserId(Jwt jwt) {
        /**
         * For Entra token, subject is usually stable per app.
         * If using Okta token, use Okta user id claim like "sub" or "uid".
         */
        return jwt.getSubject();
    }

    private String getEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("email");
        }

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("upn");
        }

        return email;
    }

    public record AdoConnectResponse(String authorizationUrl) {
    }

    public record AdoStatusResponse(
            boolean connected,
            String code,
            String adoEmail,
            String organization
    ) {
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AdoConnectRequiredException extends RuntimeException {
        private final String code;

        public AdoConnectRequiredException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static class AdoTokenExpiredException extends RuntimeException {
        public AdoTokenExpiredException(String message) {
            super(message);
        }
    }
}