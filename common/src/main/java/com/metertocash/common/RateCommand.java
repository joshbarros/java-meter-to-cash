package com.metertocash.common;

public record RateCommand(String billId, int consumption, int rateCents) {
}
