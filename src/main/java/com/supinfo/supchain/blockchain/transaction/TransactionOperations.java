package com.supinfo.supchain.blockchain.transaction;

import com.supinfo.shared.Utils.StringUtil;
import com.supinfo.shared.transaction.Transaction;
import com.supinfo.shared.transaction.TransactionInput;
import com.supinfo.shared.transaction.TransactionOutput;
import com.supinfo.supchain.blockchain.CoreStringUtil;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

import static com.supinfo.supchain.Main.blockchainHolder;

public class TransactionOperations {

    public static boolean verifySignature(Transaction transaction) {
        String data = CoreStringUtil.getStringFromKey(transaction.sender)
                + getMapAsString(transaction);
        return CoreStringUtil.verifyECDSASig(transaction.sender, data, transaction.signature);
    }

    public static Boolean verifyTransaction(Transaction transaction) {
        ArrayList<TransactionInput> inputs = transaction.inputs;
        ArrayList<TransactionOutput> outputs = transaction.outputs;

        if (!verifySignature(transaction)) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }


        //Gathers transaction inputs (Making sure they are unspent):
        for (TransactionInput i : inputs) {
            if (!blockchainHolder.UTXOs.containsKey(i.transactionOutputId)) {
                System.out.println("Transaction not found in UTXOs");
                return false;
            }
        }

        //Checks if transaction is valid:
        if (transaction.getInputsValue().compareTo(blockchainHolder.minimumTransaction) < 0) {
            System.out.println("Transaction Inputs too small: " + transaction.getInputsValue());
            System.out.println("Please enter the amount greater than " + blockchainHolder.minimumTransaction);
            return false;
        }
        return true;
    }

    public static byte[] generateSignature(PrivateKey privateKey, Transaction transaction) {
        String data = CoreStringUtil.getStringFromKey(transaction.sender)
                + getMapAsString(transaction);
        return CoreStringUtil.applyECDSASig(privateKey, data);
    }

    private static String getMapAsString(Transaction transaction) {
        HashMap<PublicKey, BigDecimal> _recipients = transaction.recipients;
        StringBuilder output = new StringBuilder();
        for (HashMap.Entry<PublicKey, BigDecimal> entry : _recipients.entrySet()) {
            String _t = CoreStringUtil.getStringFromKey(entry.getKey()) + entry.getValue().toString();
            output.append(_t);
        }
        return output.toString();
    }

    public static ArrayList<TransactionOutput> generateTransactionOutputs() {
        return new ArrayList<TransactionOutput>();
    }

    public static String generateTransactionOutputThisId(TransactionOutput transactionOutput) {
        return StringUtil.applySha256(
                CoreStringUtil.getStringFromKey(transactionOutput.reciepient)
                        + transactionOutput.value.toString()
                        + transactionOutput.parentTransactionId);
    }

    public static String calulateHashTransaction(Transaction transaction) {

        StringBuilder recipients = new StringBuilder();
        for (PublicKey key : transaction.recipients.keySet()) {
            recipients.append(CoreStringUtil.getStringFromKey(key));
        }
        return StringUtil.applySha256(
                CoreStringUtil.getStringFromKey(transaction.sender)
                        + recipients.toString() +
                        transaction.recipients.values().toString()
        );
    }
}
