package networking;

import helpers.R;
import models.TCPMessage;

public class TCPUtils {
    public static void multicast(TCPMessage tcpMessage, String origin){
        if(!R.cacheMessage.contains(tcpMessage.getMessageHash())){
            for (String ipadd: R.ClientAddreses) {
                if(!ipadd.equals(origin)){
                    System.out.println("propagating message: >>" + tcpMessage.getTcpMessageType().toString() +" to"+ ipadd);
                    TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,ipadd,8888);
                    tcpMessageEmmiter.start();
                }
            }
        }else{
            System.out.println("this message has already been sent from this node dropping it.");
        }
        R.cacheMessage.add(tcpMessage.getMessageHash());
    }
}
