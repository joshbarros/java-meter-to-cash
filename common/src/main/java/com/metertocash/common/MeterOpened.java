package com.metertocash.common;

public record MeterOpened(String meterId, String premiseId, String serial, int lastRead) {
}
