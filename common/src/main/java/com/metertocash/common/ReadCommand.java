package com.metertocash.common;

public record ReadCommand(String billId, int previous, int readValue, String route) {
}
