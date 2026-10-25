package com.metertocash.common;

public record TenderCommand(String billId, String requestId, int amountCents, String payToken) {
}
