package enums;

import java.util.HashMap;
import java.util.Map;

public enum LogLevel {
    // various instances associated with integers or not
    NONE(0),LOW(1),HIGH(2), SUPERHIGH(3),DEFAULT;
    // int value
    private int value;
    // empty constructor for default value
    LogLevel() {}
    // constructor with value
    LogLevel(int value) {
        this.value = value;
    }
    // getter for value
    public int getValue() {
        return value;
    }
    // utility method to retrieve instance by int value
    public static LogLevel forValue(int value) {
        // iterating values
        for (LogLevel n: values()) {
            // matches argument
            if (n.getValue() == value) return n;
        }
        // no match, returning DEFAULT
        return DEFAULT;
    }
}