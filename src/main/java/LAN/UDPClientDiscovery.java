package LAN;

import enums.LogLevel;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

import enums.UDPMessage;
import helpers.CLogger;
import helpers.RUtils;

public class UDPClientDiscovery implements Runnable {
    DatagramSocket c;
    static String test;
    int maxretries;

    public UDPClientDiscovery(int maxRetries){
        this.maxretries = maxRetries;
    }

    @Override
    public void run() {

        try {
            //Open a random port to send the package
            c = new DatagramSocket();
            c.setBroadcast(true);

            byte[] sendData = UDPMessage.DISCOVER_RDV_REQUEST.toString().getBytes();


            // Broadcast the message over all the LAN interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        c.send(sendPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    CLogger.print(LogLevel.HIGH,getClass().getName() + " >>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            CLogger.print(LogLevel.HIGH,getClass().getName() + " >>> Done looping over all LAN interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.setSoTimeout(10*1000);
            c.receive(receivePacket);

            //We have a response
            CLogger.print(LogLevel.HIGH,getClass().getName() + " >>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());
            //add RDV
            RUtils.localClientAddresses.add(receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals(UDPMessage.DISCOVER_RDV_RESPONSE.toString())) {
                CLogger.print(LogLevel.HIGH,getClass().getName() + " got the response: "+ message);
                //since the RDV was discovered properly we confirm the rdv that indeed we were able to discover it and have added its address to our RUtils class
                sendData = UDPMessage.CONFIRM_RDV_REQUEST.toString().getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), 8888);
                c.send(sendPacket);
            }

            //Close the port!
            c.close();


        } catch (SocketTimeoutException ex){
            CLogger.print(LogLevel.LOW,getClass().getName() + " Timeout waiting for server answer");
            maxretries -= 1;
            if(maxretries > 0){
                CLogger.print(LogLevel.LOW,getClass().getName() + " Retrying to reach server");
                this.run();
            }else{
                CLogger.print(LogLevel.LOW,getClass().getName() + " max retries reached stopping discovery");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


}
