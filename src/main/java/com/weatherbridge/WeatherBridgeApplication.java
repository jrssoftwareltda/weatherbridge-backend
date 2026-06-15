package com.weatherbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WeatherBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                WeatherBridgeApplication.class,
                args
        );
    }
}
