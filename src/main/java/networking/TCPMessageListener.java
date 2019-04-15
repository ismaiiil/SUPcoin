package networking;

import enums.Role;
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
                cLogger.log(SUPERDUPERHIGH,"ServerSocket awaiting connections...");
                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                cLogger.log(SUPERDUPERHIGH,"Connection from " + socket + "!");
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                String origin = socket.getInetAddress().getHostAddress();
                cLogger.log(SUPERDUPERHIGH,"TCP connection from " + origin);
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                cLogger.log(SUPERDUPERHIGH, "got the message " + tcpMessage.getTcpMessageType().toString() + " from " + socket.getInetAddress().getHostAddress());

                //these are protocols that apply only to RDVs
                if(RUtils.myRole == Role.RDV){
                    switch (tcpMessage.getTcpMessageType()){
                        case REQUEST_CONNECTION:
                            TCPMessage responseMessage = new TCPMessage(TCPMessageType.CONFIRM_CONNECTION,false,0);
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
                                TCPMessage messengerCarrier = new TCPMessage(TCPMessageType.MESSENGER_REQ, true,10);
                                Messenger messenger = new Messenger(origin,null);
                                // Convert messenger to byte array
                                messengerCarrier.setData(BytesUtil.toByteArray(messenger));
                                cLogger.log(HIGH,"Broadcasting a MESSENGER_REQ to look for Redundant connections");
                                TCPUtils.multicast(messengerCarrier,socket.getInetAddress().getHostAddress());
                            }

                            break;
                        case MESSENGER_REQ:
                            cLogger.log(HIGH,"This has received a MESSENGER_REQ");
                            Messenger messenger = (Messenger) BytesUtil.toObject(tcpMessage.getData());
                            if((RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections)
                                    && !RUtils.externalClientAddresses.contains(messenger.getOrigin())){
                                cLogger.log(HIGH,"Slot available and messenger not form a directly connected peer");
                                messenger.setNewPeerAddress(RUtils.externalIP);
                                TCPMessage messengerCarrier = new TCPMessage(TCPMessageType.MESSENGER_ACK, false,0);
                                messengerCarrier.setData(BytesUtil.toByteArray(messenger));
                                //it fetches the public ip of the new machine and unicast it back to its origin
                                cLogger.log(HIGH, "sending messenger back to its origin since this peer is the new peer to be added");
                                TCPUtils.unicast(messengerCarrier,messenger.getOrigin());

                            }else{
                                cLogger.log(HIGH,"This client already has the max number of allowed clients");
                                if(tcpMessage.isAlive()){
                                    TCPUtils.multicast(tcpMessage,origin);
                                    cLogger.log(HIGH,"Messenger is alive, multicast him again.");
                                }else{
                                    cLogger.log(HIGH,"Messenger is dead, no multicast done.");
                                }

                            }
                            break;
                        case MESSENGER_ACK:
                            messenger = (Messenger) BytesUtil.toObject(tcpMessage.getData());
                            if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                                TCPMessage requestMessage = new TCPMessage(TCPMessageType.REQUEST_CONNECTION,false,0);
                                TCPUtils.unicast(requestMessage,messenger.getNewPeerAddress());
                            }
                        default:
                            break;
                    }
                }

                //TODO multicasting in RDVs still send RDV related messages to edges which is not right
                //these are protocols that apply to both RDVs and EDGEs
                switch (tcpMessage.getTcpMessageType()){
                    case VERIFY:
                        if(tcpMessage.isPropagatable()){
                            TCPUtils.multicast(tcpMessage,socket.getInetAddress().getHostAddress());
                        }
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
