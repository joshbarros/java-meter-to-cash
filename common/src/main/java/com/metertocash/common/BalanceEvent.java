package com.metertocash.common;

public record BalanceEvent(String billId, String requestId, int openCents, String status) {
}
