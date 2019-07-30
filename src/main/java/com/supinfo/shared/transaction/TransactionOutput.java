package com.supinfo.shared.transaction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {
    public static final long serialVersionUID = 333333333L;
    public String id;
    public PublicKey reciepient; //also known as the new owner of these coins.
    public BigDecimal value; //the amount of coins they own
    public String parentTransactionId; //the id of the transaction this output was created in

    //Constructor
    public TransactionOutput(PublicKey reciepient, BigDecimal value, String parentTransactionId, String thisid) {
        this.reciepient = reciepient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
//		this.id = StringUtil.applySha256(StringUtil.getStringFromKey(reciepient)+BigDecimal.toString(value)+parentTransactionId);
        //TODO:set this when we are generating txn outputs for a txn
        this.id = thisid;
    }

    //Check if coin belongs to you
    public boolean isMine(PublicKey publicKey) {
        return (publicKey == reciepient);
    }

    @Override
    public String toString() {
        return "{TxnId:" + java.util.Objects.toString(id, "null") + "\n" +
                "reciepient:" + java.util.Objects.toString(reciepient, "null") + "\n" +
                "value:" + java.util.Objects.toString(value, "null") + "\n" +
                "parentTransactionId:" + java.util.Objects.toString(parentTransactionId, "null") + "\n}";
    }
}
