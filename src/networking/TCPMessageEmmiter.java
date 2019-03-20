package networking;

import models.TCPMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPMessageEmmiter extends Thread {
    String hostname;
    TCPMessage tcpMessage;
    int port;
    Socket socket;

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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
