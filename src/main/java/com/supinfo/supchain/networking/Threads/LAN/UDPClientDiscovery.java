package com.supinfo.supchain.networking.Threads.LAN;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.UDPMessage;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

public class UDPClientDiscovery implements Runnable {
    DatagramSocket c;
    int maxretries;

    private CLogger cLogger = new CLogger(this.getClass());

    public UDPClientDiscovery(int maxRetries) {
        this.maxretries = maxRetries;
    }

    @Override
    public void run() {

        try {
            //Open a random port to send the package
            c = new DatagramSocket();
            c.setBroadcast(true);

            byte[] sendData = UDPMessage.DISCOVER_RDV_REQUEST.toString().getBytes();


            // Broadcast the message over all the com.supinfo.supchain.networking.Threads.LAN interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

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
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, RUtils.udpPort);
                        c.send(sendPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    cLogger.log(LogLevel.NETWORK, "Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }


            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.setSoTimeout(10 * 1000);
            c.receive(receivePacket);

            //We have a response
            cLogger.log(LogLevel.NETWORK, "Broadcast response from server: " + receivePacket.getAddress().getHostAddress());
            //add RDV
            RUtils.localClientAddresses.add(receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals(UDPMessage.DISCOVER_RDV_RESPONSE.toString())) {
                cLogger.log(LogLevel.NETWORK, "got the response: " + message);
                //since the RDV was discovered properly we confirm the rdv that indeed we were able to discover it and have added its address to our RUtils class
                sendData = UDPMessage.CONFIRM_RDV_REQUEST.toString().getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), RUtils.udpPort);
                c.send(sendPacket);
            }

            //Close the port!
            c.close();


        } catch (SocketTimeoutException ex) {
            cLogger.log(LogLevel.NETWORK, " Timeout waiting for server answer");
            maxretries -= 1;
            if (maxretries > 0) {
                cLogger.log(LogLevel.NETWORK, "Retrying to reach server");
                this.run();
            } else {
                cLogger.log(LogLevel.NETWORK, "max retries reached stopping discovery");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


}
