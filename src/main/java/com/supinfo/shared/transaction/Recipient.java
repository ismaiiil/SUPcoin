package com.supinfo.shared.transaction;

import java.io.Serializable;
import java.security.PublicKey;

public class Recipient implements Serializable {
    public static final long serialVersionUID = 4444444444L;
    private PublicKey receiver;

    public PublicKey getReceiver() {
        return receiver;
    }

    public void setReceiver(PublicKey receiver) {
        this.receiver = receiver;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    private float value;
}
