package io.nats.connector.plugins;

/**
 * Created by colinsullivan on 12/16/15.
 */
public enum NATSEvent {

    /**
     * An asynchronuous error has occurred.
     */
    ASYNC_ERROR,

    /***
     * The server has disconnected.
     */
    DISCONNECTED,

    /***
     * The connection to the NATS cluster has been closed.
     */
    CLOSED,

    /**
     * The server has reconnected.
     */
    RECONNECTED
}