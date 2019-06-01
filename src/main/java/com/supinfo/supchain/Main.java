package com.supinfo.supchain;

import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.supchain.helpers.*;
import com.supinfo.supchain.networking.Threads.LAN.UDPClientDiscovery;
import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.supchain.networking.Tasks.PingPongTask;
import com.supinfo.supchain.networking.Threads.PingPongThread;
import com.supinfo.supchain.networking.Threads.TCPMessageListener;
import com.supinfo.supchain.networking.Utils.TCPUtils;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.util.*;

import static java.lang.Thread.sleep;

public class Main {
    private static CLogger cLogger = new CLogger(Main.class);
    private static HelpFormatter formatter = new HelpFormatter();
    private static Options options = new Options();
    public static Scanner user_input = new Scanner(System.in);

    public static void main(String[] args) throws InterruptedException, SocketException, FileNotFoundException, UnsupportedEncodingException {
        CommandLine cmd = getCommandLine(args);


        if(cmd.hasOption("c")){
            if(cmd.hasOption("i")){
                cLogger.println("Creating new config file as xml");
                ConfigManager.saveConfig();
            }
            if(cmd.hasOption("m")){
                cLogger.println("Below is the config manual:" + ConfigManager.getManualText());

            }
            if(cmd.hasOption("a")){
                PrintWriter writer = null;
                cLogger.println("Creating new apiDebug file as txt");
                new File("./.config").mkdirs();
                writer = new PrintWriter(".config/debugApi.txt", "UTF-8");
                writer.println("replace this text with the Ip you want to use");
                writer.close();
            }
        }
        else if(cmd.hasOption("r")){

            cLogger.println("Welcome to SUPCoin core");
            TCPMessageListener messageListener = new TCPMessageListener(RUtils.tcpPort);
            messageListener.start();
            ConfigManager.loadConfigFromXml();

            switch (RUtils.myRole){
                case RDV:
                    TCPUtils.startRDVRoutines();

                    //ping addresses and check for pong, remove addresses if no pong received
                    SpinnerCLI spinnerCLI = new SpinnerCLI("Checking cached nodes: ");
                    spinnerCLI.start();

                    //pinging all addresses to see if they are still up
                    PingPongThread ppthread = new PingPongThread();
                    ppthread.start();
                    ppthread.join();

                    Timer time = new Timer(); // Instantiate Timer Object
                    PingPongTask ppt = new PingPongTask(); // Instantiate SheduledTask class
                    time.schedule(ppt, 1000, RUtils.pingPongTaskPeriod); // Create Repetitively task for every 1 secs
                    spinnerCLI.showProgress = false;

                    //we are also going program a function to periodically check the external IP address and take necessary actions

                    //connect to bootnode only if minimum requirements not met
                    if(RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections){
                        TCPUtils.connectToNode(RUtils.bootstrapNode);
                    }

                    //have a thread that will check if minimum number of connections is satisfied
                    //this is done in the PingPongTask thread
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

                    TCPMessage myCustomMessage = new TCPMessage<>(TCPMessageType.VERIFY,10,null);
                    TCPUtils.multicastAll(myCustomMessage,"none");
                }
                if(user_choice.equals("exit")){
                    ConfigManager.saveConfig();
                    System.exit(1);
                }

            }


        }else{
            cLogger.println("Please check help for using this app");
            formatter.printHelp("SUPCOIN", options);
        }


    }


    private static void promptDiscoverRDV() {
        Scanner user_input = new Scanner(System.in);
        cLogger.printInput("Do you want to initiate network discovery of an RDV, this will invalidate your external connections (y/n)");
        String userChoice = user_input.nextLine();
        if(userChoice.equals("y")){
            RUtils.externalClientAddresses.clear();
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
            Option debugApi = new Option("a", "debugApi", false, "use with -c --config to create a debugApi.txt file");
        Option start = new Option("r", "run", false, "Run the program");
        options.addOption(config);
            options.addOption(init);
            options.addOption(manual);
            options.addOption(debugApi);
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


}
