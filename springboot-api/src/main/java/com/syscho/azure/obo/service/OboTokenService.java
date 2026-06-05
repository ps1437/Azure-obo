package com.syscho.azure.obo.service;

import com.syscho.azure.obo.config.AzureProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class OboTokenService {

    private final AzureProperties azureProperties;
    private final WebClient webClient;

    public OboTokenService(AzureProperties azureProperties, WebClient.Builder webClientBuilder) {
        this.azureProperties = azureProperties;
        this.webClient = webClientBuilder.build();
    }

    public String getGraphAccessTokenOnBehalfOf(String incomingUserAccessToken) {
        String tokenEndpoint = "https://login.microsoftonline.com/"
                + azureProperties.tenantId()
                + "/oauth2/v2.0/token";

        Map<String, Object> response = webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", azureProperties.clientId())
                        .with("client_secret", azureProperties.clientSecret())
                        .with("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .with("assertion", incomingUserAccessToken)
                        .with("requested_token_use", "on_behalf_of")
                        .with("scope", azureProperties.graphScope())
                )
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("No access_token returned from OBO token endpoint.");
        }

        return response.get("access_token").toString();
    }
}
