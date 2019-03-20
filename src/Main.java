import enums.LogLevel;
import enums.Role;
import helpers.CLogger;
import helpers.R;
import localNetworking.UDPClientDiscovery;
import localNetworking.UDPMessageListener;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        CLogger.logLevel = LogLevel.NONE;

        Scanner user_input = new Scanner(System.in);
        System.out.println("Welcome to SUPcoin core");
        System.out.println("This is prior setup before you start mining");
        System.out.println("Set up static IP for this machine");
        System.out.println("Do you want to use this machine as an RDV(Rendez-Vous) peer or EDGE peer");
        System.out.println("You can have only one RDV per Router connected to the internet");
        System.out.println("Set up port forwarding on port 8888");
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
        System.out.println("Your choose the myRole of " + R.myRole.toString());
        switch (R.myRole){
            case EDGE:
                Thread client = new Thread(new UDPClientDiscovery(3));
                client.start();
                try {
                    client.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!R.ClientAddreses.isEmpty()){
                    System.out.println("Sucessfully found the RDV node at: "+ R.ClientAddreses.get(0));
                }
                break;
            case RDV:
                Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
                discoveryThread.start();
                break;

        }

    }
}
