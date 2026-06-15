package com.weatherbridge.infrastructure.web;

import com.weatherbridge.infrastructure.properties.CorrelationIdProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter
        extends OncePerRequestFilter {

    private static final String MDC_KEY =
            "correlationId";

    private static final int MAXIMUM_LENGTH = 100;

    private final CorrelationIdProperties properties;

    public CorrelationIdFilter(
            CorrelationIdProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId =
                resolveCorrelationId(request);

        MDC.put(
                MDC_KEY,
                correlationId
        );

        response.setHeader(
                properties.responseHeader(),
                correlationId
        );

        try {
            filterChain.doFilter(
                    request,
                    response
            );
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(
            HttpServletRequest request
    ) {
        String headerValue = request.getHeader(
                properties.requestHeader()
        );

        if (headerValue == null
                || headerValue.isBlank()) {

            return UUID.randomUUID().toString();
        }

        String normalized = headerValue.trim();

        if (normalized.length() > MAXIMUM_LENGTH) {
            return UUID.randomUUID().toString();
        }

        if (!normalized.matches("[a-zA-Z0-9._:-]+")) {
            return UUID.randomUUID().toString();
        }

        return normalized;
    }
}