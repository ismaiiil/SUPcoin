package com.supinfo.supchain.networking;

import com.dosse.upnp.UPnP;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;

public class UPnPManager implements Runnable {
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run() {
        cLogger.log(LogLevel.SUPERDUPERHIGH,"Attempting UPnP port forwarding...");
        if (UPnP.isUPnPAvailable()) { //is UPnP available?
            if (UPnP.isMappedTCP(RUtils.tcpPort)) { //is the port already mapped?
                cLogger.log(LogLevel.SUPERDUPERHIGH,"UPnP port forwarding not enabled: port is already mapped");
            } else if (UPnP.openPortTCP(RUtils.tcpPort)) { //try to map port
                cLogger.log(LogLevel.SUPERDUPERHIGH,"UPnP port forwarding enabled");
            } else {
                cLogger.log(LogLevel.SUPERDUPERHIGH,"UPnP port forwarding failed");
            }
        } else {
            cLogger.log(LogLevel.SUPERDUPERHIGH,"UPnP is not available");
        }
    }
}
