package helpers;

import enums.LogLevel;

public class CLogger {

    //set this before program starts as desired
    public static LogLevel logLevel;

    public static void print(LogLevel logLevel,String text){
        if(CLogger.logLevel == LogLevel.LOW && logLevel == LogLevel.LOW){
            System.out.println(text);
        }
        if(CLogger.logLevel == LogLevel.HIGH && logLevel == LogLevel.HIGH){
            System.out.println(text);
        }

    }
}
