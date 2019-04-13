package networking;

import enums.TCPMessageType;
import helpers.CLogger;
import helpers.RUtils;
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
                CLogger.print(HIGH,getClass().getName() + " ServerSocket awaiting connections...");
                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                CLogger.print(HIGH,getClass().getName() + " Connection from " + socket + "!");
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                String origin = socket.getInetAddress().getHostAddress();
                CLogger.print(LOW,getClass().getName() + " TCP connection from " + origin);
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                CLogger.print(LOW,getClass().getName() + " got the message " + tcpMessage.getTcpMessageType().toString() + " from " + socket.getInetAddress().getHostAddress());
                switch (tcpMessage.getTcpMessageType()){
                    case REQUEST_CONNECTION:
                        TCPMessage responseMessage = new TCPMessage(TCPMessageType.CONFIRM_CONNECTION,false);
                        TCPUtils.unicast(responseMessage,origin);
                        RUtils.externalClientAddresses.add(origin);
                        CLogger.print(LOW,getClass().getName() + "REQUEST RECEIVED >>>added " + origin + "to the list of clients");
                        break;
                    case CONFIRM_CONNECTION:
                        RUtils.externalClientAddresses.add(origin);
                        CLogger.print(LOW,getClass().getName() + "CONFIRM RECEIVED >>>added " + origin + "to the list of clients");
                        break;
                    case VERIFY:
                        if(tcpMessage.isPropagatable()){
                            TCPUtils.multicast(tcpMessage,socket.getInetAddress().getHostAddress());
                        }
                        break;
                    default:
                        break;
                }


                socket.close();
            }catch (IOException | ClassNotFoundException ex){
                ex.printStackTrace();
            }
        }



    }
}
