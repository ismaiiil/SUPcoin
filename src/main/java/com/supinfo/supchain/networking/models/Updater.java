package com.supinfo.supchain.networking.models;

import java.io.Serializable;

public class Updater implements Serializable {
    private String oldIP;
    private String newIP;

    public Updater(String oldIP, String newIP) {
        this.oldIP = oldIP;
        this.newIP = newIP;
    }

    public String getOldIP() {
        return oldIP;
    }

    public void setOldIP(String oldIP) {
        this.oldIP = oldIP;
    }

    public String getNewIP() {
        return newIP;
    }

    public void setNewIP(String newIP) {
        this.newIP = newIP;
    }
}
