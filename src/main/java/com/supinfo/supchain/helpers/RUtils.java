package com.supinfo.supchain.helpers;

import com.supinfo.supchain.enums.Environment;
import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.Role;
import com.supinfo.shared.Network.TCPMessage;

import javax.xml.bind.annotation.*;
import java.util.HashSet;
import com.supinfo.supchain.blockchain.wallet.Wallet;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class RUtils {

    //using hashsets will allow for a margin of error when trying to add duplicate entries
    //since we heavily want unique values in those Sets, also Hashsets have a .contains search speed of O(1)
    //compared to O(n) for normal ArrayLists
    public static Environment env = Environment.PRODUCTION;
    public static LogLevel logLevel = LogLevel.BASIC;
    public static Role myRole = Role.RDV;
    public static String externalIP = "";
    public static HashSet<String> localClientAddresses = new HashSet<>();
    public static HashSet<String> externalClientAddresses = new HashSet<>();
    public static HashSet<String> cacheMessages = new HashSet<>();
    public static HashSet<String> oldCacheMessages = new HashSet<>();
    public static HashSet<String> pingedAddresses = new HashSet<>();
    public static int maxCacheSize = 1000;
    public static int tcpPort = 8888;
    public static int udpPort = 8888;
    public static int minNumberOfConnections = 2;
    public static int maxNumberOfConnections = 3;
    public static long messengerTimeout = 10;
    public static long connectionLatency = 10000;
    public static long pingPongTaskPeriod = 40000; //TODO make this xml
    public static long externalIpCheckPeriod = 40000;
    public static float minimumTransaction = 0.1f;
    public static String bootstrapNode = "";
    public static Wallet wallet;

    //By default, bitcoin-core allows up to 125 connections to different peers

    public Environment getEnv() {
        return env;
    }

    @XmlElement(name = "environment")
    public void setEnv(Environment env) {
        RUtils.env = env;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    @XmlElement(name = "logLevel")
    public void setLogLevel(LogLevel logLevel) {
        RUtils.logLevel = logLevel;
    }

    public Role getMyRole() {
        return myRole;
    }

    @XmlElement(name = "myRole")
    public void setMyRole(Role myRole) {
        RUtils.myRole = myRole;
    }

    public String getExternalIP() {
        return externalIP;
    }

    @XmlElement(name = "externalIP")
    public void setExternalIP(String externalIP) {
        RUtils.externalIP = externalIP;
    }


    public HashSet<String> getLocalClientAddresses() {
        return localClientAddresses;
    }

    @XmlElementWrapper(name = "localClientAddresses")
    @XmlElement(name = "address")
    public void setLocalClientAddresses(HashSet<String> localClientAddresses) {
        RUtils.localClientAddresses = localClientAddresses;
    }

    public HashSet<String> getExternalClientAddresses() {
        return externalClientAddresses;
    }

    @XmlElementWrapper(name = "externalClientAddresses")
    @XmlElement(name = "address")
    public void setExternalClientAddresses(HashSet<String> externalClientAddresses) {
        RUtils.externalClientAddresses = externalClientAddresses;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    @XmlElement(name = "maxCacheSize")
    public void setMaxCacheSize(int maxCacheSize) {
        RUtils.maxCacheSize = maxCacheSize;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    //@XmlElement(name = "tcpPort")
    public void setTcpPort(int tcpPort) {
        RUtils.tcpPort = tcpPort;
    }

    public int getUdpPort() {
        return udpPort;
    }

    //@XmlElement(name = "udpPort")
    public void setUdpPort(int udpPort) {
        RUtils.udpPort = udpPort;
    }

    public int getMinNumberOfConnections() {
        return minNumberOfConnections;
    }

    @XmlElement(name = "minNumberOfConnections")
    public void setMinNumberOfConnections(int minNumberOfConnections) {
        RUtils.minNumberOfConnections = minNumberOfConnections;
    }

    public int getMaxNumberOfConnections() {
        return maxNumberOfConnections;
    }

    @XmlElement(name = "maxNumberOfConnections")
    public void setMaxNumberOfConnections(int maxNumberOfConnections) {
        RUtils.maxNumberOfConnections = maxNumberOfConnections;
    }

    public long getMessengerTimeout() {
        return messengerTimeout;
    }

    @XmlElement(name = "messengerTimeout")
    public void setMessengerTimeout(long messengerTimeout) {
        RUtils.messengerTimeout = messengerTimeout;
    }

    public String getBootstrapNode() {
        return bootstrapNode;
    }

    @XmlElement(name = "bootstrapnode")
    public void setBootstrapNode(String bootstrapNode) {
        RUtils.bootstrapNode = bootstrapNode;
    }




    public static void addMessageToCache(TCPMessage message){
        String messageHash = message.getMessageHash();
        if(cacheMessages.size()>=maxCacheSize){
            oldCacheMessages.clear();
            oldCacheMessages = (HashSet) cacheMessages.clone();
            cacheMessages.clear();
        }
        cacheMessages.add(messageHash);
    }

    public static boolean isMessageCached(TCPMessage message){
        String messageHash = message.getMessageHash();
        return cacheMessages.contains(messageHash) || oldCacheMessages.contains(messageHash);
    }

    public static HashSet<String> allClientAddresses(){
        HashSet<String> total = new HashSet<>();
        total.addAll(localClientAddresses);
        total.addAll(externalClientAddresses);
        return total;
    }



    public static String getStats(){
        return ("\n" +
                "environment: " + RUtils.env + "\n" +
                "LogLevel: " + RUtils.logLevel + "\n" +
                "Role: " + RUtils.myRole + "\n" +
                "ExternalIP: " + RUtils.externalIP + "\n" +
                "localClientAddresses: " + RUtils.localClientAddresses + "\n" +
                "externalClientAddresses: " + RUtils.externalClientAddresses + "\n" +
//                "cacheMessages: " + RUtils.cacheMessages + "\n" +
//                "oldCacheMessages: " + RUtils.oldCacheMessages + "\n" +
                "maxCacheSize: " + RUtils.maxCacheSize + "\n" +
                "tcpPort: " + RUtils.tcpPort + "\n" +
                "udpPort: " + RUtils.udpPort + "\n" +
                "minNumberOfConnections: " + RUtils.minNumberOfConnections + "\n" +
                "maxNumberOfConnections: " + RUtils.maxNumberOfConnections + "\n" +
                "messengerTimeout: " + RUtils.messengerTimeout + "\n" +
                "bootstrapNode: " + RUtils.bootstrapNode + "\n");
    }

}
