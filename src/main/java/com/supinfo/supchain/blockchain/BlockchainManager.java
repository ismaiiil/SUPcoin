package com.supinfo.supchain.blockchain;

import com.supinfo.shared.Network.TCPMessage;
import com.supinfo.shared.Network.TCPMessageType;
import com.supinfo.shared.transaction.Transaction;
import com.supinfo.shared.transaction.TransactionInput;
import com.supinfo.shared.transaction.TransactionOutput;
import com.supinfo.supchain.blockchain.transaction.TransactionOperations;
import com.supinfo.supchain.blockchain.wallet.Wallet;
import com.supinfo.supchain.helpers.CLogger;
import com.supinfo.supchain.helpers.RUtils;
import com.supinfo.supchain.networking.Utils.TCPUtils;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.supinfo.supchain.blockchain.transaction.TransactionOperations.*;
import static java.lang.Thread.sleep;

public class BlockchainManager implements BlockchainCallbacks {
    public ArrayList<Block> blockchain = new ArrayList<>();
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<>();
    public TCPMessageType status;
    public HashSet<String> initTempIPS = new HashSet<>();
    public ArrayList<Transaction> mempool = new ArrayList<>();
    private CLogger cLogger = new CLogger(this.getClass());
    //tempUTXOS is used to validate the blockchain by walking through the blockchain, adding and removing UTXOS while we go though the chain
    private HashMap<String, TransactionOutput> tempUTXOs = new HashMap<>();
    //this will be used just to increase performance when buying coins: optional!
    public HashMap<String, TransactionOutput> soldUTXOs = new HashMap<>();
    public static Miner miner = new Miner();
    //have a thread that will periodically check the mempool, if it >= the min number txn specified in Rutils we can notify
    //the miner that it can start mining those transactions


