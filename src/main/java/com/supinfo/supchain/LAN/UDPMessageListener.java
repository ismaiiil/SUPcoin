package com.supinfo.supchain.LAN;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.Role;
import com.supinfo.supchain.enums.UDPMessage;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;


public class UDPMessageListener implements Runnable {

    private DatagramSocket socket;
    private List<String> adapterAddresses =  new ArrayList<>();

    private CLogger cLogger = new CLogger(this.getClass());

    @Override
    public void run() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(RUtils.udpPort, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            //Check if packet is from localhost ignore it if it is a loopback

            Enumeration<NetworkInterface> myints = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(myints)){
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    adapterAddresses.add(inetAddress.getHostAddress());
                }

            }
            cLogger.log(LogLevel.HIGH,"Ready to receive packets!");
            while (true) {

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);
                String packetAddress = packet.getAddress().getHostAddress();
                if(!adapterAddresses.contains(packet.getAddress().getHostAddress())){
                    //Packet received

                    cLogger.log(LogLevel.HIGH, "packet received from: " + packetAddress);
                    cLogger.log(LogLevel.HIGH,"data received: " + new String(packet.getData()));


                    //See if the packet holds the right command (message)
                    String message = new String(packet.getData()).trim();
                    if(RUtils.myRole == Role.RDV){
                        switch (UDPMessage.valueOf(message)){
                            case DISCOVER_RDV_REQUEST:
                                byte[] sendData = UDPMessage.DISCOVER_RDV_RESPONSE.toString().getBytes();
                                //Send a response
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                                socket.send(sendPacket);
                                cLogger.log(LogLevel.HIGH, "Sent packet to: " + sendPacket.getAddress().getHostAddress());
                                break;
                            case CONFIRM_RDV_REQUEST:
                                RUtils.localClientAddresses.add(packetAddress);
                                cLogger.log(LogLevel.LOW,"all current EDGEs connected to this RDV node are:" + RUtils.localClientAddresses.toString());
                                break;
                        }
                    }

                }

            }
        } catch (IOException ex) {
            Logger.getLogger(UDPMessageListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static UDPMessageListener getInstance() {
        return UDPMessageListenerHolder.INSTANCE;
    }

    private static class UDPMessageListenerHolder {

        private static final UDPMessageListener INSTANCE = new UDPMessageListener();
    }

}