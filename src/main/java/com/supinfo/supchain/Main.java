package com.supinfo.supchain;

import com.supinfo.supchain.enums.Environment;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.Role;
import com.supinfo.supchain.enums.TCPMessageType;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.LAN.UDPClientDiscovery;
import com.supinfo.supchain.LAN.UDPMessageListener;
import com.supinfo.supchain.models.PingPong;
import com.supinfo.supchain.models.TCPMessage;
import com.supinfo.supchain.helpers.ExternalIPGet;
import com.supinfo.supchain.networking.ExternalIPCheckTask;
import com.supinfo.supchain.networking.TCPMessageListener;
import com.supinfo.supchain.networking.TCPUtils;
import com.supinfo.supchain.networking.UPnPManager;
import org.apache.commons.cli.*;

import javax.xml.bind.*;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public class Main {
    private static CLogger cLogger = new CLogger(Main.class);
    private static HelpFormatter formatter = new HelpFormatter();
    private static Options options = new Options();

    public static void main(String[] args) throws InterruptedException, SocketException {
        CommandLine cmd = getCommandLine(args);
        Scanner user_input = new Scanner(System.in);

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
            cLogger.println("Welcome to SUPCoin core");
            loadConfigFromXml();
            TCPMessageListener messageListener = new TCPMessageListener(RUtils.tcpPort);
            messageListener.start();

            switch (RUtils.myRole){
                case RDV:
                    startRDVRoutines();

                    //ping addresses and check for pong
                    /*
                    * brodcast a PING message
                    * add PINGed addresses to pingedAddresses
                    * when a node receives a PING the message must have teh same origin and same
                    * wait until timeout of (5 seconds, make this timeout configurable as connection minLatency)
                    * for each PONG message received remove the address from the pingedAddresses list
                    * */

                    TCPMessage pingMessage = new TCPMessage<>(TCPMessageType.PING,false,0, new PingPong(RUtils.externalIP));
                    TCPUtils.multicastAll(pingMessage,RUtils.externalIP);


                    //we are also going program a function to periodically check the external IP address and take necessary actions

                    //connect to bootnode only if minimum requirements not met
                    if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                        connectToNode(RUtils.bootstrapNode);
                    }

                    //have a thread that will check if minimum number of connections is satisfied

                    break;
                case EDGE:
                    promptDiscoverRDV();
                    break;
            }

            cLogger.printInput("do you want to test a propagatable message...");

            while(true){
                String user_choice = user_input.nextLine();

                if(user_choice.equals("stats")){
                    cLogger.println(RUtils.getStats());
                }
                if(user_choice.equals("yes")){

                    TCPMessage myCustomMessage = new TCPMessage<>(TCPMessageType.VERIFY,true,10,null);
                    TCPUtils.multicastAll(myCustomMessage,"none");
                }
                if(user_choice.equals("exit")){
                    saveConfig();
                    System.exit(1);
                }

            }


        }else{
            cLogger.println("Please check help for using this app");
            formatter.printHelp("SUPCOIN", options);
        }


    }

    private static void connectToNode(String node) throws SocketException {
        List<String> adapterAddresses = UDPMessageListener.getAdapterAdresses();
        if(!node.equals(RUtils.externalIP) && !adapterAddresses.contains(node)){
            if(isValidIP(node) && isValidIP(RUtils.externalIP)){
                cLogger.println("We are now contacting a node!");
                TCPMessage requestMessage = new TCPMessage<>(TCPMessageType.REQUEST_CONNECTION,false,0,null);
                TCPUtils.unicast(requestMessage,node);
            }else{
                cLogger.println("Please make sure the bootnode or IP supplied and external IP is valid!");
                saveConfig();
                System.exit(1);
            }

        }else{
            cLogger.println("You have setup the bootnode to be this node, waiting and listening for other nodes");
        }
    }

    private static void startRDVRoutines() throws InterruptedException {
        if(RUtils.env == Environment.PRODUCTION){

            Thread uPnPManagerThread = new Thread(new UPnPManager());
            uPnPManagerThread.start();

            ExternalIPGet externalIPGet = new ExternalIPGet();
            externalIPGet.run();
            externalIPGet.join();
            cLogger.log(LogLevel.LOW,"Public IP successfully retrieved: " + RUtils.externalIP);

            //start a thread that will monitor the external IP and take necessary actions
//            Timer time = new Timer(); // Instantiate Timer Object
//            ExternalIPCheckTask st = new ExternalIPCheckTask(); // Instantiate SheduledTask class
//            time.schedule(st, 0, 1000); // Create Repetitively task for every 1 secs
        }else{
            cLogger.log(LogLevel.LOW,"DEBUG MODE using IP from config file: " + RUtils.externalIP);
        }

        Thread discoveryThread = new Thread(UDPMessageListener.getInstance());
        discoveryThread.start();
    }

    private static void promptDiscoverRDV() {
        Scanner user_input = new Scanner(System.in);
        cLogger.printInput("Do you want to initiate network discovery of an RDV (y/n)");
        String userChoice = user_input.nextLine();
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
            if(!isValidIP(RUtils.bootstrapNode)){
                cLogger.println("Please use a valid IPv4 format for the bootstrap node!");
                System.exit(1);
            }
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

    private static boolean isValidIP(String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


}
