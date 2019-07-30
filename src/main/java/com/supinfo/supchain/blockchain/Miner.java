package com.supinfo.supchain.blockchain;

public class Miner {
    //specify minimum number of txn in mempool till we start mining
    //get the transactions and remove them from the mempool
    //validate the transactions and start mining
    /*
    handle what happens to mining if we receive a new block, do we:
    ONE: PAUSE mining(handle discarding the mining txn properly)
    TWO: continue mining(if we continue mining we may face the problem that we are unable to handle the new block
    that has been added since it would conflict with that new block we just added)
    so instead what we do , we do ONE
    we validate the block and if the block is valid we discard the block we are mining(we gotta make sure to remove t
    the txns that we were mining so taht we dnt process duplicate txns... anyways even if they get discarded
    our system is nver going to double spend, unless the 51% attack takes place), else we are going to
    make a new txn

will we have callbacks???
     */
}
