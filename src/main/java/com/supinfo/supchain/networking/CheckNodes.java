package com.supinfo.supchain.networking;

public class CheckNodes extends Thread {

    public CheckNodes(){

    }

    @Override
    public void run() {
        //create a hashset for each addresses in the external addresses(store it in RUtils as addressesWaitingForPong)
        //send a PING to all these addresses (multicastRDVs)
        //start a timer
        //whenever a client receives a PING he sends back a PONG
        //when sending a PONG, we need to make sure the PING came from a known address else we dont send back a PONG
        //if a client receives a PONG he removes the IP in addressesWaitingForPong
        //stop timer after time is over, if addressesWaitingForPong isnt empty, remove all addresses still present from
        //the list of RDVs stored externalIPaddresses
        //each time we remove an IP we check if the minimum requirements are met, if they are not we contact the bootnode
        //and send a Connection request
    }
}
