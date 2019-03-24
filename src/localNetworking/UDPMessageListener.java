package localNetworking;

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
import enums.LogLevel;
import enums.Role;
import enums.UDPMessage;
import helpers.CLogger;
import helpers.R;


public class UDPMessageListener implements Runnable {

    DatagramSocket socket;
    List<String> localaddresses =  new ArrayList<>();

    @Override
    public void run() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            //Check if packet is from localhost ignore it if it is a loopback

            Enumeration<NetworkInterface> myints = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(myints)){
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    localaddresses.add(inetAddress.getHostAddress());
                }

            }
            CLogger.print(LogLevel.HIGH,getClass().getName() + ">>>Ready to receive packets!");
            while (true) {

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);
                String packetAddress = packet.getAddress().getHostAddress();
                if(!localaddresses.contains(packet.getAddress().getHostAddress())){
                    //Packet received

                    CLogger.print(LogLevel.HIGH,getClass().getName() + ">>>packet received from: " + packetAddress);
                    CLogger.print(LogLevel.HIGH,getClass().getName() + ">>>data received: " + new String(packet.getData()));


                    //See if the packet holds the right command (message)
                    String message = new String(packet.getData()).trim();
                    if(R.myRole == Role.RDV){
                        switch (UDPMessage.valueOf(message)){
                            case DISCOVER_RDV_REQUEST:
                                byte[] sendData = UDPMessage.DISCOVER_RDV_RESPONSE.toString().getBytes();
                                //Send a response
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                                socket.send(sendPacket);
                                CLogger.print(LogLevel.HIGH,getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                                break;
                            case CONFIRM_RDV_REQUEST:
                                if(!R.ClientAddreses.contains(packetAddress)){
                                    R.ClientAddreses.add(packetAddress);
                                    CLogger.print(LogLevel.LOW,getClass().getName() + "all current EDGEs connected to this RDV node are:" + R.ClientAddreses.toString());
                                }
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