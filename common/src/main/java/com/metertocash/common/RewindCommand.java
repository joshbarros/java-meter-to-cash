package com.metertocash.common;

public record RewindCommand(String billId, String meterId, int previous, int next) {
}
