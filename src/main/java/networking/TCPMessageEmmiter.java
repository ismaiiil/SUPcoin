package networking;

import enums.LogLevel;
import enums.TCPMessageType;
import helpers.CLogger;
import models.TCPMessage;

import java.io.*;
import java.net.Socket;

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
//            cLogger.print(LogLevel.SUPERHIGH, "Successfully sent message and server responded with a: " + tcpMessage.getTcpMessageType());
//
//
//            objectInputStream.close();
//
//            if(tcpMessage.getTcpMessageType() == TCPMessageType.CLOSE_SOCKET) {
//                socket.close();
//                cLogger.print(LogLevel.SUPERHIGH, "TCPEmmiter has sent its message, now closing the socket based on response: " + tcpMessage.getTcpMessageType());
//
//            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
