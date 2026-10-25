package com.metertocash.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MeterRulesTest {

    @Test
    void aReadingBehindTheDialIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MeterRules.consumption(1000, 999));
        assertEquals(0, MeterRules.consumption(1000, 1000));
        assertEquals(100, MeterRules.consumption(1000, 1100));
    }

    @Test
    void priceOverflowAndEstimateTrueUpStayInIntegers() {
        assertThrows(ArithmeticException.class, () -> MeterRules.charge(2, Integer.MAX_VALUE));
        assertEquals(1000, MeterRules.charge(100, 10));
        assertEquals(2000, MeterRules.charge(200, 10));
        assertEquals(-1000, MeterRules.adjustment(1000, 1200, 1100, 10));
        assertEquals(1000, MeterRules.openAfter(2000, -1000));
        assertThrows(IllegalArgumentException.class, () -> MeterRules.openAfter(100, -101));
        assertThrows(ArithmeticException.class, () -> MeterRules.openAfter(Integer.MAX_VALUE, 1));
    }

    @Test
    void sealedMetersAndDeclinedTendersAreExact() {
        assertTrue(MeterRules.sealedMeter("METER-SEALED"));
        assertTrue(MeterRules.sealedMeter("unsealed"));
        assertFalse(MeterRules.sealedMeter("METER-1"));
        assertFalse(MeterRules.sealedMeter("METER-OK"));
        assertTrue(MeterRules.declinedToken("PAY0000"));
        assertTrue(MeterRules.declinedToken("pay0000"));
        assertFalse(MeterRules.declinedToken("PAY0001"));
        assertTrue(MeterRules.estimatedRoute("ROUTE-ESTIMATE"));
        assertFalse(MeterRules.estimatedRoute("ROUTE-1"));
    }
}
