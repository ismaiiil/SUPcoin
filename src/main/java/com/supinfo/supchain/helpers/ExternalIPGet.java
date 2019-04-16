package com.supinfo.supchain.helpers;

import com.supinfo.supchain.enums.LogLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExternalIPGet extends Thread {
    private CLogger cLogger = new CLogger(this.getClass());
    @Override
    public void run(){
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
                RUtils.externalIP = output;
            }

            conn.disconnect();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }


}
