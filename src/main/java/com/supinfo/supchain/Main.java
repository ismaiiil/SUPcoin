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
import org.apache.commons.cli.*;

import javax.xml.bind.*;
import java.io.File;
import java.util.Scanner;

public class Main {
    public static CLogger cLogger = new CLogger(Main.class);

    public static void main(String[] args) throws InterruptedException{
        CommandLine cmd = getCommandLine(args);

        if(cmd.hasOption("init")){
            cLogger.println("Creating new config file as xml");
            saveConfig();

        }
        else if(cmd.hasOption("start")){
            loadConfigFromXml();
            /*
                find a way to set up bootstrap node, the very first node of our system
                have the ip hardcoded, we will have to settle with a local ip for now
                then the bootstrap node will try to connect to itself, will have to do
                a check for that
                TODO: will have to make an argument to configure XML file without having to directly edit it or use GUI?
                TODO:check self for bootstrap node
                TODO:have a series of machines with static ips
                TODO:have the external ip set to local ip in debug mode

            */

        }


    }

    private static CommandLine getCommandLine(String[] args) {
        Options options = new Options();
        Option input = new Option("i", "init", false, "init config file as XML");
        Option start = new Option("s", "start", false, "load xml config into program");
        options.addOption(input);
        options.addOption(start);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("SUPCOIN", options);

            System.exit(1);
        }
        return cmd;
    }

    private static void loadConfigFromXml() {
        cLogger.println("Loading from xml config file");
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(RUtils.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            // put a listener for advanced parsing errors
            jaxbUnmarshaller.setEventHandler(event -> {
                cLogger.println(event.getMessage());
                return false;
            });
            //inject the xml back into the running application
            RUtils rUtils = (RUtils) jaxbUnmarshaller.unmarshal( new File(".config/rUtils.xml") );
            cLogger.println("Last config successfully loaded!");
            cLogger.log(LogLevel.HIGH,RUtils.getStats());

        } catch (JAXBException e) {
            cLogger.println("An error has occurred while loading the config!, please make sure your fields are valid" +
                    " and that this file is present: ./config/rUtils.xml, you can recreate the config file by running with -i, --init");
        }
    }

    private static void saveConfig() {
        try {
            File file = new File(".config/rUtils.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(RUtils.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            RUtils rUtils = new RUtils();
            jaxbMarshaller.marshal(rUtils, file);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static void oldMain() throws InterruptedException {
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
                    cLogger.println("We are now contacting this RDV!");
                    TCPMessage requestMessage = new TCPMessage(TCPMessageType.REQUEST_CONNECTION,false,0);
                    TCPUtils.unicast(requestMessage,userChoice);
                }else{
                    cLogger.println("skipping, you already have this IP in your list or your input is your own External IP");
                }
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
