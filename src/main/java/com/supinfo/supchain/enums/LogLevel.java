package com.supinfo.supchain.enums;

public enum LogLevel {
    EXCEPTION(-1),NONE(0),LOW(1),HIGH(2), SUPERHIGH(3),SUPERDUPERHIGH(4),DEFAULT;
    private int value;

    LogLevel() {}

    LogLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LogLevel forValue(int value) {
        for (LogLevel n: values()) {
            if (n.getValue() == value) return n;
        }
        return DEFAULT;
    }
}