package io.github.stcksmsh.kdp.server;

import io.github.stcksmsh.kdp.common.Logger;
import io.github.stcksmsh.kdp.common.NetworkMessage;
import io.github.stcksmsh.kdp.common.Node;
import io.github.stcksmsh.kdp.distBuffer.DistributedSimBufferRouter;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerNode extends Node {
    private final String TAG;
    private ServerSocket serverSocket;
    private final DistributedSimBufferRouter<Object> router;

    public ServerNode(String logFilename, String serverAddress, int serverPort) {
        super(logFilename);
        this.TAG = Logger.getTAG();
        this.router = new DistributedSimBufferRouter<>(logger);
    }

    @Override
    public void start(){
        for(int i = 0; i < RETRY_COUNT; i++){
            try {
                serverSocket = new ServerSocket();
                break;
            } catch (IOException e) {
                logger.E(TAG, "Failed to create server socket, retrying...");
                try{
                    Thread.sleep(RETRY_TIMEOUT);
                }catch (InterruptedException ex){
                    logger.E(TAG, "Interrupted while waiting to retry");
                    logger.E(ex);
                }
            }
        }
        if(serverSocket == null){
            logger.E(TAG, "Failed to create server socket");
            return;
        }
        try {
            serverSocket.bind(null);
        } catch (IOException e) {
            logger.E(TAG, "Failed to bind server socket");
            logger.E(e);
            return;
        }
        running = true;
        new Thread(this::run).start();
    }

    private boolean running;
    private void run(){
        while(running){
            try {
                Socket clientSocket = serverSocket.accept();
                ObjectInputStream clientInput = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
                NetworkMessage message = (NetworkMessage) clientInput.readObject();
                switch (message.getType()){
                    case SIGN_ON_REQUEST -> {
                        String newManagerId = "1";
                        router.addManager(newManagerId, clientOutput);
                        logger.I(TAG, "Manager signed on: " + newManagerId);
                        clientOutput.writeObject(new NetworkMessage.SignOnResponse());
                    }
                    case EVENT_LIST -> {
                        router.handleEventList((NetworkMessage.EventListMessage) message);
                    }
                    default -> {
                        logger.E(TAG, "Received unexpected message type");
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                logger.E(TAG, "Failed to accept client connection");
                logger.E(e);
            }
        }
    }

    @Override
    protected String getTAG() {
        return TAG;
    }
}