package networking;

import enums.TCPMessageType;
import helpers.CLogger;
import helpers.RUtils;
import models.TCPMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static enums.LogLevel.*;

public class TCPMessageListener extends Thread{
    private int port;
    private ServerSocket serverSocket;
    private CLogger cLogger = new CLogger(this.getClass());
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
                cLogger.print(SUPERHIGH,"ServerSocket awaiting connections...");
                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                cLogger.print(SUPERHIGH,"Connection from " + socket + "!");
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                String origin = socket.getInetAddress().getHostAddress();
                cLogger.print(LOW,"TCP connection from " + origin);
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                cLogger.print(LOW, "got the message " + tcpMessage.getTcpMessageType().toString() + " from " + socket.getInetAddress().getHostAddress());



                switch (tcpMessage.getTcpMessageType()){
                    case REQUEST_CONNECTION:
                        TCPMessage responseMessage = new TCPMessage(TCPMessageType.CONFIRM_CONNECTION,false);
                        TCPUtils.unicast(responseMessage,origin);
                        RUtils.externalClientAddresses.add(origin);
                        cLogger.print(LOW,"REQUEST RECEIVED >>>added " + origin + "to the list of clients");
                        break;
                    case CONFIRM_CONNECTION:
                        RUtils.externalClientAddresses.add(origin);
                        cLogger.print(LOW,"CONFIRM RECEIVED >>>added " + origin + "to the list of clients");
                        break;
                    case VERIFY:
                        if(tcpMessage.isPropagatable()){
                            TCPUtils.multicast(tcpMessage,socket.getInetAddress().getHostAddress());
                        }
                        break;
                    default:
                        break;
                }

                //TODO TESTING SERVER OUTPUT BACK TO CLIENT
                TCPMessage myTestMessage = new TCPMessage(TCPMessageType.CLOSE_SOCKET,false);
                OutputStream outputStream = socket.getOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(myTestMessage);
                objectOutputStream.flush();
                objectOutputStream.close();

            }catch (IOException | ClassNotFoundException ex){
                ex.printStackTrace();
            }
        }



    }
}
