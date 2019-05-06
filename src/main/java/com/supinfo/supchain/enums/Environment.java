package com.supinfo.supchain.enums;

/**
 * Corresponding environment in which the app will run
 */
public enum Environment {
    /**
     * Debug mode has a list of behaviours such as:
     *  - Public external IP is set as the local IP for testing locally on a subnet
     *  - EDGES nodes are disabled
     */
    DEBUG,
    /**
     * Red color
     */
    PRODUCTION
}


