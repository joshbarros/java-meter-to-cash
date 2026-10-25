package com.metertocash.common;

public record AdvanceEvent(String billId, int lastRead, String status) {
}
