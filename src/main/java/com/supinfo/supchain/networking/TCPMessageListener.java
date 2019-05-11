package com.supinfo.supchain.networking;

import com.supinfo.supchain.enums.Role;
import com.supinfo.supchain.enums.TCPMessageType;
import com.supinfo.supchain.helpers.BytesUtil;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.models.Messenger;
import com.supinfo.supchain.models.TCPMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static com.supinfo.supchain.enums.LogLevel.*;

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
                            if(RUtils.externalClientAddresses.size() < RUtils.maxNumberOfConnections){
                                //if we dnt have the maximum number of connections we are going to accept the direct connection
                                TCPMessage responseMessage = new TCPMessage(TCPMessageType.CONFIRM_CONNECTION,false,0);
                                TCPUtils.unicast(responseMessage,origin);
                                RUtils.externalClientAddresses.add(origin);
                                cLogger.log(LOW,"REQUEST RECEIVED >>>added " + origin + "to the list of clients");
                            }else{
                                //else we will send a messenger to look for another peer, in the process we may find redundant peers
                                //thus easing the life of the new peer in finding redundant connections until its minimumNumber of peers
                                //is satisfied
                                TCPMessage responseMessage = new TCPMessage(TCPMessageType.WAIT_FOR_LOOKUP,false,0);
                                TCPUtils.unicast(responseMessage,origin);

                                //send a messenger to look up for a free peer
                                TCPMessage messengerCarrier = new TCPMessage(TCPMessageType.MESSENGER_REQ, true,RUtils.messengerTimeout);
                                Messenger messenger = new Messenger(origin);
                                // Convert messenger to byte array
                                messengerCarrier.setData(BytesUtil.toByteArray(messenger));
                                cLogger.log(HIGH,"Broadcasting a MESSENGER_REQ to look for a connection for the foreveralone peer: " + origin);
                                TCPUtils.multicastRDVs(messengerCarrier,origin);
                            }
                            break;
                        case WAIT_FOR_LOOKUP:
                            cLogger.log(HIGH,"REQUESTED PEER IS FULL, please wait while we search for another peer");
                            break;
                        case CONFIRM_CONNECTION:
                            RUtils.externalClientAddresses.add(origin);
                            cLogger.log(LOW,"CONFIRM RECEIVED >>>added " + origin + "to the list of clients");
                            //After that this peer has received a confirmation of connection it can begin looking up for
                            //redundant connections(if it doesnt have the minimum number of connections), it can do so by sending a messenger req.
                            if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                                TCPMessage messengerCarrier = new TCPMessage(TCPMessageType.MESSENGER_REQ, true,RUtils.messengerTimeout);
                                Messenger messenger = new Messenger(RUtils.externalIP);
                                // Convert messenger to byte array
                                messengerCarrier.setData(BytesUtil.toByteArray(messenger));
                                cLogger.log(HIGH,"Broadcasting a MESSENGER_REQ to look for Redundant connections");
                                TCPUtils.multicastRDVs(messengerCarrier,"none");
                            }
                            //if in production mode push external IP to REST api
                            break;
                        case MESSENGER_REQ:
                            cLogger.log(HIGH,"This has received a MESSENGER_REQ");
                            Messenger messenger = (Messenger) BytesUtil.toObject(tcpMessage.getData());
                            if((RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections) //change this to max?
                                    && !RUtils.externalClientAddresses.contains(messenger.getSearchingIP())){
                                cLogger.log(HIGH,"Slot available and messenger not form a directly connected peer");
                                messenger.setNewPeerAddress(RUtils.externalIP);
                                TCPMessage messengerCarrier = new TCPMessage(TCPMessageType.MESSENGER_ACK, false,0);
                                messengerCarrier.setData(BytesUtil.toByteArray(messenger));
                                //it fetches the public ip of the new machine and unicast it back to its origin
                                cLogger.log(HIGH, "sending messenger back to its origin:"+ messenger.getSearchingIP() +" since this peer is the new peer to be added");
                                TCPUtils.unicast(messengerCarrier,messenger.getSearchingIP());

                            }else{
                                cLogger.log(HIGH,"This client already has the min number of required clients or the Message REQ came from a directly connected PEER");
                                if(tcpMessage.isAlive()){
                                    TCPUtils.multicastRDVs(tcpMessage,origin);
                                    cLogger.log(HIGH,"MESSENGER_REQ is alive, multicastAll it again.");
                                }else{
                                    cLogger.log(HIGH,"MESSENGER_REQ is dead, no multicastAll done.");
                                }

                            }
                            break;
                        case MESSENGER_ACK:
                            if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                                // this will accept ack as long as we do not satisfy the minimum requirements
                                messenger = (Messenger) BytesUtil.toObject(tcpMessage.getData());
                                TCPMessage requestMessage = new TCPMessage(TCPMessageType.REQUEST_CONNECTION,false,0);
                                TCPUtils.unicast(requestMessage,messenger.getNewPeerAddress());
                                cLogger.log(LOW,"This client received MESSENGER_ACK, sending a request message to:" + messenger.getNewPeerAddress());
                            }
                            //once we satisfy the min requirements all message ack received will be dropped
                        case PING:
                            break;
                        case PONG:
                            break;
                        default:
                            break;
                    }
                }

                //these are protocols that apply to both RDVs and EDGEs
                switch (tcpMessage.getTcpMessageType()){
                    case VERIFY:
                        cLogger.log(HIGH,"Got a verify Message, from "+ origin+ " propagating");
                        if(tcpMessage.isPropagatable() && tcpMessage.isAlive()){
                            TCPUtils.multicastAll(tcpMessage,socket.getInetAddress().getHostAddress());
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
