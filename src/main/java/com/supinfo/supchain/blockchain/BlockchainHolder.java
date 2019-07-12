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

import java.security.PublicKey;
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
    public HashMap<String,TransactionOutput> UTXOs = new HashMap<>();
    public TCPMessageType status;
    public HashSet<String> initTempIPS = new HashSet<>();
    public HashSet<Transaction> mempool = new HashSet<>();
    private CLogger cLogger = new CLogger(this.getClass());
    private HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>();
    private HashMap<String,TransactionOutput> lockedUTXOs = new HashMap<>();

    //before addinga txn to the mempool we first have to validate it prior to the orginal UTXO
    //have a temp UTXO that will store utxos for unconfirmed validated txns, ie txns that have been validated prior to the actual UTXO list

    public Boolean validateBlockchain(ArrayList<Block> blockchain){
        Block currentBlock;
        Block previousBlock;

        cLogger.println("Please wait while the blockchain is validated...");
        if(blockchain.size() < 1){
            return false;
        }
        Transaction genesis = blockchain.get(0).transactions.get(0);
        //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesis.outputs.get(0).id, genesis.outputs.get(0));

        //loop through blockchainHolder to check hashes:
        for(int i=1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            if(!validateBlock(previousBlock,currentBlock)){
                tempUTXOs.clear();
                cLogger.println("The blockchain is invalid!");
                return false;
            }
        }
        UTXOs = (HashMap<String, TransactionOutput>) tempUTXOs.clone();
        tempUTXOs.clear();
        cLogger.println("The blockchain is valid!");
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
        float transactionFee = 0;
        int rewardTransactionCount = 0;
        Transaction rewardTransaction= null;
        for(int t=0; t < newBlock.transactions.size(); t++) {

            Transaction currentTransaction = newBlock.transactions.get(t);
            //transaction fee shouldnt exceed the total in minus total out, we do this verification after looping though all transactions
            transactionFee += currentTransaction.getInputsValue() - currentTransaction.getOutputsValue();
            //verify of blocks contains only one reward transaction
            if(rewardTransactionCount>1){
                System.out.println("Found more than one null Input transaction");
                return false;
            }

            if(!verifySignature(currentTransaction)) {
                System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                return false;
            }

            for(TransactionOutput output: currentTransaction.outputs) {
                tempUTXOs.put(output.id, output);
            }

            if(currentTransaction.inputs == null){
                //this is the reward transaction
                //verify that the reward transaction is correct
                rewardTransaction = currentTransaction;
                rewardTransactionCount++;
                continue;
            }



            if(currentTransaction.getInputsValue() >= currentTransaction.getOutputsValue()) {
                System.out.println("#Inputs are greater or equal to outputs on Transaction(" + t + ")");
                return false;
            }

            for(TransactionInput input: currentTransaction.inputs) {
                //verify all transaction inputs here
                tempOutput = tempUTXOs.get(input.transactionOutputId);

                if(tempOutput == null) {
                    System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                    return false;
                }

                if(input.UTXO.value != tempOutput.value) {
                    System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                    return false;
                }

                tempUTXOs.remove(input.transactionOutputId);
            }



            ArrayList<TransactionOutput> _outputs = currentTransaction.outputs;
            HashMap<PublicKey,Float> _recipientsFromOutputs = new HashMap<>();
            for (TransactionOutput _to:_outputs) {
                _recipientsFromOutputs.put(_to.reciepient,_to.value);
            }

            HashMap<PublicKey,Float> _recipients = currentTransaction.recipients;

            if(!_recipients.equals(_recipientsFromOutputs)){
                System.out.println("#Outputs and recipients do not match");
            }


        }
        //after looping through all txn we have the total txn fee and can check if the miner dint exceed its value
        if(rewardTransaction != null){
            System.out.println("WARNING! There is no reward Transaction on this block");
            if(rewardTransaction.getOutputsValue() > rewardTransactionValue+transactionFee){
                System.out.println("ERROR! This reward transaction is trying to claim more coins that its allowed to!");
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
        Wallet origin = new Wallet();
        Wallet coinbase = RUtils.wallet;
        HashMap<PublicKey,Float> _recipients = new HashMap<>();
        _recipients.put(coinbase.getPublicKey(),1000000f);
        Transaction genesisTransaction = new Transaction(origin.getPublicKey(),_recipients,null);
        genesisTransaction.signature = (generateSignature(origin.getPrivateKey(),genesisTransaction));//manually sign the genesis transaction
        genesisTransaction.transactionId = ("0"); //manually set the transaction id
        ArrayList<TransactionOutput> genesisOutputs= new ArrayList<>();
        TransactionOutput tout = new TransactionOutput(coinbase.getPublicKey(),
                1000000f,
                genesisTransaction.transactionId,"");
        tout.id = (generateTransactionOutputThisId(tout));
        genesisOutputs.add(tout);
        genesisTransaction.outputs = (genesisOutputs);
        UTXOs.put(genesisTransaction.outputs.get(0).id,genesisTransaction.outputs.get(0));
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

    public HashSet<TransactionOutput> getAllUTXOForPublicKey(PublicKey publicKey){
        HashSet<TransactionOutput> _all = new HashSet<>();
        for (TransactionOutput tout:UTXOs.values()) {
            if(tout.reciepient == publicKey){
                _all.add(tout);
            }
        }
        return _all;
    }

    public float getMinerRawBalance(){
        float _minerBalance = 0;
        for (TransactionOutput output:UTXOs.values()) {
            if(output.isMine(RUtils.wallet.getPublicKey())){
                _minerBalance += output.value;
            }
        }
        return _minerBalance;
    }

    public void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