    public Boolean validateBlockchain(ArrayList<Block> blockchain) {
        Block currentBlock;
        Block previousBlock;
        tempUTXOs.clear();
        cLogger.println("Please wait while the blockchain is validated...");
        if (blockchain.size() < 1) {
            return false;
        }
        Transaction genesis = blockchain.get(0).transactions.get(0);
        tempUTXOs.put(genesis.outputs.get(0).id, genesis.outputs.get(0));

        //loop through blockchainHolder to check hashes:
        for (int i = 1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);
            if (!validateBlock(previousBlock, currentBlock, false)) {
                tempUTXOs.clear();
                cLogger.println("The blockchain is invalid!");
                return false;
            }
        }
        UTXOs = (HashMap<String, TransactionOutput>) tempUTXOs.clone();
        tempUTXOs.clear();
        return true;
    }

    public Boolean validateBlock(Block previousBlock, Block newBlock, boolean isBroadcast) {

        //we use this in case we want to verify a new block that has been broadcast to us
        if (isBroadcast) {
            tempUTXOs = (HashMap<String, TransactionOutput>) UTXOs.clone();
        }

        String hashTarget = new String(new char[RUtils.difficulty]).replace('\0', '0');

        //see if block is empty, as per protocol empty blocks aren't allowed
        if (newBlock.transactions.size() == 0) {
            System.out.println("Transactions cannot be empty in block");
            return false;
        }

        //compare registered hash and calculated hash:
        if (!newBlock.hash.equals(newBlock.calculateHash())) {
            System.out.println("#Current Hashes not equal");
            return false;
        }
        //compare previous hash and registered previous hash
        if (!previousBlock.hash.equals(newBlock.previousHash)) {
            System.out.println("#Previous Hashes not equal");
            return false;
        }
        //check if hash is solved
        if (!newBlock.hash.substring(0, RUtils.difficulty).equals(hashTarget)) {
            System.out.println("#This block hasn't been mined");
            return false;
        }

        TransactionOutput tempOutput;
        //loop thru block transactions:
        BigDecimal transactionFee = new BigDecimal(0);
        int rewardTransactionCount = 0;
        Transaction rewardTransaction = null;
        for (int t = 0; t < newBlock.transactions.size(); t++) {

            Transaction currentTransaction = newBlock.transactions.get(t);
            //transaction fee shouldn't exceed the total in minus total out, we do this verification after looping though all transactions
            transactionFee = transactionFee.add(currentTransaction.getInputsValue().subtract(currentTransaction.getOutputsValue()));
            //verify of blocks contains only one reward transaction
            if (rewardTransactionCount > 1) {
                System.out.println("Found more than one null Input transaction");
                return false;
            }

            if (!verifySignature(currentTransaction)) {
                System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                return false;
            }

            for (TransactionOutput output : currentTransaction.outputs) {
                tempUTXOs.put(output.id, output);
            }

            if (currentTransaction.inputs == null) {
                //this is the reward transaction
                //verify that the reward transaction is correct
                rewardTransaction = currentTransaction;
                rewardTransactionCount++;
                continue;
            }


            if (currentTransaction.getInputsValue().compareTo(currentTransaction.getOutputsValue()) > 0) {
                System.out.println("#Inputs are greater than outputs on Transaction(" + t + ")");
                return false;
            }

            for (TransactionInput input : currentTransaction.inputs) {
                //verify all transaction inputs here
                tempOutput = tempUTXOs.get(input.transactionOutputId);

                if (tempOutput == null) {
                    System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                    return false;
                }

                if (!input.UTXO.value.equals(tempOutput.value)) {
                    System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                    return false;
                }

                tempUTXOs.remove(input.transactionOutputId);
            }


            ArrayList<TransactionOutput> _outputs = currentTransaction.outputs;
            HashMap<PublicKey, BigDecimal> _recipientsFromOutputs = new HashMap<>();
            for (TransactionOutput _to : _outputs) {
                _recipientsFromOutputs.put(_to.reciepient, _to.value);
            }

            HashMap<PublicKey, BigDecimal> _recipients = currentTransaction.recipients;

            if (!_recipients.equals(_recipientsFromOutputs)) {
                System.out.println("#Outputs and recipients do not match");
                return false;
            }


        }
        //after looping through all txn we have the total txn fee and can check if the miner dint exceed its value
        if (rewardTransaction != null) {
            if ((rewardTransaction.getOutputsValue().compareTo(RUtils.rewardTransactionValue.add(transactionFee)) <= 0)) {
                System.out.println("ERROR! This reward transaction is trying to claim more coins that its allowed to!");
                return false;
            }
        } else {
            System.out.println("WARNING! There is no reward Transaction on this block");
        }

        //clear the tempUTXOs after we are done checking the new block
        if (isBroadcast) {
            UTXOs = (HashMap<String, TransactionOutput>) tempUTXOs.clone();
            tempUTXOs.clear();
        }
        return true;
    }

    public boolean requestBlockchainFromPeers() throws InterruptedException {
        if (blockchain.size() == 0) {
            status = TCPMessageType.INIT_REQUEST_DOWNLOAD;
            TCPMessage requestBlockchain = new TCPMessage<>(TCPMessageType.INIT_REQUEST_DOWNLOAD, "");
            for (String ip : RUtils.allClientAddresses()) {
                if (!initTempIPS.contains(ip)) {
                    TCPUtils.unicast(requestBlockchain, ip);
                    sleep(RUtils.connectionLatency);
                    if (status == TCPMessageType.INIT_DOWNLOAD_FULL_BLOCKCHAIN) {
                        initTempIPS.add(ip);
                        return true;
                    }
                    initTempIPS.add(ip);
                }
            }
            if (status == TCPMessageType.INIT_REQUEST_DOWNLOAD) {
                cLogger.println("No blockchain could be downloaded from any peer");
                return false;
            }
        }else{
            return true;
        }
        return true;
    }

    public void initGenesisBlockchain() {
        //Wallet origin = new Wallet();
        Wallet origin = new Wallet();
        Wallet coinbase = RUtils.wallet;
        HashMap<PublicKey, BigDecimal> _recipients = new HashMap<>();
        _recipients.put(coinbase.getPublicKey(), new BigDecimal(1000000));
        Transaction genesisTransaction = new Transaction(origin.getPublicKey(), _recipients, null);
        genesisTransaction.signature = (generateSignature(origin.getPrivateKey(), genesisTransaction));//manually sign the genesis transaction
        genesisTransaction.transactionId = ("0"); //manually set the transaction id
        ArrayList<TransactionOutput> genesisOutputs = new ArrayList<>();
        TransactionOutput tout = new TransactionOutput(coinbase.getPublicKey(),
                new BigDecimal(1000000f),
                genesisTransaction.transactionId, "");
        tout.id = (generateTransactionOutputThisId(tout));
        genesisOutputs.add(tout);
        genesisTransaction.outputs = (genesisOutputs);
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
    }

    @Override
    public void initHasDownloaded(Boolean success, String ip) {
        if (success) {
            //sync mempool with the node we succeeded to get the blockchain
            TCPMessage reqMempool = new TCPMessage<>(TCPMessageType.REQUEST_MEMPOOL, "");
            TCPUtils.unicast(reqMempool, ip);

        } else {
            if (status == TCPMessageType.INIT_DOWNLOAD_FULL_BLOCKCHAIN) {
                cLogger.println("Invalid Blockchain detected and has not been downloaded!");
                try {
                    //if we have gone through all ips we reset the temp variable to restart scanning those nodes if their blockchain is valid
                    if (initTempIPS.containsAll(RUtils.allClientAddresses())) {
                        initTempIPS.clear();
                    }
                    requestBlockchainFromPeers();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public void newTxnReceived() {
        if (mempool.size() >= RUtils.minTransactionTillMine) {
            ArrayList<Transaction> txnsToMine = new ArrayList<>(mempool.subList(0, RUtils.minTransactionTillMine));
            if (!miner.isAlive()) {
                miner.start();
            }
            if(!miner.isMining){
                miner.startMiningTransactions(txnsToMine);
            }
            //start mining the tempMempool
            //now if we receive a new block while mining we are going to check if the new block is valid
            //CASE ONE, its previous block matches our latest block and we can verify the block to safely add it
            //we then have to place the miner transactions back in the mempool and update the mempool accordingly
            //by using the newblock transactions
            //CASE TWO, it is a block that has a difrent previous block, we need to know if it comes from a longer chain
            //if it comes from a longer chain we will download the new chain and verify it, if its good we discard
            //and we use their blockchain, update evrything UTXOS,mempool, etc...
            //else if it comes from  a chain with same length we continue mining
        }
    }

    @Override
    public void newBlockReceived(Block block) {
        Block lastBlock = blockchain.get(blockchain.size()-1);
        if(block.previousHash.equals(lastBlock.previousHash)){
            miner.pauseMining();
            if(validateBlock(lastBlock,block,true)){
                miner.abortMining();
                blockchain.add(block);
                validateBlockchain(blockchain);
            }else{
                miner.resumeMining();
            }
        }else{
            //check if the node that sent the block has a longer chain! and download all changes
            TCPMessage reqchainSizeMessage = new TCPMessage<>(TCPMessageType.REQUEST_CHAIN_SIZE,null);
            TCPUtils.unicast(reqchainSizeMessage, RUtils.externalIP);
            //if we receive in the background a longer chain it will automatically stop mining and will update the blockchain if the chain is valid
        }

    }

    public ArrayList<TransactionOutput> getMinUTXOForPublicKey(PublicKey publicKey, BigDecimal coins) {
        ArrayList<TransactionOutput> _all = new ArrayList<>();
        BigDecimal _coins = new BigDecimal(0);
        for (TransactionOutput tout : UTXOs.values()) {
            if (tout.reciepient.equals(publicKey)) {
                _all.add(tout);
                _coins = _coins.add(tout.value);
                //if a value other than zero is supplied we stop returning txn once we reached the value requested
                if ((coins.compareTo(BigDecimal.ZERO) != 0) && (_coins.compareTo(coins) >= 0)) {
                    return _all;
                }
            }
        }
        return _all;
    }

    public void addTransactionToMemPool(Transaction transaction) {
        //whenever a txn is added to the mempool it gets spread to the network
        //we will have to use a TCPMESSAGE TYPE to do this, and set the propagation flag true
        mempool.add(transaction);
        TCPUtils.multicastAll(new TCPMessage<>(TCPMessageType.PROPAGATE_NEW_TXN_MEMPOOL,0, transaction), RUtils.externalIP);
    }

    public BigDecimal getMinerRawBalance() {
        BigDecimal _minerBalance = new BigDecimal(0);
        for (TransactionOutput output : UTXOs.values()) {
            if (output.isMine(RUtils.wallet.getPublicKey()) && !soldUTXOs.containsKey(output.id)) {
                _minerBalance = _minerBalance.add(output.value);
            }
        }
        return _minerBalance;
    }





    public String dumpBlockchain() {
        return "{blockchain" + blockchain.toString() + "\n}" +
                "{UTXOs" + java.util.Objects.toString(UTXOs, "null") + "\n}" +
                "{mempool" + java.util.Objects.toString(mempool, "null") + "\n}" +
                "status" + java.util.Objects.toString(status, "null") + "\n" +
                "{soldUTXOs" + java.util.Objects.toString(soldUTXOs, "null") + "\n}";
    }

    public void addBlock(Block newBlock) {
        newBlock.mineBlock(RUtils.difficulty);
        blockchain.add(newBlock);
        //validateBlockchain(blockchain);
    }


}
