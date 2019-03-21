package models;

import enums.TCPMessageType;

import java.io.Serializable;
import java.util.Date;

import helpers.StringUtil;

public class TCPMessage implements Serializable {
    private String message;
    private TCPMessageType tcpMessageType;
    private String messageHash;
    private long dateTime;

    public TCPMessage(String message, TCPMessageType tcpMessageType){
        this.message = message;
        this.dateTime = new Date().getTime();
        this.tcpMessageType = tcpMessageType;
        this.messageHash = calculateHash();
    }

    private String calculateHash(){
        return StringUtil.applySha256("message" + tcpMessageType + dateTime);
    }

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


    public String getMessageHash() {
        return messageHash;
    }

    public void setMessageHash(String messageHash) {
        this.messageHash = messageHash;
    }
}
