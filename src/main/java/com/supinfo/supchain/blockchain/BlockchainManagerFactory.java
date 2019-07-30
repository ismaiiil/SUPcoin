package com.supinfo.supchain.blockchain;

public class BlockchainManagerFactory {
    /**
     * private constructor to prevent others from instantiating this class
     */
    private BlockchainManagerFactory() {
    }

    /**
     * Create an instance of the class at the time of class loading
     */
    private static final BlockchainManager instance = new BlockchainManager();

    /**
     * Provide a global point of access to the instance
     */
    public static BlockchainManager getInstance() {
        return instance;
    }
}
