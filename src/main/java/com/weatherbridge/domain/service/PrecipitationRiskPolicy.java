package com.weatherbridge.domain.service;

import com.weatherbridge.domain.model.PrecipitationRisk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrecipitationRiskPolicy {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PrecipitationRiskPolicy.class
            );

    public PrecipitationRisk classify(
            double probabilityPercent
    ) {
        log.debug(
                "Classifying precipitation risk: probabilityPercent={}",
                probabilityPercent
        );

        validateProbability(
                probabilityPercent
        );

        PrecipitationRisk risk =
                determineRisk(
                        probabilityPercent
                );

        log.debug(
                "Precipitation risk classified successfully: "
                        + "probabilityPercent={}, risk={}",
                probabilityPercent,
                risk
        );

        return risk;
    }

    private void validateProbability(
            double probabilityPercent
    ) {
        if (!Double.isFinite(probabilityPercent)) {
            log.warn(
                    "Invalid precipitation probability: "
                            + "value={}, reason=NON_FINITE_VALUE",
                    probabilityPercent
            );

            throw new IllegalArgumentException(
                    "Precipitation probability must be finite."
            );
        }

        if (probabilityPercent < 0
                || probabilityPercent > 100) {

            log.warn(
                    "Invalid precipitation probability: "
                            + "value={}, minimumAllowed=0, "
                            + "maximumAllowed=100, reason=OUT_OF_RANGE",
                    probabilityPercent
            );

            throw new IllegalArgumentException(
                    "Precipitation probability must be between 0 and 100."
            );
        }

        log.trace(
                "Precipitation probability validated: "
                        + "probabilityPercent={}",
                probabilityPercent
        );
    }

    private PrecipitationRisk determineRisk(
            double probabilityPercent
    ) {
        if (probabilityPercent < 30) {
            log.trace(
                    "Precipitation probability matched LOW range: "
                            + "probabilityPercent={}, range=[0,30)",
                    probabilityPercent
            );

            return PrecipitationRisk.LOW;
        }

        if (probabilityPercent < 60) {
            log.trace(
                    "Precipitation probability matched MODERATE range: "
                            + "probabilityPercent={}, range=[30,60)",
                    probabilityPercent
            );

            return PrecipitationRisk.MODERATE;
        }

        if (probabilityPercent < 80) {
            log.trace(
                    "Precipitation probability matched HIGH range: "
                            + "probabilityPercent={}, range=[60,80)",
                    probabilityPercent
            );

            return PrecipitationRisk.HIGH;
        }

        log.trace(
                "Precipitation probability matched VERY_HIGH range: "
                        + "probabilityPercent={}, range=[80,100]",
                probabilityPercent
        );

        return PrecipitationRisk.VERY_HIGH;
    }
}