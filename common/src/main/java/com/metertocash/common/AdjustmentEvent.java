package com.metertocash.common;

public record AdjustmentEvent(String billId, int deltaCents, String status) {
}
