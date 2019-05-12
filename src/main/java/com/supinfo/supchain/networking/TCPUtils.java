package com.supinfo.supchain.networking;

import com.supinfo.supchain.LAN.UDPMessageListener;
import com.supinfo.supchain.enums.Environment;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.TCPMessageType;
import com.supinfo.supchain.helpers.*;
import com.supinfo.supchain.models.PingPong;
import com.supinfo.supchain.models.TCPMessage;

import java.net.SocketException;
import java.util.HashSet;
import java.util.List;

import static java.lang.Thread.sleep;

public class TCPUtils {

    private static CLogger cLogger = new CLogger(TCPUtils.class);

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

    public static void startRDVRoutines() throws InterruptedException {
        if(RUtils.env == Environment.PRODUCTION){

            Thread uPnPManagerThread = new Thread(new UPnPManager());
            uPnPManagerThread.start();

            ExternalIPGet externalIPGet = new ExternalIPGet();
            externalIPGet.run();
            externalIPGet.join();
            cLogger.log(LogLevel.LOW,"Public IP successfully retrieved: " + RUtils.externalIP);

            //start a thread that will monitor the external IP and take necessary actions
//            Timer time = new Timer(); // Instantiate Timer Object
//            ExternalIPCheckTask st = new ExternalIPCheckTask(); // Instantiate SheduledTask class
//            time.schedule(st, 0, 1000); // Create Repetitively task for every 1 secs
        }else{
            cLogger.log(LogLevel.LOW,"DEBUG MODE using IP from config file: " + RUtils.externalIP);
        }

        Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
        discoveryThread.start();
    }

    public static void pingPong() {
        TCPMessage pingMessage = new TCPMessage<>(TCPMessageType.PING,false,0, new PingPong(RUtils.externalIP));
        TCPUtils.multicastAll(pingMessage,RUtils.externalIP);
        RUtils.pingedAddresses = (HashSet<String>) RUtils.externalClientAddresses.clone();

        Thread thisThread = Thread.currentThread();
        int timeToRun = 5000; //TODO:include in RUtils and marshall it

        new Thread(() -> {
            try {
                sleep(timeToRun);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thisThread.interrupt();
        }).start();
        SpinnerCLI spinnerCLI = new SpinnerCLI("Checking cached nodes: ");
        spinnerCLI.start();
        while (!Thread.interrupted() || RUtils.pingedAddresses.size() == 0) {

        }
        spinnerCLI.showProgress = false;
        for (String address:RUtils.pingedAddresses) {
            if (address != null) {
                cLogger.log(LogLevel.HIGH,address + " unreachable, did not reply to pong after latency timeout, removing from cache");
                RUtils.externalClientAddresses.remove(address);
            }
        }
    }

    public static void connectToNode(String node) throws SocketException {
        List<String> adapterAddresses = UDPMessageListener.getAdapterAdresses();
        if(!node.equals(RUtils.externalIP) && !adapterAddresses.contains(node)){
            if(isValidIP(node) && isValidIP(RUtils.externalIP)){
                cLogger.println("We are now contacting a node!");
                TCPMessage requestMessage = new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,false,0,null);
                TCPUtils.unicast(requestMessage,node);
            }else{
                cLogger.println("Please make sure the bootnode or IP supplied and external IP is valid!");
                ConfigManager.saveConfig();
                System.exit(1);
            }

        }else{
            cLogger.println("You have setup the bootnode to be this node, waiting and listening for other nodes");
        }
    }

    public static boolean isValidIP(String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
