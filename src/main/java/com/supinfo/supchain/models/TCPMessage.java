package com.supinfo.supchain.models;

import com.supinfo.supchain.enums.TCPMessageType;

import java.util.*;

import java.io.Serializable;

import com.supinfo.supchain.helpers.StringUtil;

public class TCPMessage<T> implements Serializable {
    private TCPMessageType tcpMessageType;
    private String messageHash;
    private long dateTime;
    private long propagationTimeout;
    private boolean propagatable;
    private T data;

    public TCPMessage(TCPMessageType tcpMessageType,boolean propagatable, long propagationTimeout, T data){
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.dateTime = cal.getTimeInMillis();
        this.propagationTimeout = propagationTimeout * 1000;
        this.tcpMessageType = tcpMessageType;
        this.messageHash = calculateHash();
        this.propagatable = propagatable;
        this.data = data;
    }

    private String calculateHash(){
        Random rand = new Random();
        int n = rand.nextInt(100000);
        String stringdata = "jeff";
        if (data != null) {
            stringdata = data.toString();
        }

        return StringUtil.applySha256(tcpMessageType.toString() + dateTime + n + stringdata);
    }

    public void setData(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
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

    public long getPropagationTimeout() {
        return propagationTimeout;
    }

    public void setPropagationTimeout(long propagationTimeout) {
        this.propagationTimeout = propagationTimeout;
    }

    public boolean isAlive(){
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTime = cal.getTimeInMillis();
        if(propagationTimeout == 0){
            return true;
        }
        return currentTime < dateTime + propagationTimeout;
    }
}
