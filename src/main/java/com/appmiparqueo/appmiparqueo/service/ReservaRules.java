package com.appmiparqueo.appmiparqueo.service;

import java.time.Duration;
import java.time.LocalDateTime;

public final class ReservaRules {
    private static final Duration MAX_DURATION = Duration.ofHours(24);

    private ReservaRules() {
    }

    public static boolean overlaps(LocalDateTime start, LocalDateTime end,
            LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    public static void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)
                || Duration.between(start, end).compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("La reserva debe tener un rango válido de hasta 24 horas.");
        }
    }
}