package com.syscho.azure.obo.web;

import com.syscho.azure.obo.web.ProfileController.AdoStatusResponse;

public interface AdoConnectionService {

    AdoStatusResponse getStatus(String appUserId);

    boolean isConnected(String appUserId);

    String createAuthorizationUrl(String appUserId, String email);

    void handleCallback(String code, String state);

    void disconnect(String appUserId);

    Object getMyWorkItems(String appUserId);
}