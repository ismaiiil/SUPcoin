package com.supinfo.supchain.networking.Threads.LAN;

import java.io.IOException;
import java.net.*;
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
            adapterAddresses = getAdapterAdresses();
            cLogger.log(LogLevel.NETWORK,"Ready to receive packets!");
            while (true) {

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);
                String packetAddress = packet.getAddress().getHostAddress();
                if(!adapterAddresses.contains(packet.getAddress().getHostAddress())){
                    //Packet received
                    cLogger.log(LogLevel.NETWORK, "packet received from: " + packetAddress);
                    //See if the packet holds the right command (message)
                    String message = new String(packet.getData()).trim();
                    if(RUtils.myRole == Role.RDV){
                        switch (UDPMessage.valueOf(message)){
                            case DISCOVER_RDV_REQUEST:
                                byte[] sendData = UDPMessage.DISCOVER_RDV_RESPONSE.toString().getBytes();
                                //Send a response
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                                socket.send(sendPacket);
                                cLogger.log(LogLevel.NETWORK, "Sent packet to: " + sendPacket.getAddress().getHostAddress());
                                break;
                            case CONFIRM_RDV_REQUEST:
                                RUtils.localClientAddresses.add(packetAddress);
                                cLogger.log(LogLevel.NETWORK,"all current EDGEs connected to this RDV node are:" + RUtils.localClientAddresses.toString());
                                break;
                        }
                    }

                }

            }
        } catch (IOException ex) {
            Logger.getLogger(UDPMessageListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static List<String> getAdapterAdresses() throws SocketException {
        List<String> adapterAddresses = new ArrayList<>();
        Enumeration<NetworkInterface> myints = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(myints)){
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                adapterAddresses.add(inetAddress.getHostAddress());
            }
        }
        return adapterAddresses;
    }

    public static UDPMessageListener getInstance() {
        return UDPMessageListenerHolder.INSTANCE;
    }

    private static class UDPMessageListenerHolder {

        private static final UDPMessageListener INSTANCE = new UDPMessageListener();
    }

}