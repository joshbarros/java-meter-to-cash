package com.metertocash.common;

public record PaymentEvent(String billId, String requestId, String status) {
}
