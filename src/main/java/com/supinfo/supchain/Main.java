package com.supinfo.supchain;

import com.supinfo.supchain.enums.Environment;
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
import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    private static CLogger cLogger = new CLogger(Main.class);
    private static HelpFormatter formatter = new HelpFormatter();
    private static Options options = new Options();

    public static void main(String[] args) throws InterruptedException{
        CommandLine cmd = getCommandLine(args);

        if(cmd.hasOption("c")){
            if(cmd.hasOption("i")){
                cLogger.println("Creating new config file as xml");
                saveConfig();
            }
            if(cmd.hasOption("m")){
                cLogger.println("Below is the config manual:" + getManualText());

            }
        }
        else if(cmd.hasOption("r")){
            loadConfigFromXml();
            /*
                find a way to set up bootstrap node, the very first node of our system
                have the ip hardcoded, we will have to settle with a local ip for now
                then the bootstrap node will try to connect to itself, will have to do
                a check for that
                TODO:check self for bootstrap node
                TODO:have a series of machines with static ips
                TODO:have the external ip set to local ip in debug mode
            */

            cLogger.println("Welcome to SUPCoin core");
            TCPMessageListener messageListener = new TCPMessageListener(RUtils.tcpPort);
            messageListener.start();

            switch (RUtils.myRole){
                case RDV:
                    if(RUtils.env == Environment.PRODUCTION){
                        Thread uPnPManagerThread = new Thread(new UPnPManager());
                        uPnPManagerThread.start();
                        ExternalIPGet externalIPGet = new ExternalIPGet();
                        externalIPGet.run();
                        externalIPGet.join();
                        cLogger.log(LogLevel.LOW,"Public IP successfully retrieved: " + RUtils.externalIP);
                    }else{
                        cLogger.log(LogLevel.LOW,"DEBUG MODE using IP from config file: " + RUtils.externalIP);
                    }

                    Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
                    discoveryThread.start();

                    break;
                case EDGE:
                    promptDiscoverRDV();
                    break;
            }



        }else{
            cLogger.println("Please check help for using this app");
            formatter.printHelp("SUPCOIN", options);
        }


    }

    private static void promptDiscoverRDV() {
        Scanner user_input = new Scanner(System.in);
        String userChoice = user_input.nextLine();
        cLogger.printInput("Do you want to initiate network discovery of an RDV (y/n)");

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
    }

    private static CommandLine getCommandLine(String[] args) {

        Option config = new Option("c", "config", false, "manipulate config");
            Option init = new Option("i", "init", false, "use with -c --config to init a config file");
            Option manual = new Option("m", "manual", false, "use with -c --config to show the config manual");
        Option start = new Option("r", "run", false, "Run the program");
        options.addOption(config);
            options.addOption(init);
            options.addOption(manual);
        options.addOption(start);
        CommandLineParser parser = new DefaultParser();

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
            new File("./.config").mkdirs();
            File file = new File(".config/rUtils.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(RUtils.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            RUtils rUtils = new RUtils();
            jaxbMarshaller.marshal(rUtils, file);
            cLogger.println("Config successfully saved!");
            saveConfigManual();

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private static void saveConfigManual(){
        PrintWriter writer = null;
        try {
            new File("./.config").mkdirs();
            writer = new PrintWriter(".config/ConfigManual.txt", "UTF-8");
            writer.println(getManualText());
            writer.close();

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private static String getManualText(){
        return  "\n" + "---You can modify .config/rUtils.xml to change application variables that are loaded when the application starts" + "\n" +
                "---Failure to follow these guidelines will result in errors;"+ "\n" +
                "environment: " + Arrays.toString(Environment.values()) + "\n" +
                "LogLevel: " + Arrays.toString(LogLevel.values()) + "\n" +
                "Role: " + Arrays.toString(Role.values()) + "\n" +
                "external IP: " + "this can be hard coded for debug purposes else it will be overridden in production mode " + "\n" +
                "localClientAddresses: " + "IPv4 addresses in  x.x.x.x format as a string" + "\n" +
                "externalClientAddresses: " + "IPv4 addresses in  x.x.x.x format as a string" + "\n" +
                "maxCacheSize: " + "Max cache size of recently exchanged messages in INT" + "\n" +
                //"tcpPort: " + "TCP port to use as INT" + "\n" +
                //"udpPort: " + "UDP port to use as INT" + "\n" +
                "minNumberOfConnections: " + "The minimum number of connection to this node as INT" + "\n" +
                "maxNumberOfConnections: " + "The maximum number of connection to this node as INT" + "\n" +
                "messengerTimeout: " + "Timeout when looking for redundant connections for added performance" + "\n" +
                "bootstrapNode: " + "IP address of the Bootnode" + "\n";
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

//                if(userChoice.equals("y")){
//                    cLogger.println("Searching for an RDV...");
//                    Thread client = new Thread(new UDPClientDiscovery(3));
//                    client.start();
//                    try {
//                        client.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    if(!RUtils.localClientAddresses.isEmpty()){
//                        cLogger.println("Successfully found the RDV node at: "+ RUtils.localClientAddresses);
//                    }
//                }
//                else{
//                    cLogger.println("Closing...");
//                    System.exit(0);
//                }

                break;
            case RDV:
//                Thread uPnPManagerThread = new Thread(new UPnPManager());
//                uPnPManagerThread.start();
//                ExternalIPGet externalIPGet = new ExternalIPGet();
//                externalIPGet.run();
//                externalIPGet.join();
//                cLogger.log(LogLevel.LOW,"Public IP successfully retrieved: " + RUtils.externalIP);
//
//                Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
//                discoveryThread.start();
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
