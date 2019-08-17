package com.supinfo.supchain.blockchain;

import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.shared.transaction.Transaction;
import com.supinfo.supchain.blockchain.transaction.TransactionOperations;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.networking.Utils.TCPUtils;

import java.util.ArrayList;

import static com.supinfo.shared.Network.TCPMessageType.PROPAGATE_NEW_BLOCK;


public class Miner extends Thread implements MinerCallbacks {

    private BlockchainManager blockchainManager = BlockchainManagerFactory.getInstance();
    //specify minimum number of txn in mempool till we start mining
    //get the transactions and remove them from the mempool
    //validate the transactions and start mining
    /*
    handle what happens to mining if we receive a new block, do we:
    ONE: PAUSE mining(handle discarding the mining txn properly)
    TWO: continue mining(if we continue mining we may face the problem that we are unable to handle the new block
    that has been added since it would conflict with that new block we just added)
    so instead what we do , we do ONE
    we validate the block and if the block is valid we discard the block we are mining(we gotta make sure to remove t
    the txns that we were mining so taht we dnt process duplicate txns... anyways even if they get discarded
    our system is never going to double spend, unless the 51% attack takes place), else we are going to
    make a new txn

    will we have callbacks???
    yea we need callbacks to pause mining and verify the newblock that we just received
     */
    public volatile boolean isAborted = false;
    public volatile boolean isMining = false;
    public volatile boolean isPaused = false;
    private volatile ArrayList<Transaction> transactionsToMine = new ArrayList<>();

    @Override
    public void run() {
        while (true){
            while (!isMining) { }
            while (isMining){
                if (!transactionsToMine.isEmpty()) {
                    Block latestBlock = blockchainManager.blockchain.get(blockchainManager.blockchain.size() - 1);
                    Block newBlock = new Block(latestBlock.hash);
                    ArrayList<Transaction> _temptxnsToMine = new ArrayList<>();
                    for (Transaction txn : transactionsToMine) {
                        if(newBlock.addTransaction(txn)){
                            _temptxnsToMine.add(txn);
                        }else{
                            blockchainManager.mempool.remove(txn);
                        }
                    }
                    if(_temptxnsToMine.equals(transactionsToMine)){
                        //MINE
                        if(newBlock.mineBlock(RUtils.difficulty)){
                            blockchainManager.blockchain.add(newBlock);
                            //UPDATE UTXOS and mempool
                            blockchainManager.mempool.removeAll(transactionsToMine);
                            blockchainManager.validateBlockchain(blockchainManager.blockchain);
                            //TCPMessage to propagate that new block to other nodes
                            TCPMessage propBlock = new TCPMessage<>(PROPAGATE_NEW_BLOCK,newBlock);
                            TCPUtils.multicastAll(propBlock,RUtils.externalIP);
                        }

                    }
                    isMining = false;
                    transactionsToMine = new ArrayList<>();
                }
            }
        }


        //miner mines block
        //and informs the blockchain manager
    }

    @Override
    public void startMiningTransactions(ArrayList<Transaction> transactions) {
        transactionsToMine = transactions;
        isMining = true;
        isPaused = false;
        isAborted = false;

    }

    @Override
    public void pauseMining() {
        isPaused = true;
    }

    @Override
    public void abortMining() {
        isAborted = true;
    }

    @Override
    public void resumeMining() {
        isPaused = false;
    }
}
