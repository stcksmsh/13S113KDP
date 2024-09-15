package io.github.stcksmsh.kdp.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStream;

public class SynchronisedObjectInputStream {
    private ObjectInputStream inputStream;
    private Logger logger;
    private final String TAG = Logger.getTAG();

    public SynchronisedObjectInputStream(InputStream inputStream, Logger logger){
        try{
            this.inputStream = new ObjectInputStream(inputStream);
            this.logger = logger;
        }catch (IOException e) {
            logger.E(TAG, "Failed to create object input stream");
            logger.E(TAG, e);
        }
    }

    public synchronized Object readObject(){
        try{
            return inputStream.readObject();
        }catch (IOException e) {
            logger.E(TAG, "Failed to write object to output stream");
            logger.E(TAG, e);
        }catch (ClassNotFoundException e) {
            logger.E(TAG, "Failed to find class of object");
            logger.E(TAG, e);
        }
        return null;
    }
}
