package com.supinfo.supchain.blockchain;

import com.supinfo.shared.transaction.Transaction;

import java.util.ArrayList;

public interface MinerCallbacks {
    void startMiningTransactions(ArrayList<Transaction> transactions);
    void pauseMining();
    void abortMining();
    void resumeMining();
}
