package com.supinfo.shared.Network;

public enum TCPMessageType {
    //RDV related Messages
    VERIFY,
    REQUEST_CONNECTION,
    CONFIRM_CONNECTION,
    MESSENGER_REQ,
    MESSENGER_ACK,
    WAIT_FOR_LOOKUP,
    PING,
    PONG,
    UPDATE_SENDER_IP,
    //Wallet related Messages
    WALLET_CONNECT,
    WALLET_PING,
    WALLET_LIST_NODES
}
