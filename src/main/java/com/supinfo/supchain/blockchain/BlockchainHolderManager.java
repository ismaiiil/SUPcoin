package com.supinfo.supchain.blockchain;

public class BlockchainHolderManager {
    /** private constructor to prevent others from instantiating this class */
    private BlockchainHolderManager() {}

    /** Create an instance of the class at the time of class loading */
    private static final BlockchainHolder instance = new BlockchainHolder();

    /** Provide a global point of access to the instance */
    public static BlockchainHolder getInstance() {
        return instance;
    }
}
