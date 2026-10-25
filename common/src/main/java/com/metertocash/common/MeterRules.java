package com.metertocash.common;

import java.util.Locale;

public final class MeterRules {
    private MeterRules() {
    }

    public static int consumption(int previous, int reading) {
        if (reading < previous) {
            throw new IllegalArgumentException("reading behind the dial");
        }
        return reading - previous;
    }

    public static int charge(int consumption, int rateCents) {
        return Math.multiplyExact(consumption, rateCents);
    }

    public static int adjustment(int previous, int estimatedRead, int actualRead, int rateCents) {
        int estimated = consumption(previous, estimatedRead);
        int actual = consumption(previous, actualRead);
        return Math.multiplyExact(actual - estimated, rateCents);
    }

    public static int openAfter(int amountCents, int deltaCents) {
        int next = Math.addExact(amountCents, deltaCents);
        if (next < 0) {
            throw new IllegalArgumentException("open below zero");
        }
        return next;
    }

    public static boolean estimatedRoute(String route) {
        return route != null && route.toLowerCase(Locale.ROOT).contains("estimate");
    }

    public static boolean declinedToken(String token) {
        return token != null && token.endsWith("0000");
    }

    public static boolean sealedMeter(String token) {
        return token != null && token.toLowerCase(Locale.ROOT).contains("sealed");
    }
}
