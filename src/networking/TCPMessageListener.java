package networking;

import enums.LogLevel;
import helpers.CLogger;
import models.TCPMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static enums.LogLevel.HIGH;
import static enums.LogLevel.LOW;

public class TCPMessageListener extends Thread{
    private int port;
    private ServerSocket serverSocket;
    public TCPMessageListener(int port) {
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            try{
                CLogger.print(HIGH,getClass().getName() + "ServerSocket awaiting connections...");
                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                CLogger.print(HIGH,getClass().getName() + "Connection from " + socket + "!");
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                CLogger.print(LOW,"TCP connection from" + socket.getInetAddress().getHostAddress());
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                CLogger.print(LOW,getClass().getName() + "got the message" + tcpMessage.getTcpMessageType().toString() + "from" + socket.getInetAddress().getHostAddress());

                if(tcpMessage.isPropagatable()){
                    TCPUtils.multicast(tcpMessage,socket.getInetAddress().getHostAddress());
                }

                socket.close();
            }catch (IOException | ClassNotFoundException ex){
                ex.printStackTrace();
            }
        }



    }
}
