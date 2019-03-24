import enums.LogLevel;
import enums.Role;
import enums.TCPMessageType;
import helpers.CLogger;
import helpers.R;
import LAN.UDPClientDiscovery;
import LAN.UDPMessageListener;
import models.TCPMessage;
import networking.TCPMessageEmmiter;
import networking.TCPMessageListener;
import networking.TCPUtils;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        CLogger.logLevel = LogLevel.LOW;

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

        TCPMessageListener messageListener = new TCPMessageListener(8888);
        messageListener.start();

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
                else{
                    System.out.println("Closing...");
                    System.exit(0);
                }

                break;
            case RDV:
                Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
                discoveryThread.start();
                System.out.println("input the public ip address of another optional RDV, write skip to skip this step.");
                userChoice = user_input.nextLine();
                if (!userChoice.contains("skip")) {
                    TCPMessage requestMessage = new TCPMessage(TCPMessageType.REQUEST_CONNECTION,false);
                    TCPUtils.unicast(requestMessage,userChoice);
                }
                System.out.println("moving on...");
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

        System.out.println("do you want to test a propagatable message...");

        while(true){
            String user_choice = user_input.nextLine();

            if(!user_choice.equals("")){
                TCPMessage myCustomMessage = new TCPMessage(TCPMessageType.VERIFY,true);
                TCPUtils.multicast(myCustomMessage,"none");
            }
        }


        /*
        TODO architecture for connections over the internet => TCP spider web like


        RDV can either manually connect to a specific RDV or by default the ip of the seeder RDV is hardcoded so that
        we are able to redirect the RDV to a proper connection.

        have a way to prevent multiple RDVS on hte same local network as this may cause bugs.

        prevent user from adding an RDV which is already in the list of clients connected

        */

    }
}
