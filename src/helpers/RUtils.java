package helpers;

import enums.Role;
import models.TCPMessage;

import java.util.HashSet;

public class RUtils {
    //using hashsets will allow for a margin of error when trying to add duplicate entries
    //since we heavily want unique values in those Sets, also Hashsets have a .contains search speed of O(1)
    //compared to O(n) for normal ArrayLists
    public static Role myRole;
    public static HashSet<String> ClientAddreses = new HashSet<>();
    public static HashSet<String> cacheMessages = new HashSet<>();
    public static HashSet<String> oldCacheMessages = new HashSet<>();
    public static int maxCacheSize = 1000;

    public static void addMessageToCache(TCPMessage message){
        String messageHash = message.getMessageHash();
        if(cacheMessages.size()>=1000){
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
}
