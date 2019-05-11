package com.supinfo.supchain.networking;

import java.util.Date;
import java.util.TimerTask;

public class ExternalIPCheckTask extends TimerTask {
    Date now; // to display current time

    // Add your task here
    public void run() {
        now = new Date(); // initialize date
        System.out.println("Time is :" + now); // Display current time
    }
}