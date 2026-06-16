package com.weatherbridge.infrastructure.config;

import com.weatherbridge.infrastructure.properties.WeatherCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CacheConfiguration.class
            );

    private static final String CACHE_PREFIX =
            "weather-bridge::";

    private static final String CURRENT_WEATHER_CACHE =
            "current-weather";

    private static final String WEATHER_FORECAST_CACHE =
            "weather-forecast";

    private static final String TEMPERATURE_ALERT_CACHE =
            "temperature-alert";

    @Bean
    public RedisSerializer<Object> redisCacheValueSerializer() {
        PolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator
                        .builder()
                        .allowIfSubType(
                                "com.weatherbridge."
                        )
                        .allowIfSubType(
                                "java.util.ArrayList"
                        )
                        .build();

        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer
                        .builder()
                        .enableDefaultTyping(
                                typeValidator
                        )
                        .typePropertyName(
                                "@class"
                        )
                        .customize(
                                mapperBuilder ->
                                        mapperBuilder
                                                .findAndAddModules()
                        )
                        .build();

        log.info(
                "Redis JSON cache serializer configured: "
                        + "serializer={}, allowedPackage={}",
                serializer.getClass().getSimpleName(),
                "com.weatherbridge"
        );

        return serializer;
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer
    weatherRedisCacheManagerBuilderCustomizer(
            WeatherCacheProperties properties,
            RedisSerializer<Object> redisCacheValueSerializer
    ) {
        log.info(
                "Configuring Redis caches: "
                        + "currentWeatherTtl={}, "
                        + "forecastTtl={}, "
                        + "alertDeduplicationTtl={}",
                properties.currentWeatherTtl(),
                properties.forecastTtl(),
                properties.alertDeduplicationTtl()
        );

        return builder -> builder
                .withCacheConfiguration(
                        CURRENT_WEATHER_CACHE,
                        createCacheConfiguration(
                                properties.currentWeatherTtl(),
                                redisCacheValueSerializer
                        )
                )
                .withCacheConfiguration(
                        WEATHER_FORECAST_CACHE,
                        createCacheConfiguration(
                                properties.forecastTtl(),
                                redisCacheValueSerializer
                        )
                )
                .withCacheConfiguration(
                        TEMPERATURE_ALERT_CACHE,
                        createCacheConfiguration(
                                properties.alertDeduplicationTtl(),
                                redisCacheValueSerializer
                        )
                );
    }

    private RedisCacheConfiguration createCacheConfiguration(
            Duration ttl,
            RedisSerializer<Object> valueSerializer
    ) {
        RedisSerializationContext.SerializationPair<String>
                keySerializationPair =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                StringRedisSerializer.UTF_8
                        );

        RedisSerializationContext.SerializationPair<Object>
                valueSerializationPair =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                valueSerializer
                        );

        return RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith(
                        CACHE_PREFIX
                )
                .serializeKeysWith(
                        keySerializationPair
                )
                .serializeValuesWith(
                        valueSerializationPair
                );
    }
}