package com.metertocash.common;

public record ReadingEvent(String billId, int consumption, int readValue, String status) {
}
