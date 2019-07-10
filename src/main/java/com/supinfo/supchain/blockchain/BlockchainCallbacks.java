package com.supinfo.supchain.blockchain;

import com.supinfo.shared.Network.TCPMessageType;

public interface BlockchainCallbacks {
    void initHasDownloaded(Boolean success, String ip);
}
