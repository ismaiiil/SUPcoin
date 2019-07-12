package com.supinfo.shared.transaction;

import java.io.Serializable;

public class TransactionInput implements Serializable {
	public static final long serialVersionUID = 222222222L;
	public String transactionOutputId; //Reference to TransactionOutputs -> transactionId
	public TransactionOutput UTXO; //Contains the Unspent transaction output
}
