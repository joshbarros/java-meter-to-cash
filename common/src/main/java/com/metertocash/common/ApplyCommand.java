package com.metertocash.common;

public record ApplyCommand(String billId, String requestId, int amountCents) {
}
