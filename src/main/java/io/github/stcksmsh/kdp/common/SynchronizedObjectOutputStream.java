package io.github.stcksmsh.kdp.common;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class SynchronizedObjectOutputStream {
    private ObjectOutputStream outputStream = null;
    private Logger logger;
    private final String TAG = Logger.getTAG();

    public SynchronizedObjectOutputStream(OutputStream outputStream, Logger logger) {
        this.logger = logger;
        try {
            this.outputStream = new ObjectOutputStream(outputStream);
        } catch (IOException e) {
            logger.E(TAG, "Failed to create object output stream");
            logger.E(TAG, e);
        }
    }

    public synchronized boolean writeObject(Object obj) {
        try {
            outputStream.writeObject(obj);
            outputStream.flush();  // Ensure data is sent
        } catch (IOException e) {
            logger.E(TAG, "Failed to write object to output stream");
            return false;
        }
        return true;
    }
}
