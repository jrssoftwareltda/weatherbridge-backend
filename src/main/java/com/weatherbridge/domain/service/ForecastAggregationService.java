package com.weatherbridge.domain.service;

import com.weatherbridge.domain.model.DailyWeatherSummary;
import com.weatherbridge.domain.model.FiveDayWeatherSummary;
import com.weatherbridge.domain.model.ForecastSlice;
import com.weatherbridge.domain.model.PrecipitationRisk;
import com.weatherbridge.domain.model.WeatherForecast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public final class ForecastAggregationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ForecastAggregationService.class
            );

    private static final int MAXIMUM_DAYS = 5;

    private final PrecipitationRiskPolicy
            precipitationRiskPolicy;

    public ForecastAggregationService(
            PrecipitationRiskPolicy precipitationRiskPolicy
    ) {
        this.precipitationRiskPolicy =
                Objects.requireNonNull(
                        precipitationRiskPolicy,
                        "Precipitation risk policy must not be null."
                );

        log.debug(
                "ForecastAggregationService initialized with precipitationRiskPolicy={}",
                precipitationRiskPolicy
                        .getClass()
                        .getSimpleName()
        );
    }

    public FiveDayWeatherSummary aggregate(
            WeatherForecast forecast,
            Instant generatedAt
    ) {
        Objects.requireNonNull(
                forecast,
                "Weather forecast must not be null."
        );

        Objects.requireNonNull(
                generatedAt,
                "Generated time must not be null."
        );

        long startedAt = System.nanoTime();

        log.info(
                "Starting forecast aggregation: "
                        + "city={}, stateCode={}, countryCode={}, "
                        + "source={}, receivedSlices={}, generatedAt={}",
                forecast.location().city(),
                forecast.location().stateCode(),
                forecast.location().countryCode(),
                forecast.source(),
                forecast.slices().size(),
                generatedAt
        );

        try {
            ZoneOffset zoneOffset =
                    resolveZoneOffset(forecast);

            Map<LocalDate, List<ForecastSlice>>
                    slicesByDate =
                    groupSlicesByLocalDate(
                            forecast.slices(),
                            zoneOffset
                    );

            log.info(
                    "Forecast slices grouped successfully: "
                            + "city={}, totalLocalDates={}, "
                            + "dates={}, maximumReturnedDays={}",
                    forecast.location().city(),
                    slicesByDate.size(),
                    slicesByDate.keySet(),
                    MAXIMUM_DAYS
            );

            logSliceDistribution(
                    slicesByDate
            );

            List<DailyWeatherSummary> summaries =
                    slicesByDate
                            .entrySet()
                            .stream()
                            .limit(MAXIMUM_DAYS)
                            .map(entry -> aggregateDay(
                                    entry.getKey(),
                                    entry.getValue()
                            ))
                            .toList();

            if (summaries.isEmpty()) {
                log.warn(
                        "Forecast aggregation produced no daily summaries: "
                                + "city={}, receivedSlices={}, groupedDates={}",
                        forecast.location().city(),
                        forecast.slices().size(),
                        slicesByDate.size()
                );

                throw new IllegalArgumentException(
                        "Forecast does not contain daily data."
                );
            }

            FiveDayWeatherSummary result =
                    new FiveDayWeatherSummary(
                            forecast.location(),
                            generatedAt,
                            summaries,
                            forecast.source()
                    );

            log.info(
                    "Forecast aggregation completed successfully: "
                            + "city={}, returnedDays={}, firstDate={}, "
                            + "lastDate={}, durationMs={}",
                    forecast.location().city(),
                    result.days().size(),
                    result.days().getFirst().date(),
                    result.days().getLast().date(),
                    elapsedMilliseconds(startedAt)
            );

            return result;

        } catch (RuntimeException exception) {
            log.error(
                    "Forecast aggregation failed: "
                            + "city={}, receivedSlices={}, "
                            + "exceptionType={}, message={}, durationMs={}",
                    forecast.location().city(),
                    forecast.slices().size(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    elapsedMilliseconds(startedAt)
            );

            throw exception;
        }
    }

    private ZoneOffset resolveZoneOffset(
            WeatherForecast forecast
    ) {
        int timezoneOffsetSeconds =
                forecast.location()
                        .timezoneOffsetSeconds();

        ZoneOffset zoneOffset =
                ZoneOffset.ofTotalSeconds(
                        timezoneOffsetSeconds
                );

        log.debug(
                "Forecast timezone resolved: "
                        + "city={}, timezoneOffsetSeconds={}, zoneOffset={}",
                forecast.location().city(),
                timezoneOffsetSeconds,
                zoneOffset
        );

        return zoneOffset;
    }

    private Map<LocalDate, List<ForecastSlice>>
    groupSlicesByLocalDate(
            List<ForecastSlice> slices,
            ZoneOffset zoneOffset
    ) {
        Map<LocalDate, List<ForecastSlice>>
                slicesByDate = new TreeMap<>();

        log.debug(
                "Grouping forecast slices by local date: "
                        + "sliceCount={}, zoneOffset={}",
                slices.size(),
                zoneOffset
        );

        slices.stream()
                .sorted(
                        Comparator.comparing(
                                ForecastSlice::forecastAt
                        )
                )
                .forEach(slice -> {
                    LocalDate localDate =
                            slice.forecastAt()
                                    .atOffset(zoneOffset)
                                    .toLocalDate();

                    slicesByDate
                            .computeIfAbsent(
                                    localDate,
                                    ignored -> new ArrayList<>()
                            )
                            .add(slice);

                    if (log.isTraceEnabled()) {
                        log.trace(
                                "Forecast slice assigned to local date: "
                                        + "forecastAt={}, localDate={}, "
                                        + "temperatureCelsius={}, "
                                        + "precipitationProbabilityPercent={}, "
                                        + "condition={}",
                                slice.forecastAt(),
                                localDate,
                                slice.temperatureCelsius(),
                                slice.precipitationProbabilityPercent(),
                                slice.condition()
                        );
                    }
                });

        return slicesByDate;
    }

    private void logSliceDistribution(
            Map<LocalDate, List<ForecastSlice>>
                    slicesByDate
    ) {
        if (!log.isDebugEnabled()) {
            return;
        }

        slicesByDate.forEach(
                (date, slices) -> log.debug(
                        "Forecast slice distribution: "
                                + "date={}, slices={}, "
                                + "firstForecastAt={}, lastForecastAt={}",
                        date,
                        slices.size(),
                        slices.getFirst().forecastAt(),
                        slices.getLast().forecastAt()
                )
        );
    }

    private DailyWeatherSummary aggregateDay(
            LocalDate date,
            List<ForecastSlice> slices
    ) {
        Objects.requireNonNull(
                date,
                "Aggregation date must not be null."
        );

        if (slices == null || slices.isEmpty()) {
            log.warn(
                    "Cannot aggregate empty forecast day: date={}",
                    date
            );

            throw new IllegalArgumentException(
                    "Forecast slices must not be empty for date: "
                            + date
            );
        }

        long startedAt = System.nanoTime();

        log.debug(
                "Starting daily forecast aggregation: "
                        + "date={}, sliceCount={}",
                date,
                slices.size()
        );

        double minimumTemperature =
                slices.stream()
                        .mapToDouble(
                                ForecastSlice
                                        ::minimumTemperatureCelsius
                        )
                        .min()
                        .orElseThrow();

        double maximumTemperature =
                slices.stream()
                        .mapToDouble(
                                ForecastSlice
                                        ::maximumTemperatureCelsius
                        )
                        .max()
                        .orElseThrow();

        double averageTemperature =
                slices.stream()
                        .mapToDouble(
                                ForecastSlice
                                        ::temperatureCelsius
                        )
                        .average()
                        .orElseThrow();

        double averageFeelsLike =
                slices.stream()
                        .mapToDouble(
                                ForecastSlice
                                        ::feelsLikeCelsius
                        )
                        .average()
                        .orElseThrow();

        int averageHumidity =
                (int) Math.round(
                        slices.stream()
                                .mapToInt(
                                        ForecastSlice
                                                ::humidityPercent
                                )
                                .average()
                                .orElseThrow()
                );

        double maximumPrecipitationProbability =
                slices.stream()
                        .mapToDouble(
                                ForecastSlice
                                        ::precipitationProbabilityPercent
                        )
                        .max()
                        .orElse(0);

        double totalPrecipitation =
                slices.stream()
                        .mapToDouble(
                                ForecastSlice
                                        ::precipitationMillimeters
                        )
                        .sum();

        double maximumWindSpeed =
                slices.stream()
                        .mapToDouble(
                                ForecastSlice::windSpeedKmh
                        )
                        .max()
                        .orElse(0);

        log.debug(
                "Daily numerical aggregates calculated: "
                        + "date={}, minimumTemperature={}, "
                        + "maximumTemperature={}, averageTemperature={}, "
                        + "averageFeelsLike={}, averageHumidity={}, "
                        + "maximumPrecipitationProbability={}, "
                        + "totalPrecipitation={}, maximumWindSpeed={}",
                date,
                minimumTemperature,
                maximumTemperature,
                averageTemperature,
                averageFeelsLike,
                averageHumidity,
                maximumPrecipitationProbability,
                totalPrecipitation,
                maximumWindSpeed
        );

        PrecipitationRisk precipitationRisk =
                precipitationRiskPolicy.classify(
                        maximumPrecipitationProbability
                );

        String dominantCondition =
                dominantCondition(
                        date,
                        slices
                );

        DailyWeatherSummary summary =
                new DailyWeatherSummary(
                        date,
                        roundOneDecimal(
                                minimumTemperature
                        ),
                        roundOneDecimal(
                                maximumTemperature
                        ),
                        roundOneDecimal(
                                averageTemperature
                        ),
                        roundOneDecimal(
                                averageFeelsLike
                        ),
                        averageHumidity,
                        roundOneDecimal(
                                maximumPrecipitationProbability
                        ),
                        precipitationRisk,
                        roundOneDecimal(
                                totalPrecipitation
                        ),
                        roundOneDecimal(
                                maximumWindSpeed
                        ),
                        dominantCondition
                );

        log.info(
                "Daily forecast aggregation completed: "
                        + "date={}, slices={}, minimumTemperatureCelsius={}, "
                        + "maximumTemperatureCelsius={}, "
                        + "averageTemperatureCelsius={}, "
                        + "precipitationProbabilityPercent={}, "
                        + "precipitationRisk={}, "
                        + "totalPrecipitationMillimeters={}, "
                        + "dominantCondition={}, durationMs={}",
                summary.date(),
                slices.size(),
                summary.minimumTemperatureCelsius(),
                summary.maximumTemperatureCelsius(),
                summary.averageTemperatureCelsius(),
                summary.maximumPrecipitationProbabilityPercent(),
                summary.precipitationRisk(),
                summary.totalPrecipitationMillimeters(),
                summary.dominantCondition(),
                elapsedMilliseconds(startedAt)
        );

        return summary;
    }

    private String dominantCondition(
            LocalDate date,
            List<ForecastSlice> slices
    ) {
        Map<String, Integer> frequencies =
                new LinkedHashMap<>();

        for (ForecastSlice slice : slices) {
            frequencies.merge(
                    slice.condition(),
                    1,
                    Integer::sum
            );
        }

        log.debug(
                "Weather condition frequencies calculated: "
                        + "date={}, frequencies={}",
                date,
                frequencies
        );

        String dominantCondition = null;
        int highestFrequency = -1;

        for (Map.Entry<String, Integer> entry
                : frequencies.entrySet()) {

            if (entry.getValue() > highestFrequency) {
                highestFrequency = entry.getValue();
                dominantCondition = entry.getKey();
            }
        }

        if (dominantCondition == null) {
            log.warn(
                    "Unable to determine dominant weather condition: "
                            + "date={}, sliceCount={}, frequencies={}",
                    date,
                    slices.size(),
                    frequencies
            );

            throw new IllegalArgumentException(
                    "Unable to determine dominant weather condition."
            );
        }

        log.debug(
                "Dominant weather condition selected: "
                        + "date={}, condition={}, frequency={}",
                date,
                dominantCondition,
                highestFrequency
        );

        return dominantCondition;
    }

    private double roundOneDecimal(
            double value
    ) {
        double roundedValue =
                BigDecimal
                        .valueOf(value)
                        .setScale(
                                1,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue();

        if (log.isTraceEnabled()) {
            log.trace(
                    "Numeric value rounded: originalValue={}, roundedValue={}",
                    value,
                    roundedValue
            );
        }

        return roundedValue;
    }

    private long elapsedMilliseconds(
            long startedAt
    ) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }
}