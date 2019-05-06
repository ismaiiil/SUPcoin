package com.supinfo.supchain.enums;

import com.supinfo.supchain.enums.EnumAdapters.LogLevelAdapter;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(LogLevelAdapter.class)
public enum LogLevel {
    EXCEPTION(-1),
    NONE(0),
    LOW(1),
    HIGH(2),
    SUPERHIGH(3),
    SUPERDUPERHIGH(4),
    DEFAULT;
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