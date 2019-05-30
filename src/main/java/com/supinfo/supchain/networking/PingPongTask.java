package com.supinfo.supchain.networking;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.shared.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.shared.TCPMessage;

import java.util.TimerTask;

public class PingPongTask extends TimerTask {
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run() {
        cLogger.log(LogLevel.NETWORK,"Running PingPongTask task");
        PingPongThread ppthread1 = new PingPongThread();
        ppthread1.start();
        try {
            ppthread1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //first try to connect to the closest peers using a REQUEST_CONNECTION
        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
            TCPUtils.multicastRDVs(new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,false,0,null),RUtils.externalIP);
        }

        PingPongThread ppthread2 = new PingPongThread();
        ppthread2.start();
        try {
            ppthread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //if after the latency delay the minimum number of connections are still not satisfied, ONLY THEN will we connect the bootnode
        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
            TCPUtils.unicast(new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,false,0,null),RUtils.bootstrapNode);
        }

        cLogger.log(LogLevel.NETWORK,"finished PingPongTask task");
    }

}
