package com.syscho.azure.obo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.azure")
public record AzureProperties(
        String tenantId,
        String clientId,
        String clientSecret,
        String audience,
        String graphScope
) {
}
