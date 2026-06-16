package com.weatherbridge.application.port.out;

import com.weatherbridge.application.model.TemperatureAlert;
import com.weatherbridge.application.model.WeatherAlertDelivery;

public interface WeatherAlertPort {

    WeatherAlertDelivery send(
            TemperatureAlert alert
    );
}