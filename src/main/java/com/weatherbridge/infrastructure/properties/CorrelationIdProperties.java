package com.weatherbridge.infrastructure.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.correlation-id")
public record CorrelationIdProperties(

        @NotBlank
        String requestHeader,

        @NotBlank
        String responseHeader
) {
}