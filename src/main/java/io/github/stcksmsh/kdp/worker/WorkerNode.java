package io.github.stcksmsh.kdp.worker;

import io.github.stcksmsh.kdp.common.Logger;
import io.github.stcksmsh.kdp.common.Node;
import rs.ac.bg.etf.sleep.simulation.Event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class WorkerNode extends Node {
    private final String TAG;
    private Socket serverNodeSocket = null;
    private final String serverNodeAddress;
    private final int serverNodePort;
    private ObjectOutputStream serverNodeOut = null;
    private ObjectInputStream serverNodeIn
    
    public WorkerNode(int port, String logFilename, String serverNodeAddress, int serverNodePort) {
        super(port, logFilename);
        this.TAG = Logger.getTAG();
        this.serverNodeAddress = serverNodeAddress;
        this.serverNodePort = serverNodePort;
    }

    @Override
    public void start(){
        /// First make a socket connection to the server node
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                serverNodeSocket = new Socket(serverNodeAddress, serverNodePort);
                logInfo("Connected to server node");
                break;
            } catch (IOException e) {
                logWarning("Failed to connect to server node. Retrying...");
                try {
                    Thread.sleep(RETRY_TIMEOUT);
                } catch (InterruptedException ex) {
                    logException(ex);
                }
            }
        }
        if(serverNodeSocket == null){
            logError("Failed to connect to server node");
            System.exit(1);
        }
        /// Then make the input and output streams
        try {
            serverNodeOut = new ObjectOutputStream(serverNodeSocket.getOutputStream());
            serverNodeIn = new ObjectInputStream(serverNodeSocket.getInputStream());
        } catch (IOException e) {
            logError("Failed to create input/output streams");
            logException(e);
            System.exit(2);
        }
    }

    protected void handleClientConnection(Socket clientSocket) {
        new Thread(() -> {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                Event<Object> event = (Event<Object>) in.readObject();
                if (event != null) {
//                    buffer.putEvent(event);
                    logInfo("Received event with destPort: " + event.getDstPort());
                }
            } catch (IOException | ClassNotFoundException e) {
                logException(e);
            }
        }).start();
    }

    @Override
    protected String getTAG() {
        return TAG;
    }
}
