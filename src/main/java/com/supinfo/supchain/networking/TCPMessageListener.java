package com.supinfo.supchain.networking;

import com.supinfo.supchain.enums.Role;
import com.supinfo.shared.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.models.Messenger;
import com.supinfo.supchain.models.PingPong;
import com.supinfo.shared.TCPMessage;
import com.supinfo.supchain.models.Updater;

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

                Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                String origin = socket.getInetAddress().getHostAddress();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TCPMessage tcpMessage = (TCPMessage) objectInputStream.readObject();
                cLogger.log(NETWORK, "got the message " + tcpMessage.getTcpMessageType().toString() + " from " + socket.getInetAddress().getHostAddress());

                //these are protocols that apply only to RDVs
                if(RUtils.myRole == Role.RDV){
                    switch (tcpMessage.getTcpMessageType()){
                        case REQUEST_CONNECTION:
                            if((RUtils.externalClientAddresses.size() < RUtils.maxNumberOfConnections) && (!RUtils.externalClientAddresses.contains(origin))){
                                //if we dnt have the maximum number of connections we are going to accept the direct connection
                                TCPMessage responseMessage = new TCPMessage<>(TCPMessageType.CONFIRM_CONNECTION,false,0,null);
                                TCPUtils.unicast(responseMessage,origin);
                                RUtils.externalClientAddresses.add(origin);
                                cLogger.log(NETWORK,"REQUEST RECEIVED >>>added " + origin + "to the list of clients");
                            }else{
                                //else we will send a messenger to look for another peer, in the process we may find redundant peers
                                //thus easing the life of the new peer in finding redundant connections until its minimumNumber of peers
                                //is satisfied
                                TCPMessage responseMessage = new TCPMessage<>(TCPMessageType.WAIT_FOR_LOOKUP,false,0,null);
                                TCPUtils.unicast(responseMessage,origin);

                                //send a messenger to look up for a free peer
                                Messenger messenger = new Messenger(origin);
                                TCPMessage messengerCarrier = new TCPMessage<>(TCPMessageType.MESSENGER_REQ, true,RUtils.messengerTimeout,messenger);
                                cLogger.log(NETWORK,"Broadcasting a MESSENGER_REQ to look for a connection for the foreveralone peer: " + origin);
                                TCPUtils.multicastRDVs(messengerCarrier,origin);
                            }
                            break;
                        case WAIT_FOR_LOOKUP:
                            cLogger.log(NETWORK,"REQUESTED PEER IS FULL, please wait while we search for another peer");
                            break;
                        case CONFIRM_CONNECTION:
                            RUtils.externalClientAddresses.add(origin);
                            cLogger.log(NETWORK,"CONFIRM RECEIVED >>>added " + origin + "to the list of clients");
                            //After that this peer has received a confirmation of connection it can begin looking up for
                            //redundant connections(if it doesnt have the minimum number of connections), it can do so by sending a messenger req.
                            if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                                Messenger messenger = new Messenger(RUtils.externalIP);
                                TCPMessage messengerCarrier = new TCPMessage<>(TCPMessageType.MESSENGER_REQ, true,RUtils.messengerTimeout,messenger);

                                cLogger.log(NETWORK,"Broadcasting a MESSENGER_REQ to look for Redundant connections");
                                TCPUtils.multicastRDVs(messengerCarrier,"none");
                            }
                            //if in production mode push external IP to REST api
                            break;
                        case MESSENGER_REQ:
                            Messenger messenger = (Messenger) tcpMessage.getData();
                            if((RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections) //change this to max?
                                    && !RUtils.externalClientAddresses.contains(messenger.getSearchingIP())){
                                messenger.setNewPeerAddress(RUtils.externalIP);
                                TCPMessage messengerCarrier = new TCPMessage<>(TCPMessageType.MESSENGER_ACK, false,0,messenger);
                                //it fetches the public ip of the new machine and unicast it back to its origin
                                cLogger.log(NETWORK, "sending messenger back to its origin:"+ messenger.getSearchingIP() +" since this peer is the new peer to be added");
                                TCPUtils.unicast(messengerCarrier,messenger.getSearchingIP());

                            }else{
                               if(tcpMessage.isAlive()){
                                    TCPUtils.multicastRDVs(tcpMessage,origin);
                                }
                            }
                            break;
                        case MESSENGER_ACK:
                            if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                                // this will accept ack as long as we do not satisfy the minimum requirements
                                messenger = (Messenger) tcpMessage.getData();
                                TCPMessage requestMessage = new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,false,0,messenger);
                                TCPUtils.unicast(requestMessage,messenger.getNewPeerAddress());
                                cLogger.log(NETWORK,"This client received MESSENGER_ACK, sending a request message to:" + messenger.getNewPeerAddress());
                            }
                            //once we satisfy the min requirements all message ack received will be dropped

                        default:
                            break;
                    }
                }

                //these are protocols that apply to both RDVs and EDGEs
                switch (tcpMessage.getTcpMessageType()){
                    case VERIFY:
                        cLogger.log(BASIC,"Got a verify Message, from "+ origin+ " propagating");
                        if(tcpMessage.isPropagatable() && tcpMessage.isAlive()){
                            TCPUtils.multicastAll(tcpMessage,socket.getInetAddress().getHostAddress());
                        }
                        break;
                    case PING:

                        PingPong ping = (PingPong) tcpMessage.getData();
                        //send back a pong only if ping has the same origin as the message, and the ping origin is stored in the list of external addresses
                        if(ping.getOrigin().equals(origin) && RUtils.externalClientAddresses.contains(ping.getOrigin())){
                            cLogger.log(NETWORK,"received a ping from + " + ping.getOrigin() + ", sending back a pong!");
                            TCPMessage pongMessage = new TCPMessage<>(TCPMessageType.PONG,false,0, new PingPong(RUtils.externalIP));
                            TCPUtils.unicast(pongMessage, ping.getOrigin());
                        }else{
                            cLogger.log(NETWORK,"received a PING from an unknown host!" + ping.getOrigin() );
                        }

                        break;
                    case PONG:
                        PingPong pong = (PingPong) tcpMessage.getData();
                        cLogger.log(NETWORK,"successfully received back a pong from + " + pong.getOrigin());
                        RUtils.pingedAddresses.remove(pong.getOrigin());
                        break;
                    case UPDATE_SENDER_IP:
                        Updater updater = (Updater) tcpMessage.getData();
                        if(RUtils.externalClientAddresses.contains(updater.getOldIP())){
                            RUtils.externalClientAddresses.remove(updater.getOldIP());
                            RUtils.externalClientAddresses.add(updater.getNewIP());
                        }
                        if(RUtils.localClientAddresses.contains(updater.getOldIP())){
                            RUtils.localClientAddresses.remove(updater.getOldIP());
                            RUtils.localClientAddresses.add(updater.getNewIP());
                        }
                        break;

                    //ALL WALLET RELATED MESSAGES:
                    case WALLET_CONNECT:
                        cLogger.log(BASIC,"successfully received wallet message from + " + origin);
                        String data = (String) tcpMessage.getData();
                        cLogger.log(BASIC,"GOT THE DATA"+ data);
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
