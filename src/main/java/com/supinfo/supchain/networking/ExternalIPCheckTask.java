package com.supinfo.supchain.networking;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.ExternalIPGet;

import java.util.Date;
import java.util.TimerTask;

public class ExternalIPCheckTask extends TimerTask {
    private CLogger cLogger = new CLogger(this.getClass());
    // Add your task here
    public void run() {
        cLogger.log(LogLevel.HIGH,"Running ExternalIPCheckTask!");
        ExternalIPGet externalIPGet = new ExternalIPGet();
        externalIPGet.run();
        try {
            externalIPGet.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cLogger.log(LogLevel.HIGH,"Finished Running ExternalIPCheckTask!");
    }
}