package com.supinfo.supchain.blockchain;

import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.shared.transaction.Transaction;
import com.supinfo.shared.transaction.TransactionInput;
import com.supinfo.shared.transaction.TransactionOutput;
import com.supinfo.supchain.blockchain.wallet.Wallet;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.networking.Utils.TCPUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.supinfo.supchain.blockchain.transaction.TransactionOperations.generateSignature;
import static com.supinfo.supchain.blockchain.transaction.TransactionOperations.verifySignature;
import static com.supinfo.supchain.blockchain.transaction.TransactionOperations.generateTransactionOutputThisId;
import static java.lang.Thread.sleep;

public class BlockchainHolder implements BlockchainCallbacks{
    public int difficulty = 3;
    public float minimumTransaction = 0.1f;
    public float rewardTransactionValue = 12f;
    public ArrayList<Block> blockchain = new ArrayList<>();
    public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();
    public TCPMessageType status;
    public HashSet<String> initTempIPS = new HashSet<>();
    public HashSet<Transaction> mempool = new HashSet<>();

    private CLogger cLogger = new CLogger(this.getClass());
    private HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>();

    public Boolean validateBlockchain(ArrayList<Block> blockchain){
        Block currentBlock;
        Block previousBlock;

        cLogger.println("Please wait while the blockchain is validated...");
        if(blockchain.size() < 1){
            return false;
        }
        Transaction genesis = blockchain.get(0).transactions.get(0);
        //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesis.getOutputs().get(0).getId(), genesis.getOutputs().get(0));

        //loop through blockchainHolder to check hashes:
        for(int i=1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            if(!validateBlock(previousBlock,currentBlock)){
                tempUTXOs.clear();
                return false;
            }
        }
        UTXOs = (HashMap<String, TransactionOutput>) tempUTXOs.clone();
        tempUTXOs.clear();

        return true;
    }

    public Boolean validateBlock(Block previousBlock,Block newBlock){
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        //compare registered hash and calculated hash:


        if(!newBlock.hash.equals(newBlock.calculateHash()) ){
            System.out.println("#Current Hashes not equal");
            return false;
        }
        //compare previous hash and registered previous hash
        if(!previousBlock.hash.equals(newBlock.previousHash) ) {
            System.out.println("#Previous Hashes not equal");
            return false;
        }
        //check if hash is solved
        if(!newBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
            System.out.println("#This block hasn't been mined");
            return false;
        }

        TransactionOutput tempOutput;
        //loop thru block transactions:
        int rewardTransactionCount = 0;
        for(int t=0; t < newBlock.transactions.size(); t++) {



            Transaction currentTransaction = newBlock.transactions.get(t);

            //verify of blocks contains only one reward transaction
            if(rewardTransactionCount>1){
                System.out.println("Found more than one null Input transaction");
                return false;
            }

            if(currentTransaction.getInputs().size() == 0){
                //this is the reward transaction
                //verify that the reward transaction is correct
                if(currentTransaction.getOutputsValue() > 12){
                    return false;
                }
                rewardTransactionCount++;
                continue;
            }


            if(!verifySignature(currentTransaction)) {
                System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                return false;
            }
            if(currentTransaction.getInputsValue() >= currentTransaction.getOutputsValue()) {
                System.out.println("#Inputs are greater or equal to outputs on Transaction(" + t + ")");
                return false;
            }

            for(TransactionInput input: currentTransaction.getInputs()) {
                //verify all transaction inputs here
                tempOutput = tempUTXOs.get(input.getTransactionOutputId());

                if(tempOutput == null) {
                    System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                    return false;
                }

                if(input.getUTXO().getValue() != tempOutput.getValue()) {
                    System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                    return false;
                }

                tempUTXOs.remove(input.getTransactionOutputId());
            }

            for(TransactionOutput output: currentTransaction.getOutputs()) {
                tempUTXOs.put(output.getId(), output);
            }

            if( currentTransaction.getOutputs().get(0).getReciepient() != currentTransaction.getRecipient()) {
                System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                return false;
            }
            if( currentTransaction.getOutputs().get(1).getReciepient() != currentTransaction.getSender()) {
                System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                return false;
            }

        }
        return true;
    }

    public boolean requestBlockchainFromPeers() throws InterruptedException {
        if(blockchain.size() == 0){
            status = TCPMessageType.INIT_REQUEST_DOWNLOAD;
            TCPMessage requestBlockchain = new TCPMessage<>(TCPMessageType.INIT_REQUEST_DOWNLOAD,"");
            for (String ip : RUtils.allClientAddresses()) {
                if(!initTempIPS.contains(ip)){
                    TCPUtils.unicast(requestBlockchain, ip);
                    sleep(RUtils.connectionLatency);
                    if(status == TCPMessageType.INIT_DOWNLOAD_FULL_BLOCKCHAIN){
                        initTempIPS.add(ip);
                        return true;
                    }
                    initTempIPS.add(ip);
                }
            }
            if(status == TCPMessageType.INIT_REQUEST_DOWNLOAD){
                cLogger.println("No blockchain could be downloaded from any peer");
                return false;
            }
        }
        return false;
    }

    public void initGenesisBlockchain(){
        //Wallet origin = new Wallet();
        Wallet origin = RUtils.wallet;
        Wallet coinbase = new Wallet();
        Transaction genesisTransaction = new Transaction(origin.getPublicKey(), coinbase.getPublicKey(), 1000000f, null);
        genesisTransaction.setSignature(generateSignature(origin.getPrivateKey(),genesisTransaction));//manually sign the genesis transaction
        genesisTransaction.setTransactionId("0"); //manually set the transaction id
        ArrayList<TransactionOutput> genesisOutputs= new ArrayList<>();
        TransactionOutput tout = new TransactionOutput(genesisTransaction.getRecipient(),
                genesisTransaction.getValue(),
                genesisTransaction.getTransactionId(),"");
        tout.setId(generateTransactionOutputThisId(tout));
        genesisOutputs.add(tout);
        genesisTransaction.setOutputs(genesisOutputs);
        UTXOs.put(genesisTransaction.getOutputs().get(0).getId(),genesisTransaction.getOutputs().get(0));
        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
    }

    @Override
    public void initHasDownloaded(Boolean success, String ip) {
        if(success){
            //sync mempool with the node we succeeded to get the blockchain
            TCPMessage reqMempool = new TCPMessage<>(TCPMessageType.REQUEST_MEMPOOL,"");
            TCPUtils.unicast(reqMempool,ip);

        }else{
            if(status == TCPMessageType.INIT_DOWNLOAD_FULL_BLOCKCHAIN){
                cLogger.println("Invalid Blockchain detected and has not been downloaded!");
                try {
                    //if we have gone through all ips we reset the temp variable to restart scanning those nodes if their blockchain is valid
                    if(initTempIPS.containsAll(RUtils.allClientAddresses())){
                        initTempIPS.clear();
                    }
                    requestBlockchainFromPeers();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
