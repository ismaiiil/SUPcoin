package com.supinfo.supchain.networking.Threads;

import com.dosse.upnp.UPnP;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;

public class UPnPManager implements Runnable {
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run() {
        cLogger.log(LogLevel.NETWORK,"Attempting UPnP port forwarding...");
        if (UPnP.isUPnPAvailable()) { //is UPnP available?
            if (UPnP.isMappedTCP(RUtils.tcpPort)) { //is the port already mapped?
                cLogger.log(LogLevel.NETWORK,"UPnP port forwarding not enabled: port is already mapped");
            } else if (UPnP.openPortTCP(RUtils.tcpPort)) { //try to map port
                cLogger.log(LogLevel.NETWORK,"UPnP port forwarding enabled");
            } else {
                cLogger.log(LogLevel.NETWORK,"UPnP port forwarding failed");
            }
        } else {
            cLogger.log(LogLevel.NETWORK,"UPnP is not available");
        }
    }
}
