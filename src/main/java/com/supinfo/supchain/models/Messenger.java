package com.supinfo.supchain.models;

import java.io.Serializable;

public class Messenger implements Serializable {
    private String searchingIP;
    private String newPeerAddress;

    public String getSearchingIP() {
        return searchingIP;
    }

    public void setSearchingIP(String searchingIP) {
        this.searchingIP = searchingIP;
    }

    public String getNewPeerAddress() {
        return newPeerAddress;
    }

    public void setNewPeerAddress(String newPeerAddress) {
        this.newPeerAddress = newPeerAddress;
    }

    public Messenger(String searchingIP) {
        this.searchingIP = searchingIP;
    }
}
