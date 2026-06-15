package com.weatherbridge.infrastructure.config;

import com.weatherbridge.domain.service.ThermalSensationPolicy;
import com.weatherbridge.infrastructure.properties.OpenWeatherProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class OpenWeatherClientConfiguration {

    @Bean
    @Qualifier("openWeatherRestClient")
    public RestClient openWeatherRestClient(
            RestClient.Builder builder,
            OpenWeatherProperties properties
    ) {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(
                        properties.connectTimeout()
                )
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        return builder
                .baseUrl(
                        properties.baseUrl()
                )
                .requestFactory(
                        requestFactory
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    @Bean
    public ThermalSensationPolicy thermalSensationPolicy() {
        return new ThermalSensationPolicy();
    }
}