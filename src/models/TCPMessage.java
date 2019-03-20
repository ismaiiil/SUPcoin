package models;

import enums.TCPMessageType;

import java.io.Serializable;

public class TCPMessage implements Serializable {
    private String message;
    private TCPMessageType tcpMessageType;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TCPMessageType getTcpMessageType() {
        return tcpMessageType;
    }

    public void setTcpMessageType(TCPMessageType tcpMessageType) {
        this.tcpMessageType = tcpMessageType;
    }


}
