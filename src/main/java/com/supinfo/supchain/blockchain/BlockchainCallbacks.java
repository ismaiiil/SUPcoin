package com.supinfo.supchain.blockchain;


public interface BlockchainCallbacks {
    void initHasDownloaded(Boolean success, String ip);
    void newTxnReceived();
    void newBlockReceived(Block block);
}
