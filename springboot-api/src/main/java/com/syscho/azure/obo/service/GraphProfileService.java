package com.syscho.azure.obo.service;

import com.syscho.azure.obo.web.GraphProfileResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GraphProfileService {

    private final OboTokenService oboTokenService;
    private final WebClient webClient;

    public GraphProfileService(OboTokenService oboTokenService, WebClient.Builder webClientBuilder) {
        this.oboTokenService = oboTokenService;
        this.webClient = webClientBuilder
                .baseUrl("https://graph.microsoft.com/v1.0")
                .build();
    }

    public GraphProfileResponse getMyProfileUsingObo(String incomingUserAccessToken) {
        String graphAccessToken = oboTokenService.getGraphAccessTokenOnBehalfOf(incomingUserAccessToken);

        return webClient.get()
                .uri("/me?$select=id,displayName,givenName,surname,userPrincipalName,mail,jobTitle,mobilePhone,officeLocation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + graphAccessToken)
                .retrieve()
                .bodyToMono(GraphProfileResponse.class)
                .block();
    }
}
