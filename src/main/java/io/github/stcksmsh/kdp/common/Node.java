package io.github.stcksmsh.kdp.common;

/// Represents a node in the network
abstract public class Node {
    protected final Logger logger;

    protected static final int RETRY_COUNT = 5;
    protected static final int RETRY_TIMEOUT = 1000;

    protected abstract String getTAG();

    protected abstract void start();

    public Node(String logFilename) {
        this.logger = Logger.getInstance(logFilename);
    }
    
    protected void logInfo(String message) {
        logger.I(getTAG(), message);
    }

    protected void logException(Exception e) {
        logger.E(getTAG(), e);
    }

    protected void logError(String message) {
        logger.E(getTAG(), message);
    }

    protected void logWarning(String message) {
        logger.W(getTAG(), message);
    }
}
