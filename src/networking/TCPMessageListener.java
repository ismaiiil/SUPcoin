package networking;

import models.TCPMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

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
                System.out.println("ServerSocket awaiting connections...");
                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                System.out.println("Connection from " + socket + "!");
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                System.out.println(tcpMessage.getMessage());
                socket.close();
            }catch (IOException | ClassNotFoundException ex){
                ex.printStackTrace();
            }
        }



    }
}
