package com.example.springboot4demo.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DbReadMetrics {

    private final MeterRegistry meterRegistry;

    public <T> T record(String service, String repository, String operation, Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return supplier.get();
        } finally {
            Counter.builder("app.db.reads.total")
                    .description("Total DB read operations")
                    .tag("service", service)
                    .tag("repository", repository)
                    .tag("operation", operation)
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder("app.db.read.duration")
                    .description("DB read duration")
                    .publishPercentileHistogram()
                    .tag("service", service)
                    .tag("repository", repository)
                    .tag("operation", operation)
                    .register(meterRegistry));
        }
    }
}

