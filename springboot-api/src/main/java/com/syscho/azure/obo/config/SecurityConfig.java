package com.syscho.azure.obo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.List;

//Support for old version
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final AzureProperties azureProperties;
    private final CorsProperties corsProperties;

    public SecurityConfig(AzureProperties azureProperties, CorsProperties corsProperties) {
        this.azureProperties = azureProperties;
        this.corsProperties = corsProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        String tenantId = azureProperties.tenantId();

        String v2Issuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
        String v1Issuer = "https://sts.windows.net/" + tenantId + "/";

        String jwkSetUri = "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> timestampValidator = JwtValidators.createDefault();

        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String actualIssuer = jwt.getIssuer().toString();

            if (actualIssuer.equals(v2Issuer) || actualIssuer.equals(v1Issuer)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Invalid issuer. Actual issuer: " + actualIssuer
                            + ", expected: " + v2Issuer + " or " + v1Issuer,
                    null
            );

            return OAuth2TokenValidatorResult.failure(error);
        };

        OAuth2TokenValidator<Jwt> audienceValidator =
                new AudienceValidator(azureProperties.audience());

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        timestampValidator,
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(corsProperties.allowedOrigin()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}