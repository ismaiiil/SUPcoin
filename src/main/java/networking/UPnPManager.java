package networking;

import com.dosse.upnp.UPnP;
import enums.LogLevel;
import helpers.CLogger;
import helpers.RUtils;

public class UPnPManager implements Runnable {
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run() {
        cLogger.print(LogLevel.HIGH,"Attempting UPnP port forwarding...");
        if (UPnP.isUPnPAvailable()) { //is UPnP available?
            if (UPnP.isMappedTCP(RUtils.tcpPort)) { //is the port already mapped?
                cLogger.print(LogLevel.HIGH,"UPnP port forwarding not enabled: port is already mapped");
            } else if (UPnP.openPortTCP(RUtils.tcpPort)) { //try to map port
                cLogger.print(LogLevel.HIGH,"UPnP port forwarding enabled");
            } else {
                cLogger.print(LogLevel.HIGH,"UPnP port forwarding failed");
            }
        } else {
            cLogger.print(LogLevel.HIGH,"UPnP is not available");
        }
    }
}
