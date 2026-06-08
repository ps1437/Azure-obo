package com.syscho.azure.obo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ado.oauth")
public class AdoProperties {

    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String frontendSuccessUrl;
    private String organization;
    private String project;

    /**
     * Azure DevOps resource scope.
     * Common value:
     * 499b84ac-1321-427f-aa17-267ca6975798/.default
     *
     * Or delegated:
     * 499b84ac-1321-427f-aa17-267ca6975798/user_impersonation
     */
    private String scope;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getFrontendSuccessUrl() {
        return frontendSuccessUrl;
    }

    public void setFrontendSuccessUrl(String frontendSuccessUrl) {
        this.frontendSuccessUrl = frontendSuccessUrl;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}