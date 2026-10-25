package com.metertocash.common;

public record OpenBalanceCommand(String billId, int amountCents) {
}
