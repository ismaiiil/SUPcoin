package com.supinfo.supchain.helpers;

import com.supinfo.supchain.Main;
import com.supinfo.supchain.enums.Environment;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.TCPMessageType;
import com.supinfo.supchain.models.TCPMessage;
import com.supinfo.supchain.models.Updater;
import com.supinfo.supchain.networking.TCPUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class ExternalIPGet extends Thread {
    private CLogger cLogger = new CLogger(this.getClass());
    private String oldExternalIPAddress = RUtils.externalIP;
    private String newExternalIPAddress = "";
    @Override
    public void run(){
        if(RUtils.env == Environment.PRODUCTION){
            try {

                cLogger.log(LogLevel.LOW,"Fetching your public IP...");
                URL url = new URL("https://myexternalip.com/raw");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed to connect to internet : HTTP error code : "
                            + conn.getResponseCode());
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream())));

                String output;
                while ((output = br.readLine()) != null) {
                    newExternalIPAddress = output;
                }

                conn.disconnect();

            } catch (IOException e) {

                e.printStackTrace();

            }
        }else{
            String everything = "";
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(".config/debugApi.txt"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                everything = sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            newExternalIPAddress = everything.replaceAll("\\s+","");
        }

        if(!newExternalIPAddress.equals(oldExternalIPAddress)){
            cLogger.log(LogLevel.LOW,"WARNING: PUBLIC IP HAS BEEN CHANGED!");
            RUtils.externalIP = newExternalIPAddress;
            Updater updater = new Updater(oldExternalIPAddress,newExternalIPAddress);
            TCPMessage tcpMessage = new TCPMessage<>(TCPMessageType.UPDATE_SENDER_IP, false, 0, updater);
            TCPUtils.multicastAll(tcpMessage,RUtils.externalIP);
        }

        cLogger.log(LogLevel.LOW,"Public IP successfully retrieved: " + RUtils.externalIP);

    }


}
