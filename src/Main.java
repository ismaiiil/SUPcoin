import enums.LogLevel;
import enums.Role;
import enums.TCPMessageType;
import helpers.CLogger;
import helpers.R;
import localNetworking.UDPClientDiscovery;
import localNetworking.UDPMessageListener;
import models.TCPMessage;
import networking.TCPMessageEmmiter;
import networking.TCPMessageListener;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        CLogger.logLevel = LogLevel.NONE;

        Scanner user_input = new Scanner(System.in);
        System.out.println("Welcome to SUPCoin core");
        System.out.println("This is prior setup before you start mining");
        System.out.println("Do you want to use this machine as an RDV(Rendez-Vous) peer or EDGE peer");
        String userChoice = user_input.nextLine();
        while (true){
            try{
                R.myRole = Role.valueOf(userChoice);
                break;
            }catch (IllegalArgumentException e){
                System.out.println("wrong choice please try either inputting EDGE or RDV");
                userChoice = user_input.nextLine();
            }
        }
        System.out.println("Your choose the Role of " + R.myRole.toString());
        switch (R.myRole){
            case EDGE:
                System.out.println("Do you want to initiate network discovery of an RDP (y/n)");
                userChoice = user_input.nextLine();

                if(userChoice.equals("y")){
                    Thread client = new Thread(new UDPClientDiscovery(3));
                    client.start();
                    try {
                        client.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(!R.ClientAddreses.isEmpty()){
                        System.out.println("Successfully found the RDV node at: "+ R.ClientAddreses.get(0));
                    }
                }

                break;
            case RDV:
                Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
                discoveryThread.start();
                break;
        }


        /*
        if this is an RDV get the block chain from SEEDER peer which is technically also an RDV peer
        after the block chain has been downloaded, the EDGE peers can send a request a connection to the seed peer
        once a connection has been established to a SEED peer OR another RDV peer, the current RDV peer will request for the
        block chain, the seeder will cascade redirect the RDV peer to other RDV peers in the p2p network,
        even if the seeder peers go down, the p2p would be still working but no new connections would be possible to be made
        no new transactions either, unless you can get the public ip of any RDV peer
        send messages over TCP, a message class contains different data types such as, origin of TCP packet, an enum, and the
        data associated to the message for example the block chain itself.
        */

        System.out.println("do you want to test exchange of messages over TCP (y/n)");
        TCPMessageListener messageListener = new TCPMessageListener(8888);
        messageListener.start();
        String user_choice = user_input.nextLine();
        if(user_choice.equals("y")){
            TCPMessage myCustomMessage = new TCPMessage();
            myCustomMessage.setMessage("TEST MESSAGE");
            myCustomMessage.setTcpMessageType(TCPMessageType.TEXT);

            for (String ipadd:R.ClientAddreses) {
                System.out.println("sending test message to"+ ipadd);
                TCPMessageEmmiter tcpMessageEmmiter = new TCPMessageEmmiter(myCustomMessage,ipadd,8888);
                tcpMessageEmmiter.start();
            }
        }

        /*
        TODO architecture for connections over the internet => TCP spider web like
        TODO set up propagating packets
        set up the message listener to call the message emitter to propagate messages
        challenge while doing so is facing p2p architectures where the message might loopcak to the same device more than twice
        */


    }
}
