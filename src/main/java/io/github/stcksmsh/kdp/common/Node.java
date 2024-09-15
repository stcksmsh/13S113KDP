package io.github.stcksmsh.kdp.common;

/// Represents a node in the network
abstract public class Node {
    protected final Logger logger;

    protected static final int RETRY_COUNT = 5;
    protected static final int RETRY_TIMEOUT = 1000;

    protected abstract void start();

    public Node(String logFilename) {
        this.logger = Logger.getInstance(logFilename);
    }

}