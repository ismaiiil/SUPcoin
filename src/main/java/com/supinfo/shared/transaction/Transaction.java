package com.supinfo.shared.transaction;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class Transaction implements Serializable {
    public static final long serialVersionUID = 1111111111L;
    //TODO we intend to make this just a POJO that carries data ofa txn every validation will be done later on the node
    //TODO simple validations can be done on the wallet to make sure we dnt lose time and send an invalid txn to the node

    public String transactionId; //Contains a hash of transaction*
    public PublicKey sender; //Senders address/public key.
    public HashMap<PublicKey,Float> recipients = new HashMap<>();
    public byte[] signature; //This is to prevent anybody else from spending funds in our wallet.

    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    // Constructor:
    public Transaction(PublicKey from,HashMap<PublicKey,Float> recipients,  ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipients = recipients;
        this.inputs = inputs;
    }

//    public boolean processTransaction() {
//
//
        //TODO this is all the nodes work
//        if(verifySignature() == false) {
//            System.out.println("#Transaction Signature failed to verify");
//            return false;
//        }

        //TODO node will check evry txn inputs to make sure they are unspent
//        //Gathers transaction inputs (Making sure they are unspent):
//        for(TransactionInput i : inputs) {
//            i.UTXO = NoobChain.UTXOs.get(i.transactionOutputId);
//        }

            //TODO this is also the nodes work to verify the txn is valid by sum dfiference
//        //Checks if transaction is valid:
//        if(getInputsValue() < NoobChain.minimumTransaction) {
//            System.out.println("Transaction Inputs too small: " + getInputsValue());
//            System.out.println("Please enter the amount greater than " + NoobChain.minimumTransaction);
//            return false;
//        }


        //TODO set the txn outputs in txn operations
//        //Generate transaction outputs:
//        float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
//        transactionId = calulateHash();
//        outputs.add(new TransactionOutput( this.reciepient, value,transactionId)); //send value to recipient
//        outputs.add(new TransactionOutput( this.sender, leftOver,transactionId)); //send the left over 'change' back to sender

//        // TODO Add outputs to Unspent list this will be done after miner has mined the block this transaction is in
//        for(TransactionOutput o : outputs) {
//            NoobChain.UTXOs.put(o.id , o);
//        }

        //TODO node will Remove transaction inputs from UTXO lists as spent, will also have to remove in wallet
//        for(TransactionInput i : inputs) {
//            if(i.UTXO == null) continue; //if Transaction can't be found skip it
//            NoobChain.UTXOs.remove(i.UTXO.id);
//        }
//
//        return true;
//    }

    public float getInputsValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //if Transaction can't be found skip it, This behavior may not be optimal, miner or reward txns have null inputs
            total += i.UTXO.value;
        }
        return total;
    }

    //move this outside
//    public void generateSignature(PrivateKey privateKey) {
//        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
//        signature = StringUtil.applyECDSASig(privateKey,data);
//    }


    //this will done on the node side
//    public boolean verifySignature() {
//        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
//        return StringUtil.verifyECDSASig(sender, data, signature);
//    }

    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }

//    private String calulateHash() {
//        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
//        return StringUtil.applySha256(
//                StringUtil.getStringFromKey(sender) +
//                        StringUtil.getStringFromKey(reciepient) +
//                        Float.toString(value) + sequence
//        );
//    }


    //to know if a transaction contains our public key below are some methods to do so
    public boolean containsPublickKey(PublicKey pkey){
        return sentByPublickKey(pkey) || hasOutputToPublicKey(pkey);
    }

    public boolean sentByPublickKey(PublicKey pkey){
        return sender == pkey;
    }

    public boolean hasOutputToPublicKey(PublicKey pkey){
        for (TransactionOutput tout : outputs) {
            if (tout.isMine(pkey)) return true;
        }
        return false;
    }
}
