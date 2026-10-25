package com.metertocash.common;

public record ActualCommand(String billId, int previous, int estimatedRead, int readValue) {
}
