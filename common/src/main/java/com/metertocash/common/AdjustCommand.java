package com.metertocash.common;

public record AdjustCommand(String billId, int previous, int estimatedRead, int readValue, int rateCents) {
}
