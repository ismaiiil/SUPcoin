package com.supinfo.supchain.networking.Threads;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.networking.Utils.TCPUtils;
import com.supinfo.supchain.networking.models.PingPong;
import com.supinfo.shared.Network.TCPMessage;

import java.util.HashSet;

public class PingPongThread extends Thread{
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run() {
        TCPMessage pingMessage = new TCPMessage<>(TCPMessageType.PING,new PingPong(RUtils.externalIP));
        TCPUtils.multicastAll(pingMessage,RUtils.externalIP);
        RUtils.pingedAddresses = (HashSet<String>) RUtils.allClientAddresses().clone();
        try {
            sleep(RUtils.connectionLatency);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (String address:RUtils.pingedAddresses) {
            if (address != null) {
                cLogger.log(LogLevel.NETWORK,address + " unreachable, did not reply to pong after latency timeout, removing from cache");
                RUtils.externalClientAddresses.remove(address);
                RUtils.localClientAddresses.remove(address);
            }
        }
    }


}
