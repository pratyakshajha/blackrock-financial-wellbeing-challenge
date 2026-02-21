package com.example.controller;

import com.example.dto.PerformanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

@RestController
public class PerformanceController {

    private static final double MEGABYTE_DIVISOR = 1024.0 * 1024.0;
    private final MeterRegistry meterRegistry;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.of("UTC"));

    @Autowired
    public PerformanceController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/performance")
    public PerformanceResponse getPerformanceMetrics() {
        return PerformanceResponse.builder()
                .time(getFormattedUptime())
                .memory(getFormattedHeapMemory())
                .threads(getLiveThreadCount())
                .build();
    }

    /**
     * Retrieves and formats the application uptime from the Actuator MeterRegistry.
     * @return Uptime formatted as yyyy-MM-dd HH:mm:ss.SSS.
     */
    private String getFormattedUptime() {
        // "process.uptime" directly gives the duration in seconds, which is more direct
        // and less error-prone than calculating from "process.start.time".
        var uptimeGauge = meterRegistry.find("process.uptime").timeGauge();
        if (uptimeGauge == null) {
            return "N/A";
        }
        // Get the uptime in milliseconds.
        long uptimeMillis = (long) uptimeGauge.value(TimeUnit.MILLISECONDS);

        // Format this duration as an Instant starting from the epoch to get the "1970-01-01" date part.
        return FORMATTER.format(Instant.ofEpochMilli(uptimeMillis));
    }

    /**
     * Retrieves and formats the used heap memory from the Actuator MeterRegistry.
     * @return Used heap memory formatted as "XXX.XX MB".
     */
    private String getFormattedHeapMemory() {
        var gauge = meterRegistry.find("jvm.memory.used").tag("area", "heap").gauge();
        if (gauge == null) {
            return "N/A";
        }
        double usedMemoryMb = gauge.value() / MEGABYTE_DIVISOR;
        return String.format("%.2f MB", usedMemoryMb);
    }

    /**
     * Retrieves the live thread count from the Actuator MeterRegistry.
     * @return The current number of live threads.
     */
    private int getLiveThreadCount() {
        var gauge = meterRegistry.find("jvm.threads.live").gauge();
        return gauge != null ? (int) gauge.value() : 0;
    }
}