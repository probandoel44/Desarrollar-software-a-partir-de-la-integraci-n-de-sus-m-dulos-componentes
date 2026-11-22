package com.appmiparqueo.appmiparqueo.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReservaRulesTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 22, 10, 0);

    @Test
    void adjacentReservationsDoNotOverlap() {
        assertFalse(ReservaRules.overlaps(START, START.plusHours(1), START.plusHours(1), START.plusHours(2)));
    }

    @Test
    void crossingReservationsOverlap() {
        assertTrue(ReservaRules.overlaps(START, START.plusHours(2), START.plusHours(1), START.plusHours(3)));
    }

    @Test
    void acceptsMaximumDurationAndRejectsLongerPeriods() {
        ReservaRules.validatePeriod(START, START.plusHours(24));

        assertThrows(IllegalArgumentException.class,
                () -> ReservaRules.validatePeriod(START, START.plusHours(24).plusNanos(1)));
    }

    @Test
    void rejectsMissingOrNonPositivePeriods() {
        assertThrows(IllegalArgumentException.class, () -> ReservaRules.validatePeriod(null, START));
        assertThrows(IllegalArgumentException.class, () -> ReservaRules.validatePeriod(START, START));
        assertThrows(IllegalArgumentException.class,
                () -> ReservaRules.validatePeriod(START.plusHours(1), START));
    }
}