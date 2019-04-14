package models;

import enums.TCPMessageType;
import java.util.Random;

import java.io.Serializable;
import java.util.Date;

import helpers.StringUtil;

public class TCPMessage implements Serializable {
    private TCPMessageType tcpMessageType;
    private String messageHash;
    private long dateTime;
    private boolean propagatable;







    public TCPMessage(TCPMessageType tcpMessageType,boolean propagatable){
        this.dateTime = new Date().getTime();
        this.tcpMessageType = tcpMessageType;
        this.messageHash = calculateHash();
        this.propagatable = propagatable;
    }

    private String calculateHash(){
        Random rand = new Random();
        int n = rand.nextInt(100000);
        return StringUtil.applySha256(tcpMessageType.toString() + dateTime + n);
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

    public boolean isPropagatable() {
        return propagatable;
    }

    public void setPropagatable(boolean propagatable) {
        this.propagatable = propagatable;
    }
}
