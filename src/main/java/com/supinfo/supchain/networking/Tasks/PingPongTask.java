package com.supinfo.supchain.networking.Tasks;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.supchain.networking.Utils.TCPUtils;
import com.supinfo.supchain.networking.Threads.PingPongThread;

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

        //first try to connect to the bootnode( we always build the network around the bootnode(s) )
        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
            TCPUtils.unicast(new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,null),RUtils.bootstrapNode);
        }

        cLogger.log(LogLevel.NETWORK,"finished PingPongTask task");
    }

}
