package com.metertocash.common;

public record AdvanceCommand(String billId, String meterId, int previous, int next) {
}
