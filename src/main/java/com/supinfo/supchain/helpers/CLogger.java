package com.supinfo.supchain.helpers;

import com.supinfo.supchain.enums.LogLevel;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.supinfo.supchain.helpers.ConsoleColors.*;


public class CLogger {

    private String classCaller;

    private Map<LogLevel, String> coloredPrefix = new HashMap<LogLevel, String>() {{
        put(LogLevel.EXCEPTION, RED_UNDERLINED);
        put(LogLevel.LOW, YELLOW);
        put(LogLevel.HIGH, BLUE);
        put(LogLevel.SUPERHIGH, CYAN);
        put(LogLevel.SUPERDUPERHIGH, PURPLE);
    }};

    public CLogger(Class classCaller){
        this.classCaller = classCaller.getName();
    }

    public void log(LogLevel logLevel, String text){

        if(RUtils.logLevel.getValue() >= logLevel.getValue()){
            System.out.println(beautify(logLevel,text));
        }

    }

    public void println(String text){
        System.out.println(WHITE_BACKGROUND_BRIGHT+BLACK_BOLD+"SUPCOIN >>> "+RESET + text);
    }
    public void printInput(String text){
        println(text);
        System.out.print(WHITE_BACKGROUND_BRIGHT+BLACK_BOLD+"SUPCOIN >>> "+RESET);
    }

    private String beautify(LogLevel logLevel,String text){
        Date date = new Date();
        Timestamp ts = new Timestamp(date.getTime());
        return (coloredPrefix.get(logLevel) +ts + " [["+logLevel+"]] " + classCaller +" : "+ text + RESET);
    }
}
