package com.supinfo.shared.transaction;

import java.io.Serializable;

public class TransactionInput implements Serializable {
    public static final long serialVersionUID = 222222222L;
    public String transactionOutputId; //Reference to TransactionOutputs -> transactionId
    public TransactionOutput UTXO; //Contains the Unspent transaction output

    public TransactionInput(String transactionOutputId, TransactionOutput UTXO) {
        this.transactionOutputId = transactionOutputId;
        this.UTXO = UTXO;
    }

    @Override
    public String toString() {
        return "{transactionOutputId:" + java.util.Objects.toString(transactionOutputId, "null") + "\n" +
                "TransactionOutput:" + java.util.Objects.toString(UTXO, "null") + "\n}";
    }
}
