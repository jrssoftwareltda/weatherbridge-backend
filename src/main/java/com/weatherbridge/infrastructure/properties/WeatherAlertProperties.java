package com.weatherbridge.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

@ConfigurationProperties(prefix = "weather.alert")
public record WeatherAlertProperties(
        boolean enabled,
        double thresholdCelsius,
        String webhookUrl,
        Duration connectTimeout,
        Duration readTimeout
) {

    public WeatherAlertProperties {
        webhookUrl =
                webhookUrl == null
                        ? ""
                        : webhookUrl.trim();

        if (!Double.isFinite(thresholdCelsius)) {
            throw new IllegalArgumentException(
                    "Weather alert threshold must be finite."
            );
        }

        Objects.requireNonNull(
                connectTimeout,
                "Weather alert connect timeout must not be null."
        );

        Objects.requireNonNull(
                readTimeout,
                "Weather alert read timeout must not be null."
        );

        validatePositiveDuration(
                connectTimeout,
                "Weather alert connect timeout"
        );

        validatePositiveDuration(
                readTimeout,
                "Weather alert read timeout"
        );

        if (enabled) {
            validateWebhookUrl(
                    webhookUrl
            );
        }
    }

    public URI webhookUri() {
        if (!enabled) {
            throw new IllegalStateException(
                    "Weather alert webhook is disabled."
            );
        }

        return URI.create(
                webhookUrl
        );
    }

    public String webhookHost() {
        if (webhookUrl.isBlank()) {
            return "not-configured";
        }

        try {
            URI uri =
                    URI.create(
                            webhookUrl
                    );

            if (uri.getHost() == null
                    || uri.getHost().isBlank()) {

                return "unknown";
            }

            return uri.getHost();

        } catch (IllegalArgumentException exception) {
            return "invalid";
        }
    }

    private static void validateWebhookUrl(
            String webhookUrl
    ) {
        if (webhookUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Weather alert webhook URL must be configured "
                            + "when temperature alerts are enabled."
            );
        }

        final URI uri;

        try {
            uri =
                    URI.create(
                            webhookUrl
                    );

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Weather alert webhook URL is invalid.",
                    exception
            );
        }

        String scheme =
                uri.getScheme() == null
                        ? ""
                        : uri.getScheme()
                        .toLowerCase(
                                Locale.ROOT
                        );

        boolean validScheme =
                scheme.equals("http")
                        || scheme.equals("https");

        if (!uri.isAbsolute()
                || !validScheme
                || uri.getHost() == null
                || uri.getHost().isBlank()) {

            throw new IllegalArgumentException(
                    "Weather alert webhook URL must be "
                            + "an absolute HTTP or HTTPS URL."
            );
        }
    }

    private static void validatePositiveDuration(
            Duration duration,
            String fieldName
    ) {
        if (duration.isZero()
                || duration.isNegative()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must be greater than zero."
            );
        }
    }
}