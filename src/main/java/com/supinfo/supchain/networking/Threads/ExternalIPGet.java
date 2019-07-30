package com.supinfo.supchain.networking.Threads;

import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.supchain.enums.Environment;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.networking.Utils.TCPUtils;
import com.supinfo.supchain.networking.models.Updater;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExternalIPGet extends Thread {
    private CLogger cLogger = new CLogger(this.getClass());
    private String oldExternalIPAddress = RUtils.externalIP;
    private String newExternalIPAddress = "";

    @Override
    public void run() {
        if (RUtils.env == Environment.PRODUCTION) {
            try {

                cLogger.log(LogLevel.NETWORK, "Fetching your public IP...");
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
        } else {
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
            newExternalIPAddress = everything.replaceAll("\\s+", "");
        }

        if (!newExternalIPAddress.equals(oldExternalIPAddress)) {
            cLogger.log(LogLevel.NETWORK, "WARNING: PUBLIC IP HAS BEEN CHANGED!");
            RUtils.externalIP = newExternalIPAddress;
            Updater updater = new Updater(oldExternalIPAddress, newExternalIPAddress);
            TCPMessage tcpMessage = new TCPMessage<>(TCPMessageType.UPDATE_SENDER_IP, updater);
            TCPUtils.multicastAll(tcpMessage, RUtils.externalIP);
        }

        cLogger.log(LogLevel.NETWORK, "Public IP successfully retrieved: " + RUtils.externalIP);

    }


}
