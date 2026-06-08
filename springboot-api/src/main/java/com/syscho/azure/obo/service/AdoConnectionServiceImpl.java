package com.syscho.azure.obo.service;

import com.syscho.azure.obo.config.AdoProperties;
import com.syscho.azure.obo.web.AdoConnectionService;
import com.syscho.azure.obo.web.ProfileController.AdoStatusResponse;
import com.syscho.azure.obo.web.ProfileController.AdoTokenExpiredException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdoConnectionServiceImpl implements AdoConnectionService {

    private final AdoProperties properties;
    private final RestClient restClient;

    /**
     * Demo only.
     * Replace with DB table later.
     *
     * Key: appUserId / Okta user id / Entra subject
     */
    private final Map<String, AdoTokenRecord> tokenStore = new ConcurrentHashMap<>();

    /**
     * Demo only.
     * State is used to map callback request back to current app user.
     *
     * Key: OAuth state
     * Value: pending user info
     */
    private final Map<String, PendingAdoConnect> pendingStateStore = new ConcurrentHashMap<>();

    public AdoConnectionServiceImpl(AdoProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public AdoStatusResponse getStatus(String appUserId) {
        AdoTokenRecord tokenRecord = tokenStore.get(appUserId);

        if (tokenRecord == null) {
            return new AdoStatusResponse(
                    false,
                    "ADO_CONNECT_REQUIRED",
                    null,
                    properties.getOrganization()
            );
        }

        return new AdoStatusResponse(
                true,
                "ADO_CONNECTED",
                tokenRecord.adoEmail(),
                properties.getOrganization()
        );
    }

    @Override
    public boolean isConnected(String appUserId) {
        return tokenStore.containsKey(appUserId);
    }

    @Override
    public String createAuthorizationUrl(String appUserId, String email) {
        String state = UUID.randomUUID().toString();

        pendingStateStore.put(
                state,
                new PendingAdoConnect(appUserId, email, Instant.now().plusSeconds(300))
        );

        String authorizeEndpoint = "https://login.microsoftonline.com/"
                + properties.getTenantId()
                + "/oauth2/v2.0/authorize";

        return authorizeEndpoint
                + "?client_id=" + encode(properties.getClientId())
                + "&response_type=code"
                + "&redirect_uri=" + encode(properties.getRedirectUri())
                + "&response_mode=query"
                + "&scope=" + encode(properties.getScope() + " offline_access openid profile email")
                + "&state=" + encode(state)
                + "&prompt=consent";
    }

    @Override
    public void handleCallback(String code, String state) {
        PendingAdoConnect pending = pendingStateStore.remove(state);

        if (pending == null) {
            throw new IllegalArgumentException("Invalid or expired OAuth state.");
        }

        if (pending.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OAuth state expired.");
        }

        TokenResponse tokenResponse = exchangeCodeForToken(code);

        /**
         * For production:
         * - Fetch ADO/Microsoft profile using token if needed
         * - Validate tenant/user
         * - Encrypt refresh token/token cache before saving
         */
        AdoTokenRecord tokenRecord = new AdoTokenRecord(
                pending.appUserId(),
                pending.email(),
                tokenResponse.access_token(),
                tokenResponse.refresh_token(),
                Instant.now().plusSeconds(tokenResponse.expires_in() - 60),
                properties.getOrganization()
        );

        tokenStore.put(pending.appUserId(), tokenRecord);
    }

    @Override
    public void disconnect(String appUserId) {
        tokenStore.remove(appUserId);
    }

    @Override
    public Object getMyWorkItems(String appUserId) {
        String adoAccessToken = getValidAdoAccessToken(appUserId);

        String url = "https://dev.azure.com/"
                + properties.getOrganization()
                + "/"
                + properties.getProject()
                + "/_apis/wit/wiql?api-version=7.1";

        Map<String, String> body = Map.of(
                "query",
                """
                SELECT [System.Id], [System.Title], [System.State], [System.AssignedTo]
                FROM WorkItems
                WHERE [System.AssignedTo] = @Me
                ORDER BY [System.ChangedDate] DESC
                """
        );

        try {
            return restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adoAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Object.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            tokenStore.remove(appUserId);
            throw new AdoTokenExpiredException("Azure DevOps token is invalid or expired.");
        }
    }

    private String getValidAdoAccessToken(String appUserId) {
        AdoTokenRecord tokenRecord = tokenStore.get(appUserId);

        if (tokenRecord == null) {
            throw new AdoTokenExpiredException("Azure DevOps is not connected.");
        }

        if (tokenRecord.expiresAt().isAfter(Instant.now())) {
            return tokenRecord.accessToken();
        }

        TokenResponse refreshedToken = refreshAccessToken(tokenRecord.refreshToken());

        AdoTokenRecord updated = new AdoTokenRecord(
                tokenRecord.appUserId(),
                tokenRecord.adoEmail(),
                refreshedToken.access_token(),
                refreshedToken.refresh_token() != null
                        ? refreshedToken.refresh_token()
                        : tokenRecord.refreshToken(),
                Instant.now().plusSeconds(refreshedToken.expires_in() - 60),
                tokenRecord.organization()
        );

        tokenStore.put(appUserId, updated);

        return updated.accessToken();
    }

    private TokenResponse exchangeCodeForToken(String code) {
        String tokenEndpoint = "https://login.microsoftonline.com/"
                + properties.getTenantId()
                + "/oauth2/v2.0/token";

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("scope", properties.getScope() + " offline_access openid profile email");

        return restClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    private TokenResponse refreshAccessToken(String refreshToken) {
        String tokenEndpoint = "https://login.microsoftonline.com/"
                + properties.getTenantId()
                + "/oauth2/v2.0/token";

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("scope", properties.getScope() + " offline_access openid profile email");

        try {
            return restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (HttpClientErrorException ex) {
            throw new AdoTokenExpiredException("Unable to refresh Azure DevOps token.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record PendingAdoConnect(
            String appUserId,
            String email,
            Instant expiresAt
    ) {
    }

    private record AdoTokenRecord(
            String appUserId,
            String adoEmail,
            String accessToken,
            String refreshToken,
            Instant expiresAt,
            String organization
    ) {
    }

    private record TokenResponse(
            String token_type,
            String scope,
            Integer expires_in,
            Integer ext_expires_in,
            String access_token,
            String refresh_token,
            String id_token
    ) {
    }
}