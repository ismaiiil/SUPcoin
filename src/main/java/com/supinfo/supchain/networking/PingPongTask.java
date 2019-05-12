package com.supinfo.supchain.networking;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.models.TCPMessage;

import java.util.TimerTask;

public class PingPongTask extends TimerTask {
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run() {
        cLogger.log(LogLevel.HIGH,"Running PingPongTask task");
        TCPUtils.waitPingPong();
        //first try to connect to the closest peers using a REQUEST_CONNECTION
        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
            TCPUtils.multicastRDVs(new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,false,0,null),RUtils.externalIP);
        }
        TCPUtils.waitPingPong();
        //if after the latency delay the minimum number of connections are still not satisfied, ONLY THEN will we connect the bootnode
        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
            TCPUtils.unicast(new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,false,0,null),RUtils.bootstrapNode);
        }
        cLogger.log(LogLevel.HIGH,"finished PingPongTask task");
    }

}
