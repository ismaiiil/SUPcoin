package networking;

import enums.TCPMessageType;
import helpers.BytesUtil;
import helpers.CLogger;
import helpers.RUtils;
import models.Messenger;
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
                cLogger.log(SUPERHIGH,"ServerSocket awaiting connections...");
                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                cLogger.log(SUPERHIGH,"Connection from " + socket + "!");
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                String origin = socket.getInetAddress().getHostAddress();
                cLogger.log(LOW,"TCP connection from " + origin);
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                cLogger.log(LOW, "got the message " + tcpMessage.getTcpMessageType().toString() + " from " + socket.getInetAddress().getHostAddress());



                switch (tcpMessage.getTcpMessageType()){
                    case REQUEST_CONNECTION:
                        TCPMessage responseMessage = new TCPMessage(TCPMessageType.CONFIRM_CONNECTION,false);
                        TCPUtils.unicast(responseMessage,origin);
                        RUtils.externalClientAddresses.add(origin);
                        cLogger.log(LOW,"REQUEST RECEIVED >>>added " + origin + "to the list of clients");
                        break;
                    case CONFIRM_CONNECTION:
                        RUtils.externalClientAddresses.add(origin);
                        cLogger.log(LOW,"CONFIRM RECEIVED >>>added " + origin + "to the list of clients");

                        //After that this peer has received a confirmation of connection it can begin looking up for
                        //redundant connections, it can do so by sending a messenger.
                        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                            TCPMessage messengerCarrier = new TCPMessage(TCPMessageType.MESSENGER_REQ, true);
                            Messenger messenger = new Messenger(origin,null);
                            // Convert messenger to byte array
                            messengerCarrier.setData(BytesUtil.toByteArray(messenger));
                            TCPUtils.multicast(messengerCarrier,socket.getInetAddress().getHostAddress());
                        }

                        break;
                    case VERIFY:
                        if(tcpMessage.isPropagatable()){
                            TCPUtils.multicast(tcpMessage,socket.getInetAddress().getHostAddress());
                        }
                        break;
                    case MESSENGER_REQ:
                        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                            Messenger messenger = (Messenger) BytesUtil.toObject(tcpMessage.getData());
                            if(messenger.getNewPeerAddress() == null){
                                messenger.setNewPeerAddress(RUtils.externalIP);
                                TCPMessage messengerCarrier = new TCPMessage(TCPMessageType.MESSENGER_ACK, true);
                                messengerCarrier.setData(BytesUtil.toByteArray(messenger));
                                TCPUtils.unicast(messengerCarrier,messenger.getOrigin());
                            }
                        }else{
                            //TODO fox infinitely spreading messenger, the propagation NEEDS to stop at some point
                            TCPUtils.multicast(tcpMessage,origin);
                        }
                        break;
                    case MESSENGER_ACK:
                        Messenger messenger = (Messenger) BytesUtil.toObject(tcpMessage.getData());
                        if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                            RUtils.externalClientAddresses.add(messenger.getNewPeerAddress());
                        }
                    default:
                        break;
                }

//                //TESTING NODE OUTPUT BACK TO WALLET
//                TCPMessage myTestMessage = new TCPMessage(TCPMessageType.CLOSE_SOCKET,false);
//                OutputStream outputStream = socket.getOutputStream();
//                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
//                objectOutputStream.writeObject(myTestMessage);
//                objectOutputStream.flush();
//                objectOutputStream.close();

                socket.close();

            }catch (IOException | ClassNotFoundException ex){
                ex.printStackTrace();
            }
        }



    }
}
