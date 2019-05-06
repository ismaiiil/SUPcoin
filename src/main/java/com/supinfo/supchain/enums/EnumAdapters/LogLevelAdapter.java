package com.supinfo.supchain.enums.EnumAdapters;

import com.supinfo.supchain.enums.LogLevel;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LogLevelAdapter extends XmlAdapter<String, LogLevel> {

    @Override
    public String marshal(LogLevel logLevel) throws Exception {
        return logLevel.name();
    }

    @Override
    public LogLevel unmarshal(String string) throws Exception {
        try {
            return LogLevel.valueOf(string);
        } catch(Exception e) {
            throw new JAXBException(e);
        }
    }

}
