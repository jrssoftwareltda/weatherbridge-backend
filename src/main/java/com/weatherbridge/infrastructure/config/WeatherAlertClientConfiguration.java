package com.weatherbridge.infrastructure.config;

import com.weatherbridge.application.model.TemperatureAlertSettings;
import com.weatherbridge.domain.service.TemperatureThresholdPolicy;
import com.weatherbridge.infrastructure.properties.WeatherAlertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
public class WeatherAlertClientConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(
                    WeatherAlertClientConfiguration.class
            );

    @Bean
    public TemperatureAlertSettings
    temperatureAlertSettings(
            WeatherAlertProperties properties
    ) {
        TemperatureAlertSettings settings =
                new TemperatureAlertSettings(
                        properties.enabled(),
                        properties.thresholdCelsius()
                );

        log.info(
                "Temperature alert settings configured: "
                        + "enabled={}, thresholdCelsius={}",
                settings.enabled(),
                settings.thresholdCelsius()
        );

        return settings;
    }

    @Bean
    public TemperatureThresholdPolicy
    temperatureThresholdPolicy() {
        log.debug(
                "TemperatureThresholdPolicy configured"
        );

        return new TemperatureThresholdPolicy();
    }

    @Bean(name = "weatherAlertRestClient")
    @ConditionalOnProperty(
            prefix = "weather.alert",
            name = "enabled",
            havingValue = "true"
    )
    public RestClient weatherAlertRestClient(
            WeatherAlertProperties properties
    ) {
        HttpClient httpClient =
                HttpClient
                        .newBuilder()
                        .connectTimeout(
                                properties.connectTimeout()
                        )
                        .followRedirects(
                                HttpClient.Redirect.NEVER
                        )
                        .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        log.info(
                "Temperature alert RestClient configured: "
                        + "webhookHost={}, connectTimeout={}, "
                        + "readTimeout={}",
                properties.webhookHost(),
                properties.connectTimeout(),
                properties.readTimeout()
        );

        return RestClient
                .builder()
                .requestFactory(
                        requestFactory
                )
                .defaultHeader(
                        HttpHeaders.USER_AGENT,
                        "WeatherBridge/1.0"
                )
                .build();
    }
}