package com.supinfo.supchain.helpers;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.enums.Role;
import com.supinfo.supchain.models.TCPMessage;

import java.util.HashSet;

public class RUtils {
    //using hashsets will allow for a margin of error when trying to add duplicate entries
    //since we heavily want unique values in those Sets, also Hashsets have a .contains search speed of O(1)
    //compared to O(n) for normal ArrayLists
    public static LogLevel logLevel;
    public static Role myRole;
    public static String externalIP;
    public static HashSet<String> localClientAddresses = new HashSet<>();
    public static HashSet<String> externalClientAddresses = new HashSet<>();
    public static HashSet<String> cacheMessages = new HashSet<>();
    public static HashSet<String> oldCacheMessages = new HashSet<>();
    public static int maxCacheSize = 1000;
    public static int tcpPort = 8888;
    public static int udpPort = 8888;
    public static int minNumberOfConnections = 2;
    public static int maxNumberOfConnections = 2;
    public static long messengerTimeout = 10;

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
        return ("LogLevel: " + RUtils.logLevel + "\n" +
                "Role: " + RUtils.myRole + "\n" +
                "ExternalIP: " + RUtils.externalIP + "\n" +
                "localClientAddresses: " + RUtils.localClientAddresses + "\n" +
                "externalClientAddresses: " + RUtils.externalClientAddresses + "\n" +
                "cacheMessages: " + RUtils.cacheMessages + "\n" +
                "oldCacheMessages: " + RUtils.oldCacheMessages + "\n" +
                "maxCacheSize: " + RUtils.maxCacheSize + "\n" +
                "tcpPort: " + RUtils.tcpPort + "\n" +
                "udpPort: " + RUtils.udpPort + "\n" +
                "minNumberOfConnections: " + RUtils.minNumberOfConnections + "\n");
    }

}
