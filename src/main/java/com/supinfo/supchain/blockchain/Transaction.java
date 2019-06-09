package com.supinfo.supchain.blockchain;

import com.supinfo.supchain.helpers.RUtils;

import java.security.*;
import java.util.ArrayList;
import static com.supinfo.shared.Utils.StringUtil.applySha256;

public class Transaction {

    public String transactionId; //Contains a hash of transaction*
    public PublicKey sender; //Senders address/public key.
    public PublicKey recipient; //Recipients address/public key.
    public float value; //Contains the amount we wish to send to the recipient.
    public byte[] signature; //This is to prevent anybody else from spending funds in our wallet.

    public ArrayList<Transaction> inputs = new ArrayList<Transaction>();
    public ArrayList<Transaction> outputs = new ArrayList<Transaction>();

    private static int sequence = 0; //A rough count of how many transactions have been generated

    // Constructor:
    public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<Transaction> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    public boolean processTransaction() {

        if(verifySignature() == false) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

//        //Gathers transaction inputs (Making sure they are unspent):
//        for(Transaction i : inputs) {
//            i.UTXO = NoobChain.UTXOs.get(i.transactionOutputId);
//        }

        //Checks if transaction is valid:
        if(getInputsValue() < RUtils.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            System.out.println("Please enter the amount greater than " + RUtils.minimumTransaction);
            return false;
        }

        //Generate transaction outputs:
        float change = getInputsValue() - value; //get value of inputs then the left over change:
        transactionId = calulateHash();
        outputs.add(new Transaction( this.sender,this.recipient, value,inputs)); //send value to recipient
        outputs.add(new Transaction( this.sender, this.sender,   change,inputs)); //send the left over 'change' back to sender


        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for(Transaction i : inputs) {
            if(i.outputs == null) continue; //if Transaction can't be found skip it, This behavior may not be optimal.
            total += i.value;
        }
        return total;
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = CoreStringUtil.getStringFromKey(sender) + CoreStringUtil.getStringFromKey(recipient) + Float.toString(value)	;
        signature = CoreStringUtil.applyECDSASig(privateKey,data);
    }

    public boolean verifySignature() {
        String data = CoreStringUtil.getStringFromKey(sender) + CoreStringUtil.getStringFromKey(recipient) + Float.toString(value)	;
        return CoreStringUtil.verifyECDSASig(sender, data, signature);
    }

    public float getOutputsValue() {
        float total = 0;
        for(Transaction o : outputs) {
            total += o.value;
        }
        return total;
    }

    private String calulateHash() {
        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
        return applySha256(
                CoreStringUtil.getStringFromKey(sender) +
                        CoreStringUtil.getStringFromKey(recipient) +
                        Float.toString(value) + sequence
        );
    }
}
