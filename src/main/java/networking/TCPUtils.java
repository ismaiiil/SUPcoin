package networking;

import enums.LogLevel;
import helpers.CLogger;
import helpers.RUtils;
import models.TCPMessage;

public class TCPUtils {
    public static void multicast(TCPMessage tcpMessage, String origin){
        if(!RUtils.isMessageCached(tcpMessage)){
            for (String ipadd: RUtils.allClientAddresses()) {
                if(!ipadd.equals(origin)){
                    CLogger.print(LogLevel.LOW," sending message to: >>" + tcpMessage.getTcpMessageType().toString() +" to "+ ipadd);
                    TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,ipadd,RUtils.tcpPort);
                    tcpMessageEmmiter.start();
                }
            }
        }else{
            CLogger.print(LogLevel.LOW,"this TCP message has already been sent from this node dropping it.");
        }
        RUtils.addMessageToCache(tcpMessage);
    }
    public static void unicast(TCPMessage tcpMessage,String destination){
        TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,destination,RUtils.tcpPort);
        tcpMessageEmmiter.start();
    }
}
