package com.supinfo.supchain;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.Role;
import com.supinfo.supchain.enums.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.LAN.UDPClientDiscovery;
import com.supinfo.supchain.LAN.UDPMessageListener;
import com.supinfo.supchain.models.TCPMessage;
import com.supinfo.supchain.helpers.ExternalIPGet;
import com.supinfo.supchain.networking.TCPMessageListener;
import com.supinfo.supchain.networking.TCPUtils;
import com.supinfo.supchain.networking.UPnPManager;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) throws InterruptedException {
        RUtils.logLevel = LogLevel.SUPERDUPERHIGH;
        CLogger cLogger = new CLogger(Main.class);

        Scanner user_input = new Scanner(System.in);
        cLogger.println("Welcome to SUPCoin core");
        cLogger.println("This is prior setup before you start mining");
        cLogger.printInput("Do you want to use this machine as an RDV(Rendez-Vous) peer or EDGE peer");
        String userChoice = user_input.nextLine();
        while (true){
            try{
                RUtils.myRole = Role.valueOf(userChoice);
                break;
            }catch (IllegalArgumentException e){
                //cLogger.log(LogLevel.EXCEPTION,"wrong choice please try either inputting EDGE or RDV");
                cLogger.printInput("wrong choice please try either inputting EDGE or RDV");
                userChoice = user_input.nextLine();
            }
        }
        cLogger.println("Your choose the Role of " + RUtils.myRole.toString());

        TCPMessageListener messageListener = new TCPMessageListener(RUtils.tcpPort);
        messageListener.start();

        switch (RUtils.myRole){
            case EDGE:
                cLogger.printInput("Do you want to initiate network discovery of an RDP (y/n)");
                userChoice = user_input.nextLine();

                if(userChoice.equals("y")){
                    cLogger.println("Searching for an RDV...");
                    Thread client = new Thread(new UDPClientDiscovery(3));
                    client.start();
                    try {
                        client.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(!RUtils.localClientAddresses.isEmpty()){
                        cLogger.println("Successfully found the RDV node at: "+ RUtils.localClientAddresses);
                    }
                }
                else{
                    cLogger.println("Closing...");
                    System.exit(0);
                }

                break;
            case RDV:
                Thread uPnPManagerThread = new Thread(new UPnPManager());
                uPnPManagerThread.start();
                ExternalIPGet externalIPGet = new ExternalIPGet();
                externalIPGet.run();
                externalIPGet.join();
                cLogger.log(LogLevel.LOW,"Public IP successfully retrieved: " + RUtils.externalIP);

                Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
                discoveryThread.start();
                cLogger.printInput("input the public ip address of another optional RDV, write skip to skip this step.");
                userChoice = user_input.nextLine();
                if (!userChoice.contains("skip") && !RUtils.allClientAddresses().contains(userChoice) && !userChoice.equals(RUtils.externalIP)) {
                    TCPMessage requestMessage = new TCPMessage(TCPMessageType.REQUEST_CONNECTION,false,0);
                    TCPUtils.unicast(requestMessage,userChoice);
                }else{
                    cLogger.println("skipping, you already have this IP in your list or your input is your own External IP");
                }
                cLogger.println("moving on...");
                break;
        }


        cLogger.printInput("do you want to test a propagatable message...");

        while(true){
            String user_choice = user_input.nextLine();

            if(user_choice.equals("stats")){
                cLogger.println(RUtils.getStats());
            }
            if(user_choice.equals("yes")){

                TCPMessage myCustomMessage = new TCPMessage(TCPMessageType.VERIFY,true,10);
                TCPUtils.multicastAll(myCustomMessage,"none");
            }

        }


    }
}
