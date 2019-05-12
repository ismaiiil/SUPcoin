package com.supinfo.supchain.networking;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.models.PingPong;
import com.supinfo.supchain.models.TCPMessage;

import java.util.HashSet;

public class PingPongThread extends Thread{
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run() {
        TCPMessage pingMessage = new TCPMessage<>(TCPMessageType.PING,false,0, new PingPong(RUtils.externalIP));
        TCPUtils.multicastAll(pingMessage,RUtils.externalIP);
        RUtils.pingedAddresses = (HashSet<String>) RUtils.externalClientAddresses.clone();
        try {
            sleep(RUtils.connectionLatency);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (String address:RUtils.pingedAddresses) {
            if (address != null) {
                cLogger.log(LogLevel.HIGH,address + " unreachable, did not reply to pong after latency timeout, removing from cache");
                RUtils.externalClientAddresses.remove(address);
            }
        }
    }


}
