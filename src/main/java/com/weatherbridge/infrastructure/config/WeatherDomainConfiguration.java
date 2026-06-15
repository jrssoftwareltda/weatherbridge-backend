package com.weatherbridge.infrastructure.config;

import com.weatherbridge.domain.service.ForecastAggregationService;
import com.weatherbridge.domain.service.PrecipitationRiskPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WeatherDomainConfiguration {

    @Bean
    public PrecipitationRiskPolicy
    precipitationRiskPolicy() {
        return new PrecipitationRiskPolicy();
    }

    @Bean
    public ForecastAggregationService
    forecastAggregationService(
            PrecipitationRiskPolicy
                    precipitationRiskPolicy
    ) {
        return new ForecastAggregationService(
                precipitationRiskPolicy
        );
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}