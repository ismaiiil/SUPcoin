package com.supinfo.supchain.networking.Threads;

import com.supinfo.shared.transaction.Transaction;
import com.supinfo.shared.transaction.TransactionInput;
import com.supinfo.shared.transaction.TransactionOutput;
import com.supinfo.supchain.blockchain.Block;
import com.supinfo.supchain.blockchain.BlockchainCallbacks;
import com.supinfo.supchain.blockchain.BlockchainHolderManager;
import com.supinfo.supchain.blockchain.transaction.TransactionOperations;
import com.supinfo.supchain.blockchain.wallet.Wallet;
import com.supinfo.supchain.enums.Role;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.networking.Utils.TCPUtils;
import com.supinfo.supchain.networking.models.Messenger;
import com.supinfo.supchain.networking.models.PingPong;
import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.supchain.networking.models.Updater;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;

import static com.supinfo.supchain.Main.blockchainHolder;
import static com.supinfo.supchain.enums.LogLevel.*;

public class TCPMessageListener extends Thread{
    private int port;
    private ServerSocket serverSocket;
    private CLogger cLogger = new CLogger(this.getClass());
    private BlockchainCallbacks blockchainCallbacks;
    public TCPMessageListener(int port,BlockchainCallbacks blockchainCallbacks) {
        this.port = port;
        this.blockchainCallbacks = blockchainCallbacks;
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
                        case REQUEST_CONNECTION:{
                            if((RUtils.externalClientAddresses.size() < RUtils.maxNumberOfConnections) && (!RUtils.externalClientAddresses.contains(origin))){
                                //if we dnt have the maximum number of connections we are going to accept the direct connection
                                TCPMessage responseMessage = new TCPMessage<>(TCPMessageType.CONFIRM_CONNECTION,null);
                                TCPUtils.unicast(responseMessage,origin);
                                RUtils.externalClientAddresses.add(origin);
                                cLogger.log(NETWORK,"REQUEST RECEIVED >>>added " + origin + "to the list of clients");
                            }else{
                                //else we will send a messenger to look for another peer, in the process we may find redundant peers
                                //thus easing the life of the new peer in finding redundant connections until its minimumNumber of peers
                                //is satisfied
                                TCPMessage responseMessage = new TCPMessage<>(TCPMessageType.WAIT_FOR_LOOKUP,null);
                                TCPUtils.unicast(responseMessage,origin);

                                //send a messenger to look up for a free peer,
                                Messenger messenger = new Messenger(origin);
                                TCPMessage messengerCarrier = new TCPMessage<>(TCPMessageType.MESSENGER_REQ,RUtils.messengerTimeout,messenger);
                                cLogger.log(NETWORK,"Broadcasting a MESSENGER_REQ to look for a connection for the foreveralone peer: " + origin);
                                TCPUtils.multicastRDVs(messengerCarrier,origin);
                            }
                            break;}
                        case WAIT_FOR_LOOKUP:{
                            cLogger.log(NETWORK,"REQUESTED PEER IS FULL, please wait while we search for another peer");
                            break;}
                        case CONFIRM_CONNECTION:{
                            RUtils.externalClientAddresses.add(origin);
                            cLogger.log(NETWORK,"CONFIRM RECEIVED >>>added " + origin + "to the list of clients");
                            //After that this peer has received a confirmation of connection it can begin looking up for
                            //redundant connections(if it doesnt have the minimum number of connections), it can do so by sending a messenger req.
                            if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                                Messenger messenger = new Messenger(RUtils.externalIP);
                                TCPMessage messengerCarrier = new TCPMessage<>(TCPMessageType.MESSENGER_REQ,RUtils.messengerTimeout,messenger);

                                cLogger.log(NETWORK,"Broadcasting a MESSENGER_REQ to look for Redundant connections");
                                TCPUtils.multicastRDVs(messengerCarrier,"none");
                            }
                            //if in production mode push external IP to REST api
                            break;}
                        case MESSENGER_REQ:{
                            Messenger messenger = (Messenger) tcpMessage.getData();
                            if((RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections) //change this to max?
                                    && !RUtils.externalClientAddresses.contains(messenger.getSearchingIP())){
                                messenger.setNewPeerAddress(RUtils.externalIP);
                                TCPMessage messengerCarrier = new TCPMessage<>(TCPMessageType.MESSENGER_ACK,messenger);
                                //it fetches the public ip of the new machine and unicast it back to its origin
                                cLogger.log(NETWORK, "sending messenger back to its origin:"+ messenger.getSearchingIP() +" since this peer is the new peer to be added");
                                TCPUtils.unicast(messengerCarrier,messenger.getSearchingIP());

                            }else{
                               if(tcpMessage.isAlive()){
                                    TCPUtils.multicastRDVs(tcpMessage,origin);
                                }
                            }
                            break;}
                        case MESSENGER_ACK:{

                            if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                                // this will accept ack as long as we do not satisfy the minimum requirements
                                Messenger messenger = (Messenger) tcpMessage.getData();
                                TCPMessage requestMessage = new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,messenger);
                                TCPUtils.unicast(requestMessage,messenger.getNewPeerAddress());
                                cLogger.log(NETWORK,"This client received MESSENGER_ACK, sending a request message to:" + messenger.getNewPeerAddress());
                            }
                            //once we satisfy the min requirements all message ack received will be dropped
                            break;}
                        default:
                            break;
                    }
                }

                //these are protocols that apply to both RDVs and EDGEs
                switch (tcpMessage.getTcpMessageType()){
                    case VERIFY:{
                        cLogger.log(BASIC,"Got a verify Message, from "+ origin+ " propagating");
                        if(tcpMessage.isPropagatable() && tcpMessage.isAlive()){
                            TCPUtils.multicastAll(tcpMessage,socket.getInetAddress().getHostAddress());
                        }
                        break;}
                    case PING:{

                        PingPong ping = (PingPong) tcpMessage.getData();
                        //send back a pong only if ping has the same origin as the message, and the ping origin is stored in the list of external addresses
                        if(ping.getOrigin().equals(origin) && RUtils.externalClientAddresses.contains(ping.getOrigin())){
                            cLogger.log(NETWORK,"received a ping from + " + ping.getOrigin() + ", sending back a pong!");
                            TCPMessage pongMessage = new TCPMessage<>(TCPMessageType.PONG,new PingPong(RUtils.externalIP));
                            TCPUtils.unicast(pongMessage, ping.getOrigin());
                        }else{
                            cLogger.log(NETWORK,"received a PING from an unknown host!" + ping.getOrigin() );
                        }

                        break;}
                    case PONG:{
                        PingPong pong = (PingPong) tcpMessage.getData();
                        cLogger.log(NETWORK,"successfully received back a pong from + " + pong.getOrigin());
                        RUtils.pingedAddresses.remove(pong.getOrigin());
                        break;}
                    case UPDATE_SENDER_IP:{
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
                    }

                    //ALL WALLET RELATED MESSAGES:
                    case WALLET_PING:{
                        TCPMessage<String> m = new TCPMessage<>(TCPMessageType.WALLET_CONNECT,"");
                        putInStream(socket, m);
                        break;
                    }
                    case WALLET_LIST_NODES:{
                        TCPMessage<HashSet<String>> myTestMessage = new TCPMessage<>(TCPMessageType.WALLET_CONNECT, RUtils.externalClientAddresses);
                        putInStream(socket, myTestMessage);
                        break;
                    }
                    case WALLET_FETCH_UTXOS:{
                        break;
                    }
                    case WALLET_BUY_COINS:{
                        //will receive a txn and will reply with a WALLET_NODE_INSUFFICIENT_COINS or WALLET_SUCCESS_BUY with putInStream
                        //if we have enough coins the txn is validated and it is sent to the mempool
                        //now the important thing is how are we going to manage mempool and when are we going to mine the coins?
                        Transaction rt = (Transaction) tcpMessage.getData();
                        //putting back the node wallet to get back change
                        BigDecimal _amount = rt.recipients.values().iterator().next();
                        PublicKey destination = rt.recipients.keySet().iterator().next();

                        if(_amount.compareTo(blockchainHolder.getMinerRawBalance()) < 0){
                            //the request is less than the total number of available coins on the node
                            //we can start making the txn valid and then push it onto the mempool
                            rt.sender = RUtils.wallet.getPublicKey();
                            ArrayList<TransactionOutput> _utxosToUse = blockchainHolder.getMinUTXOForPublicKey(RUtils.wallet.getPublicKey(),_amount);
                            ArrayList<TransactionInput> _inputs = new ArrayList<>();
                            //convert the txn outputs to inputs
                            for (TransactionOutput tout:_utxosToUse) {
                                _inputs.add(new TransactionInput(tout.id,tout));
                                blockchainHolder.lockedUTXOs.put(tout.id,tout);
                            }
                            rt.inputs = _inputs;
                            rt.recipients.put(RUtils.wallet.getPublicKey(),rt.getInputsValue().subtract(_amount));
                            rt.transactionId = TransactionOperations.calulateHashTransaction(rt);
                            //we only need one tout to  the user and a change back to the coin base
                            ArrayList<TransactionOutput> _txnOutputs =  new ArrayList<>();
                            TransactionOutput _tout = new TransactionOutput(destination,_amount,rt.transactionId,null);
                            _tout.id = TransactionOperations.generateTransactionOutputThisId(_tout);
                            TransactionOutput _change = new TransactionOutput(RUtils.wallet.getPublicKey(), rt.getInputsValue().subtract(_amount),rt.transactionId,null);
                            _change.id = TransactionOperations.generateTransactionOutputThisId(_change);
                            _txnOutputs.add(_tout);
                            _txnOutputs.add(_change);
                            rt.outputs = _txnOutputs;
                            rt.signature = TransactionOperations.generateSignature(RUtils.wallet.getPrivateKey(),rt);

                            //we can now reply back the final transaction and we can push it to the mempool
                            putInStream(socket,new TCPMessage<>(TCPMessageType.WALLET_SUCCESS_BUY,rt));
                            blockchainHolder.addTransactionToMemPool(rt);

                        }
                        else{
                            putInStream(socket,new TCPMessage<>(TCPMessageType.WALLET_NODE_INSUFFICIENT_COINS,null));
                        }
                        break;

                    }
                    case WALLET_NODE_BALANCE:{
                        //return to wallet the balance the miner has!
                        TCPMessage<BigDecimal> amountMessage = new TCPMessage<>(TCPMessageType.WALLET_NODE_BALANCE,blockchainHolder.getMinerRawBalance());
                        putInStream(socket, amountMessage);
                        break;
                    }

                    //ALL BlockchainHolder related message:
                    case INIT_REQUEST_DOWNLOAD:{
                        //send to the peer the blockchainHolder
                        cLogger.log(CHAIN,"received a request to download this node's chain");
                        TCPMessage sendBlockchain = new TCPMessage<>(TCPMessageType.INIT_DOWNLOAD_FULL_BLOCKCHAIN, BlockchainHolderManager.getInstance().blockchain);
                        TCPUtils.unicast(sendBlockchain,origin);
                        break;
                    }
                    case INIT_DOWNLOAD_FULL_BLOCKCHAIN:{
                        //verify the blockchainHolder
                        cLogger.log(CHAIN,"NOW DOWNLOADING CHAIN FROM:"+ origin);
                        blockchainHolder.status = TCPMessageType.INIT_DOWNLOAD_FULL_BLOCKCHAIN;
                        ArrayList<Block> newBlockchain = (ArrayList<Block>) tcpMessage.getData();
                        if(blockchainHolder.validateBlockchain(newBlockchain)
                                && (blockchainHolder.blockchain.size() < newBlockchain.size())){
                            blockchainHolder.blockchain = newBlockchain;
                            //only if it is valid assign it to the blockchainHolder
                            //once it has received the full blockchainHolder we can start do stuff
                            blockchainCallbacks.initHasDownloaded(true,origin);
                        }else{
                            //send callback failed to get a valid blockchainHolder
                            blockchainCallbacks.initHasDownloaded(false,origin);
                        }
                        break;
                    }
                    case REQUEST_MEMPOOL:{
                        //send the mempool to the requesting node
                        cLogger.log(CHAIN,"A peer has requested a copy of the mempool!");
                        TCPMessage sendMempool = new TCPMessage<>(TCPMessageType.RECEIVE_MEMPOOL,blockchainHolder.mempool);
                        TCPUtils.unicast(sendMempool,origin);
                        break;
                    }
                    case RECEIVE_MEMPOOL:{
                        //add the new mempool to the current mempool
                        ArrayList<Transaction> newMempool = (ArrayList<Transaction>) tcpMessage.getData();
                        cLogger.log(CHAIN,"Successfully received the new mempool!");
                        blockchainHolder.mempool.addAll(newMempool);
                        break;
                    }
                    case PROPAGATE_NEW_TXN_MEMPOOL:{
                        Transaction transaction = (Transaction) tcpMessage.getData();
                        break;

                    }

                }


                objectInputStream.close();
                socket.close();

            } catch (StreamCorruptedException e){
                cLogger.log(EXCEPTION,"A Stream was corrupted closing!");
            } catch (IOException | ClassNotFoundException ex){
                ex.printStackTrace();
            }
        }



    }

    private void putInStream(Socket socket, TCPMessage m) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(m);
        objectOutputStream.flush();
        objectOutputStream.close();
    }
}
