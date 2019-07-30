package com.supinfo.supchain.networking.Utils;

import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.shared.Utils.StringUtil;
import com.supinfo.supchain.enums.Environment;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.ConfigManager;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.networking.Tasks.ExternalIPCheckTask;
import com.supinfo.supchain.networking.Threads.ExternalIPGet;
import com.supinfo.supchain.networking.Threads.LAN.UDPMessageListener;
import com.supinfo.supchain.networking.Threads.TCPMessageEmmiter;
import com.supinfo.supchain.networking.Threads.UPnPManager;

import java.net.SocketException;
import java.util.List;
import java.util.Timer;

public class TCPUtils {

    private static CLogger cLogger = new CLogger(TCPUtils.class);

    public static void multicastAll(TCPMessage tcpMessage, String origin) {
        CLogger cLogger = new CLogger(TCPUtils.class);
        if (!RUtils.isMessageCached(tcpMessage)) {
            for (String ipadd : RUtils.allClientAddresses()) {
                if (!ipadd.equals(origin)) {
                    cLogger.log(LogLevel.NETWORK, " MulticastAll message to: >>" + tcpMessage.getTcpMessageType().toString() + " to " + ipadd);
                    TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage, ipadd, RUtils.tcpPort);
                    tcpMessageEmmiter.start();
                }
            }
        } else {
            cLogger.log(LogLevel.BASIC, "this TCP message has already been sent from this node dropping it." + tcpMessage.getMessageHash());
        }
        RUtils.addMessageToCache(tcpMessage);
    }

    public static void multicastRDVs(TCPMessage tcpMessage, String origin) {
        CLogger cLogger = new CLogger(TCPUtils.class);
        if (!RUtils.isMessageCached(tcpMessage)) {
            for (String ipadd : RUtils.externalClientAddresses) {
                if (!ipadd.equals(origin)) {
                    cLogger.log(LogLevel.NETWORK, " MulticastRDV message to: >>" + tcpMessage.getTcpMessageType().toString() + " to " + ipadd);
                    TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage, ipadd, RUtils.tcpPort);
                    tcpMessageEmmiter.start();
                }
            }
        } else {
            cLogger.log(LogLevel.NETWORK, "this TCP message has already been sent from this node dropping it." + tcpMessage.getMessageHash());
        }
        RUtils.addMessageToCache(tcpMessage);
    }

    public static void unicast(TCPMessage tcpMessage, String destination) {
        TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage, destination, RUtils.tcpPort);
        tcpMessageEmmiter.start();
    }

    public static void startRDVRoutines() throws InterruptedException {

        ExternalIPGet externalIPGet = new ExternalIPGet();
        externalIPGet.run();
        externalIPGet.join();

        Timer time = new Timer(); // Instantiate Timer Object
        ExternalIPCheckTask ppt = new ExternalIPCheckTask(); // Instantiate SheduledTask class
        time.schedule(ppt, RUtils.externalIpCheckPeriod, RUtils.externalIpCheckPeriod);

        if (RUtils.env == Environment.PRODUCTION) {

            Thread uPnPManagerThread = new Thread(new UPnPManager());
            uPnPManagerThread.start();

        } else {
            cLogger.log(LogLevel.BASIC, "DEBUG MODE using IP from debugAPI file: " + RUtils.externalIP);
        }

        Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
        discoveryThread.start();
    }

    public static void connectToNode(String node) throws SocketException {
        List<String> adapterAddresses = UDPMessageListener.getAdapterAdresses();
        if (!node.equals(RUtils.externalIP) && !adapterAddresses.contains(node)) {
            if (StringUtil.isValidIP(node) && StringUtil.isValidIP(RUtils.externalIP)) {
                cLogger.println("We are now contacting a node!");
                TCPMessage requestMessage = new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION, null);
                TCPUtils.unicast(requestMessage, node);
            } else {
                cLogger.println("Please make sure the IP supplied and external IP is valid!");
                ConfigManager.saveConfig();
                System.exit(1);
            }

        } else {
            if (RUtils.externalIP.equals(node)) {
                cLogger.println("You have setup the bootnode to be this node, waiting and listening for other nodes");
            }
        }
    }

}
