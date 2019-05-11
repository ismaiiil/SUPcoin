package com.supinfo.supchain.models;

import java.io.Serializable;

public class PingPong implements Serializable {
    private String origin;

    public PingPong(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
