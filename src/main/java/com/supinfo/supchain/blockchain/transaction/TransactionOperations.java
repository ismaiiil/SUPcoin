package com.supinfo.supchain.blockchain.transaction;

import com.supinfo.shared.Utils.StringUtil;
import com.supinfo.shared.transaction.Transaction;
import com.supinfo.shared.transaction.TransactionInput;
import com.supinfo.shared.transaction.TransactionOutput;
import com.supinfo.supchain.blockchain.BlockchainHolder;
import com.supinfo.supchain.blockchain.CoreStringUtil;
import static com.supinfo.supchain.Main.blockchainHolder;

import java.security.PrivateKey;
import java.util.ArrayList;

public class TransactionOperations {

    public static boolean verifySignature(Transaction transaction) {
        String data = CoreStringUtil.getStringFromKey(transaction.getSender())
                + CoreStringUtil.getStringFromKey(transaction.getRecipient())
                + Float.toString(transaction.getValue())	;
        return CoreStringUtil.verifyECDSASig(transaction.getSender(), data, transaction.getSignature());
    }

    public static Boolean verifyTransaction(Transaction transaction){
        ArrayList<TransactionInput> inputs = transaction.getInputs();
        ArrayList<TransactionOutput> outputs = transaction.getOutputs();

        if(!verifySignature(transaction)){
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }


        //Gathers transaction inputs (Making sure they are unspent):
        for(TransactionInput i : inputs) {
            if(!blockchainHolder.UTXOs.containsKey(i.getTransactionOutputId())){
                System.out.println("Transaction not found in UTXOs");
                return false;
            }
        }

        //Checks if transaction is valid:
        if(transaction.getInputsValue() < blockchainHolder.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + transaction.getInputsValue());
            System.out.println("Please enter the amount greater than " + blockchainHolder.minimumTransaction);
            return false;
        }
        return true;
    }

    public static byte[] generateSignature(PrivateKey privateKey,Transaction transaction) {
        String data = CoreStringUtil.getStringFromKey(transaction.getSender())
                + CoreStringUtil.getStringFromKey(transaction.getRecipient())
                + Float.toString(transaction.getValue())	;
        return CoreStringUtil.applyECDSASig(privateKey,data);
    }

    public static ArrayList<TransactionOutput> generateTransactionOutputs(){
        return new ArrayList<TransactionOutput>();
    }

    public static String generateTransactionOutputThisId(TransactionOutput transactionOutput){
        return StringUtil.applySha256(
                CoreStringUtil.getStringFromKey(transactionOutput.getReciepient())
                        +Float.toString(transactionOutput.getValue())
                        +transactionOutput.getParentTransactionId());
    }
}
