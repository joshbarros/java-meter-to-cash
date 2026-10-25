package com.metertocash.common;

public record ChargeEvent(String billId, int amountCents, String status) {
}
