package com.supinfo.supchain.helpers;

import com.supinfo.supchain.Main;
import com.supinfo.supchain.enums.Environment;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.Role;
import com.supinfo.supchain.networking.TCPUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ConfigManager {
    private static CLogger cLogger = new CLogger(ConfigManager.class);

    public static void loadConfigFromXml() {
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
            if(!TCPUtils.isValidIP(RUtils.bootstrapNode)){
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

    public static void saveConfig() {
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

    public static void saveConfigManual(){
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

    public static String getManualText(){
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
}
