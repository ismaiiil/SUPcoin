package com.supinfo.supchain.blockchain;

import com.supinfo.shared.Utils.StringUtil;
import com.supinfo.shared.transaction.Transaction;
import com.supinfo.shared.transaction.TransactionOutput;
import com.supinfo.supchain.blockchain.transaction.TransactionOperations;
import com.supinfo.supchain.helpers.RUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static com.supinfo.supchain.blockchain.BlockchainManager.miner;


public class Block implements Serializable {

    public String hash;
    public String previousHash;
    public String merkleRoot;
    public ArrayList<Transaction> transactions = new ArrayList<Transaction>(); //our data will be a simple message.
    public long timeStamp; //as number of milliseconds since 1/1/1970.
    public int nonce;

    //Block Constructor.
    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();

        this.hash = calculateHash(); //Making sure we do this after we set the other values.
    }

    //Calculate new hash based on blocks contents
    public String calculateHash() {
        String calculatedhash = StringUtil.applySha256(
                previousHash +
                        timeStamp +
                        nonce +
                        merkleRoot
        );
        return calculatedhash;
    }

    //Increases nonce value until hash target is reached.
    public boolean mineBlock(int difficulty) {
        //add reward transaction here before mining
        BigDecimal fees = new BigDecimal(0);
        for (Transaction txn : transactions) {
            if(!txn.transactionId.equals("0")){
                fees = fees.add(txn.getInputsValue().subtract(txn.getOutputsValue()));
            }
        }
        HashMap<PublicKey, BigDecimal> _rewardRecipient = new HashMap<>();
        _rewardRecipient.put(RUtils.wallet.getPublicKey(), RUtils.rewardTransactionValue.add(fees));
        Transaction rewardTransaction = new Transaction(RUtils.wallet.getPublicKey(), _rewardRecipient, null);
        TransactionOutput _rewardTransactionOutput = new TransactionOutput(RUtils.wallet.getPublicKey(), RUtils.rewardTransactionValue.add(fees), null, null);
        _rewardTransactionOutput.id = TransactionOperations.generateTransactionOutputThisId(_rewardTransactionOutput);
        rewardTransaction.outputs.add(_rewardTransactionOutput);
        rewardTransaction.signature = TransactionOperations.generateSignature(RUtils.wallet.getPrivateKey(), rewardTransaction);
        rewardTransaction.transactionId = TransactionOperations.calulateHashTransaction(rewardTransaction);
        transactions.add(rewardTransaction);

        //start mining here
        merkleRoot = CoreStringUtil.getMerkleRoot(transactions);
        String target = CoreStringUtil.getDificultyString(difficulty); //Create a string with difficulty * "0"
        while (!hash.substring(0, difficulty).equals(target)) {
            while (miner.isPaused) {
            }
            //wait and do nothing while mining is paused!!!
            if (miner.isAborted) return false;
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + hash);
        return true;
        //DONE uptate UTXOS here after the block has been mined, ie all txns are valid


    }

    //Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if (transaction == null) return false;
        if ((!"0".equals(previousHash))) {
            System.out.println(transaction.toString());
            if (!(TransactionOperations.verifyTransaction(transaction))) {

                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }else{
            System.out.println("Previous hash of block is zero");
        }

        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }

    @Override
    public String toString() {
        return "{hash:" + hash + "\n" +
                "previousHash:" + previousHash + "\n" +
                "merkleRoot:" + merkleRoot + "\n" +
                "transactions:" + Arrays.toString(transactions.toArray()) + "\n" +
                "timeStamp:" + timeStamp + "\n" +
                "nonce:" + nonce + "\n}";

    }
}

