package com.syscho.azure.obo.web;

public record GraphProfileResponse(
        String id,
        String displayName,
        String givenName,
        String surname,
        String userPrincipalName,
        String mail,
        String jobTitle,
        String mobilePhone,
        String officeLocation
) {
}
