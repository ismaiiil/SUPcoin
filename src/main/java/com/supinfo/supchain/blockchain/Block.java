package com.supinfo.supchain.blockchain;

import com.supinfo.shared.Utils.StringUtil;
import com.supinfo.shared.transaction.Transaction;
import com.supinfo.supchain.blockchain.transaction.TransactionOperations;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;


public class Block implements Serializable {

    public String hash;
    public String previousHash;
    public String merkleRoot;
    public ArrayList<Transaction> transactions = new ArrayList<Transaction>(); //our data will be a simple message.
    public long timeStamp; //as number of milliseconds since 1/1/1970.
    public int nonce;

    //Block Constructor.
    public Block(String previousHash ) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();

        this.hash = calculateHash(); //Making sure we do this after we set the other values.
    }

    //Calculate new hash based on blocks contents
    public String calculateHash() {
        String calculatedhash = StringUtil.applySha256(
                previousHash +
                        Long.toString(timeStamp) +
                        Integer.toString(nonce) +
                        merkleRoot
        );
        return calculatedhash;
    }

    //Increases nonce value until hash target is reached.
    public void mineBlock(int difficulty) {
        //add reward transaction here before mining
        merkleRoot = CoreStringUtil.getMerkleRoot(transactions);
        String target = CoreStringUtil.getDificultyString(difficulty); //Create a string with difficulty * "0"
        while(!hash.substring( 0, difficulty).equals(target)) {
            nonce ++;
            hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + hash);
        //TODO uptate UTXOS here after the block has been mined, ie all txns are valid
    }

    //Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if(transaction == null) return false;
        if((!"0".equals(previousHash))) {
            if((!TransactionOperations.verifyTransaction(transaction))) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }

        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }

}

