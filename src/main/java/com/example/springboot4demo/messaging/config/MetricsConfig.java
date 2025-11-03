package com.example.springboot4demo.messaging.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Getter
@Component
@RequiredArgsConstructor
public class MetricsConfig {
    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> successCountersByPath = new ConcurrentHashMap<>();
    private final Map<String, Counter> unauthorizedCountersByPath = new ConcurrentHashMap<>();

    public void incrementSuccess(String path) {
        successCountersByPath.computeIfAbsent(normalizePath(path), p ->
                Counter.builder("success")
                        .tag("path", p)
                        .register(meterRegistry)
        ).increment();
    }

    public void incrementUnauthorized(String path) {
        unauthorizedCountersByPath.computeIfAbsent(normalizePath(path), p ->
                Counter.builder("unauthorized")
                        .tag("path", p)
                        .register(meterRegistry)
        ).increment();
    }



private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        Pattern number = Pattern.compile("\\d+");
        Pattern uuid = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        String normalized = Arrays.stream(path.split("/"))
                .filter(part -> !part.isEmpty())
                .map(part -> (number.matcher(part).matches() || uuid.matcher(part).matches()) ? "{id}" : part)
                .collect(java.util.stream.Collectors.joining("/", "/", ""));
        return normalized.isEmpty() ? "/" : normalized;
    }

}
