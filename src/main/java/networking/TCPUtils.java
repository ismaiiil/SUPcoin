package networking;

import enums.LogLevel;
import helpers.CLogger;
import helpers.RUtils;
import models.TCPMessage;

public class TCPUtils {

    public static void multicastAll(TCPMessage tcpMessage, String origin){
        CLogger cLogger = new CLogger(TCPUtils.class);
        if(!RUtils.isMessageCached(tcpMessage)){
            for (String ipadd: RUtils.allClientAddresses()) {
                if(!ipadd.equals(origin)){
                    cLogger.log(LogLevel.LOW," MulticastAll message to: >>" + tcpMessage.getTcpMessageType().toString() +" to "+ ipadd);
                    TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,ipadd,RUtils.tcpPort);
                    tcpMessageEmmiter.start();
                }
            }
        }else{
            cLogger.log(LogLevel.LOW,"this TCP message has already been sent from this node dropping it." + tcpMessage.getMessageHash());
        }
        RUtils.addMessageToCache(tcpMessage);
    }

    public static void multicastRDVs(TCPMessage tcpMessage, String origin){
        CLogger cLogger = new CLogger(TCPUtils.class);
        if(!RUtils.isMessageCached(tcpMessage)){
            for (String ipadd: RUtils.externalClientAddresses) {
                if(!ipadd.equals(origin)){
                    cLogger.log(LogLevel.LOW," MulticastRDV message to: >>" + tcpMessage.getTcpMessageType().toString() +" to "+ ipadd);
                    TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,ipadd,RUtils.tcpPort);
                    tcpMessageEmmiter.start();
                }
            }
        }else{
            cLogger.log(LogLevel.LOW,"this TCP message has already been sent from this node dropping it." + tcpMessage.getMessageHash());
        }
        RUtils.addMessageToCache(tcpMessage);
    }

    public static void unicast(TCPMessage tcpMessage,String destination){
        TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,destination,RUtils.tcpPort);
        tcpMessageEmmiter.start();
    }
}
