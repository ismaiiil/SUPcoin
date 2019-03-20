import enums.LogLevel;
import enums.Role;
import helpers.CLogger;
import helpers.R;
import localNetworking.ClientBroadcaster;
import localNetworking.DiscoveryThread;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        CLogger.logLevel = LogLevel.LOW;

        Scanner user_input = new Scanner(System.in);
        System.out.println("Welcome to SUPcoin core");
        System.out.println("This is prior setup before you start mining");
        System.out.println("Do you want to use this machine as an RDV peer or EDGE peer");
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
                Thread client = new Thread(new ClientBroadcaster(3));
                client.start();
                try {
                    client.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(R.RDVadress != null){
                    System.out.println("Sucessfully found teh RDV node at: "+ R.RDVadress);
                }
                break;
            case RDV:
                Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
                discoveryThread.start();
                break;

        }

    }
}
