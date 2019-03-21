package networking;

import helpers.CLogger;
import helpers.R;
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
                CLogger.print(LOW,getClass().getName() + "ServerSocket awaiting connections...");
                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                CLogger.print(LOW,getClass().getName() + "Connection from " + socket + "!");
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                CLogger.print(LOW,getClass().getName() + "got the message" + tcpMessage.getMessage() + "from" + socket.getInetAddress());

                if(!R.cacheMessage.contains(tcpMessage.getMessageHash())){
                    for (String ipadd: R.ClientAddreses) {
                        System.out.println("propagating test message to"+ ipadd);
                        TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(tcpMessage,ipadd,8888);
                        tcpMessageEmmiter.start();
                    }
                }else{
                    System.out.println("this message has already been sent from this node dropping it.");
                }
                R.cacheMessage.add(tcpMessage.getMessageHash());


                socket.close();
            }catch (IOException | ClassNotFoundException ex){
                ex.printStackTrace();
            }
        }



    }
}
