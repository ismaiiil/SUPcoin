package helpers;

import enums.LogLevel;

import java.sql.Timestamp;
import java.util.Date;

public class CLogger {
    private String classCaller;

    public CLogger(Class classCaller){
        this.classCaller = classCaller.getName();
    }

    public void print(LogLevel logLevel,String text){

        if(RUtils.logLevel.getValue() >= logLevel.getValue()){
            System.out.println(beautify(logLevel,text));
        }

    }

    private String beautify(LogLevel logLevel,String text){
        Date date = new Date();
        Timestamp ts = new Timestamp(date.getTime());
        return (ts + " [["+logLevel+"]] " + classCaller +" : "+ text);
    }
}
