package helpers;

import enums.Role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class R {
    //using hashsets will allow for a margin of error when trying to add duplicate entries
    //since we heavily want unique values in those Sets, also Hashsets have a .contains search speed of O(1)
    //compared to O(n) for normal ArrayLists
    public static Role myRole;
    public static HashSet<String> ClientAddreses = new HashSet<>();
    public static HashSet<String> cacheMessage = new HashSet<>();
}
