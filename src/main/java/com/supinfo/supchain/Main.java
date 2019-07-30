package com.supinfo.supchain;

import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.supchain.blockchain.BlockchainHolder;
import com.supinfo.supchain.blockchain.BlockchainHolderManager;
import com.supinfo.supchain.blockchain.wallet.Wallet;
import com.supinfo.supchain.blockchain.wallet.WalletFileManager;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.ConfigManager;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.helpers.SpinnerCLI;
import com.supinfo.supchain.networking.Tasks.PingPongTask;
import com.supinfo.supchain.networking.Threads.LAN.UDPClientDiscovery;
import com.supinfo.supchain.networking.Threads.PingPongThread;
import com.supinfo.supchain.networking.Threads.TCPMessageListener;
import com.supinfo.supchain.networking.Utils.TCPUtils;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;
import java.util.Timer;

import static java.lang.Thread.sleep;

public class Main {
    private static CLogger cLogger = new CLogger(Main.class);
    private static HelpFormatter formatter = new HelpFormatter();
    private static Options options = new Options();
    public static Scanner user_input = new Scanner(System.in);
    public static String user_choice;
    public static BlockchainHolder blockchainHolder = BlockchainHolderManager.getInstance();


    public static void main(String[] args) throws InterruptedException, SocketException, FileNotFoundException, UnsupportedEncodingException {

        //adding SC security provider
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        CommandLine cmd = getCommandLine(args);


        if (cmd.hasOption("c")) {
            if (cmd.hasOption("i")) {
                cLogger.println("Creating new config file as xml");
                ConfigManager.saveConfig();
            }
            if (cmd.hasOption("m")) {
                cLogger.println("Below is the config manual:" + ConfigManager.getManualText());

            }
            if (cmd.hasOption("a")) {
                PrintWriter writer = null;
                cLogger.println("Creating new apiDebug file as txt");
                new File("./.config").mkdirs();
                writer = new PrintWriter(".config/debugApi.txt", "UTF-8");
                writer.println("replace this text with the Ip you want to use");
                writer.close();
            }
        } else if (cmd.hasOption("r")) {

            cLogger.println("Welcome to SUPCoin core");
            TCPMessageListener messageListener = new TCPMessageListener(RUtils.tcpPort, blockchainHolder);
            messageListener.start();
            ConfigManager.loadConfigFromXml();
            cLogger.println("Checking if a wallet has been configured for this node...");
            try {
                PrivateKey privateKey = WalletFileManager.loadPrivateKey("./.config");
                RUtils.wallet = new Wallet(WalletFileManager.derivePublicKey(privateKey), privateKey);
            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
                e.printStackTrace();
                cLogger.log(LogLevel.EXCEPTION, "Could Not find Your wallet under ./config/private.key");
                System.exit(1);
            } catch (IOException e) {
                cLogger.println("Do you want to create your private key?(Y/N)");
                while (true) {
                    user_choice = user_input.nextLine();
                    if (user_choice.equals("Y") || user_choice.equals("y")) {
                        RUtils.wallet = new Wallet();
                        try {
                            WalletFileManager.savePrivateKey("./.config", RUtils.wallet.getPrivateKey());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            cLogger.log(LogLevel.EXCEPTION, "Please Make sure you have initialised the app with -c -i," +
                                    "can also include -m for a manual, closing...");
                            System.exit(-1);
                        }
                        break;
                    } else if (user_choice.equals("N") || user_choice.equals("n")) {
                        cLogger.log(LogLevel.BASIC, "You can only mine if you have a wallet associated to this node, exiting...");
                        System.exit(-1);
                    }
                }

            }
            cLogger.println("Wallet Loaded successfully!");

            switch (RUtils.myRole) {
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
                    if (RUtils.externalClientAddresses.size() < RUtils.minNumberOfConnections) {
                        TCPUtils.connectToNode(RUtils.bootstrapNode);
                    }

                    //have a thread that will check if minimum number of connections is satisfied
                    //this is done in the PingPongTask thread
                    break;
                case EDGE:
                    promptDiscoverRDV();
                    break;
            }

            boolean isGenesis = false;
            while (RUtils.allClientAddresses().size() < 1) {

                cLogger.println("Your Node currently has no other peers to connect to");
                if (blockchainHolder.blockchain.size() > 0) {
                    cLogger.println("You already have a genesis block, moving to mining setup!");
                }
                cLogger.println("Do you want to wait for the node to discover Blockchain data on other peers or set this as a genesis block, use:(genesis/wait)");
                user_choice = user_input.nextLine();
                if (user_choice.equals("genesis")) {
                    blockchainHolder.initGenesisBlockchain();
                    blockchainHolder.validateBlockchain(blockchainHolder.blockchain);
                    isGenesis = true;
                    break;
                }
                if (user_choice.equals("wait")) {
                    SpinnerCLI spinnerCLI = new SpinnerCLI("Listening and Discovering Peers to download Blockchain data: ");
                    spinnerCLI.start();
                    while (RUtils.allClientAddresses().size() < 1) {
                    }
                    spinnerCLI.showProgress = false;
                    break;
                }

            }

            if (!isGenesis) {
                cLogger.println("At least one peer has been found, trying to download blockchain from peer!");
                boolean hasFoundBlockchain = blockchainHolder.requestBlockchainFromPeers();
                if (!hasFoundBlockchain) {
                    cLogger.println("No blockchain could be downloaded from the peers you are connected to, the node will now" +
                            "enter a passive mode until it is able to download a blockchain!");
                    while (!blockchainHolder.requestBlockchainFromPeers()) {
                        cLogger.println("Waiting for " + RUtils.initDownloadPeriod + " until we retry requesting peers for the chain!");
                        sleep(RUtils.initDownloadPeriod);
                    }
                }
            }


            cLogger.printInput("do you want to test a propagatable message...");


            //test blockchainHolder here

            while (true) {
                user_choice = user_input.nextLine();

                if (user_choice.equals("stats")) {
                    cLogger.println(RUtils.getStats());
                }
                if (user_choice.equals("yes")) {

                    TCPMessage myCustomMessage = new TCPMessage<>(TCPMessageType.VERIFY, 10, null);
                    TCPUtils.multicastAll(myCustomMessage, "none");
                }
                if (user_choice.equals("exit")) {
                    ConfigManager.saveConfig();
                    System.exit(1);
                }
                if (user_choice.equals("wallet")) {
                    WalletFileManager.dumpKeyPair(RUtils.wallet.getKeyPair());
                }
                if (user_choice.equals("dump")) {
                    cLogger.println("\n" + blockchainHolder.dumpBlockchain());
                }

            }


        } else {
            cLogger.println("Please check help for using this app");
            formatter.printHelp("SUPCOIN", options);
        }


    }


    private static void promptDiscoverRDV() {
        Scanner user_input = new Scanner(System.in);
        cLogger.printInput("Do you want to initiate network discovery of an RDV, this will invalidate your external connections (y/n)");
        String userChoice = user_input.nextLine();
        if (userChoice.equals("y")) {
            RUtils.externalClientAddresses.clear();
            cLogger.println("Searching for an RDV...");
            Thread client = new Thread(new UDPClientDiscovery(3));
            client.start();
            try {
                client.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!RUtils.localClientAddresses.isEmpty()) {
                cLogger.println("Successfully found the RDV node at: " + RUtils.localClientAddresses);
            }
        } else {
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
