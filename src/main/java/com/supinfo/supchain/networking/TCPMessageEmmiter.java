package com.supinfo.supchain.networking;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.shared.TCPMessage;

import java.io.*;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPMessageEmmiter extends Thread {
    String hostname;
    TCPMessage tcpMessage;
    int port;
    Socket socket;

    private CLogger cLogger = new CLogger(this.getClass());

    public TCPMessageEmmiter(TCPMessage tcpMessage, String hostname, int port){
        this.tcpMessage = tcpMessage;
        this.hostname = hostname;
        this.port = port;
    }
    @Override
    public void run() {
        if(!hostname.equals(RUtils.externalIP) && TCPUtils.isValidIP(hostname)){
            try {
                socket = new Socket(hostname, port);
                OutputStream outputStream = socket.getOutputStream();
                // create an object output stream from the output stream so we can send an object through it
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(tcpMessage);
                objectOutputStream.flush();
                objectOutputStream.close();

//            //TESTING WALLET RECEIVING SERVER RESPONSE IN THE SOCKET ITSELF INSTEAD OF A NEW SOCKET RESPONSE
//            InputStream inputStream = socket.getInputStream();
//            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
//            TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
//            cLogger.log(LogLevel.NETWORK, "Successfully sent message and server responded with a: " + tcpMessage.getTcpMessageType());
//
//
//            objectInputStream.close();
//
//            if(tcpMessage.getTcpMessageType() == TCPMessageType.CLOSE_SOCKET) {
//                socket.close();
//                cLogger.log(LogLevel.NETWORK, "TCPEmmiter has sent its message, now closing the socket based on response: " + tcpMessage.getTcpMessageType());
//
//            }

                socket.close();
            } catch (UnknownHostException e){
                cLogger.log(LogLevel.EXCEPTION,e.toString() + ", You may have input an invalid IP");
            } catch (ConnectException e){
                cLogger.log(LogLevel.NETWORK,"Address: "+ hostname + " is unreachable!");
                //this is liekly a connection timeout when we try to reach a dead IP, in the case of which we start a checknodes
                //thread in the background to see if all nodes are alive, and take proper action
            } catch (NoRouteToHostException e){
                //do nothing PingPongTask will handle all unreachable hosts
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            cLogger.log(LogLevel.NETWORK,"Invalid IP supplied(self IP or not an IP)!");
        }

    }
}
