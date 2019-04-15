package models;

import java.io.Serializable;

public class Messenger implements Serializable {
    private String origin;
    private String newPeerAddress;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getNewPeerAddress() {
        return newPeerAddress;
    }

    public void setNewPeerAddress(String newPeerAddress) {
        this.newPeerAddress = newPeerAddress;
    }

    public Messenger(String origin, String newPeerAddress) {
        this.origin = origin;
        this.newPeerAddress = newPeerAddress;
    }
}
