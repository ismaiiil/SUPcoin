package com.supinfo.shared.transaction;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction implements Serializable {
    public static final long serialVersionUID = 1111111111L;
    //TODO we intend to make this just a POJO that carries data ofa txn every validation will be done later on the node
    //TODO simple validations can be done on the wallet to make sure we dnt lose time and send an invalid txn to the node

    private String transactionId; //Contains a hash of transaction*
    private PublicKey sender; //Senders address/public key.
    private ArrayList<Recipient> recipients ; //Recipients address/public key.
    private float value; //Contains the amount we wish to send to the recipient.
    private byte[] signature; //This is to prevent anybody else from spending funds in our wallet.

    private ArrayList<TransactionInput> inputs = new ArrayList<>();
    private ArrayList<TransactionOutput> outputs = new ArrayList<>();

    // Constructor:
    public Transaction(PublicKey from, ArrayList<Recipient> recipients, float value,  ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipients = recipients;
        this.value = value;
        this.inputs = inputs;
    }


    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public PublicKey getSender() {
        return sender;
    }

    public void setSender(PublicKey sender) {
        this.sender = sender;
    }


    public ArrayList<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(ArrayList<Recipient> recipients) {
        this.recipients = recipients;
    }
    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public ArrayList<TransactionInput> getInputs() {
        return inputs;
    }

    public void setInputs(ArrayList<TransactionInput> inputs) {
        this.inputs = inputs;
    }

    public ArrayList<TransactionOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(ArrayList<TransactionOutput> outputs) {
        this.outputs = outputs;
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
            if(i.getUTXO() == null) continue; //if Transaction can't be found skip it, This behavior may not be optimal.
            total += i.getUTXO().getValue();
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
            total += o.getValue();
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
}
