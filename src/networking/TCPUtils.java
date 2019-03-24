package networking;

import enums.LogLevel;
import helpers.CLogger;
import helpers.R;
import models.TCPMessage;

public class TCPUtils {
    public static void multicast(TCPMessage tcpMessage, String origin){
        if(!R.cacheMessage.contains(tcpMessage.getMessageHash())){
            for (String ipadd: R.ClientAddreses) {
                if(!ipadd.equals(origin)){
                    CLogger.print(LogLevel.LOW," sending message to: >>" + tcpMessage.getTcpMessageType().toString() +" to "+ ipadd);
                    TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,ipadd,8888);
                    tcpMessageEmmiter.start();
                }
            }
        }else{
            CLogger.print(LogLevel.LOW,"this TCP message has already been sent from this node dropping it.");
        }
        R.cacheMessage.add(tcpMessage.getMessageHash());
    }
    public static void unicast(TCPMessage tcpMessage,String destination){
        TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,destination,8888);
        tcpMessageEmmiter.start();
    }
}
