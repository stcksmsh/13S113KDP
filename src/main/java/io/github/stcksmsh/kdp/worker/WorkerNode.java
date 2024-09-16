package io.github.stcksmsh.kdp.worker;


import io.github.stcksmsh.kdp.common.*;
import io.github.stcksmsh.kdp.distBuffer.DistributedSimBuffer;
import io.github.stcksmsh.kdp.distBuffer.DistributedSimBufferManager;
import rs.ac.bg.etf.sleep.simulation.*;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorkerNode extends Node {
    private final String TAG;
    private Socket serverNodeSocket = null;
    private final String serverNodeAddress;
    private final int serverNodePort;
    private SynchronizedObjectOutputStream serverNodeOut = null;
    private SynchronisedObjectInputStream serverNodeIn = null;
    private final DistributedSimBufferManager<Object> bufferManager;

    public WorkerNode(String logFilename, String serverNodeAddress, int serverNodePort) {
        super(logFilename);
        this.TAG = Logger.getTAG();
        this.serverNodeAddress = serverNodeAddress;
        this.serverNodePort = serverNodePort;
        this.bufferManager = new DistributedSimBufferManager<Object>(logger, this::sendEventList);
    }

    @Override
    public void start(){
        logger.I(TAG, "Starting worker node");
        /// First make a socket connection to the server node
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                serverNodeSocket = new Socket(serverNodeAddress, serverNodePort);
                logger.I(TAG, "Connected to server node");
                break;
            } catch (IOException e) {
                logger.W(TAG, "Failed to connect to server node. Retrying...");
                try {
                    Thread.sleep(RETRY_TIMEOUT);
                } catch (InterruptedException ex) {
                    logger.E(TAG, ex);
                }
            }
        }
        if(serverNodeSocket == null){
            logger.E(TAG, "Failed to connect to server node, exiting...");
            System.exit(1);
        }
        /// Then make the input and output streams
        try{
            serverNodeOut = new SynchronizedObjectOutputStream(serverNodeSocket.getOutputStream(), logger);
            serverNodeIn = new SynchronisedObjectInputStream(serverNodeSocket.getInputStream(), logger);
        }catch (IOException e){
            logger.E(TAG, e);
        }
        if(serverNodeOut == null || serverNodeIn == null){
            logger.E(TAG, "Failed to create input/output streams, exiting...");
            System.exit(2);
        }


        running = true;
        serverNodeOut.writeObject(new NetworkMessage.SignOnRequest());

        NetworkMessage signOnResponse = (NetworkMessage) serverNodeIn.readObject();
        if( signOnResponse == null ){
            logger.E(TAG, "Server node did not respond to sign on request");
            System.exit(3);
        }
        if(!(signOnResponse instanceof NetworkMessage.SignOnResponse)){
            logger.E(TAG, "Server node did not respond with a sign on response");
            System.exit(4);
        }else{
            logger.I(TAG, "Server node accepted sign on request");
        }

        running = true;
        new Thread(this::run).start();
    }

    private boolean running;
    private void run(){
        while(running) {
            NetworkMessage message = (NetworkMessage) serverNodeIn.readObject();
            if(message == null){
                logger.E(TAG, "Failed to read message from server node, exiting...");
                break;
            }
            switch (message.getType()){
                case NEW_JOB -> {
                    NetworkMessage.NewJobMessage<Object> newJobMessage = (NetworkMessage.NewJobMessage<Object>) message;
                    logger.D(TAG, "Received new job " + newJobMessage.getJobId() + " from server node");
                    handleNewJob(newJobMessage);
                }
                case EVENT_LIST -> {
                    NetworkMessage.EventListMessage eventListMessage = (NetworkMessage.EventListMessage) message;
                    logger.D(TAG, "Received event list for job " + eventListMessage.getEventList().getJobId() + " from server node");
                    handleEventList(eventListMessage);
                }
                case PING_REQUEST -> {
                    logger.D(TAG, "Received ping request from server node");
                    serverNodeOut.writeObject(new NetworkMessage.PingResponse());
                }
                default -> {
                    logger.W(TAG, "Received unexpected message type '" + message.getType() + "' from server node");
                }
            }

        }
    }

    private void sendEventList(NetworkMessage.EventListMessage eventListMessage){
        logger.D(TAG, "Sending event list for job " + eventListMessage.getEventList().getJobId() + " to server node with size " + eventListMessage.getEventList().getEvents().size());
        serverNodeOut.writeObject(eventListMessage);
    }

    private void handleNewJob(NetworkMessage.NewJobMessage newJobMessage){
        logger.I(TAG, "Received new job with ID: " + newJobMessage.getJobId());
        SimBuffer<Object> buffer = bufferManager.newJob(newJobMessage.getJobId(), newJobMessage.getNetList());

        createSimulation(buffer, newJobMessage.getNetList());
    }

    private DistributedSimBuffer<Object> makeBuffer(){
        return bufferManager.newJob("0", new Netlist<Object>());
    }

    private static int SIMULATOR_COUNT = 0;
    private void createSimulation(SimBuffer<Object> buffer, Netlist<Object> netList){
        new Thread(() -> {
            Simulator<Object> simulator = new SimulatorMultithread<Object>(++SIMULATOR_COUNT);
            simulator.setQueue(buffer);
            simulator.setNetlist(netList);
            logger.I(TAG, "Starting simulation");
            simulator.init();
            simulator.simulate();
        }).start();
    }

    private void handleEventList(NetworkMessage.EventListMessage eventListMessage) {
        bufferManager.giveEvents(eventListMessage.getEventList().getJobId(), eventListMessage.getEventList().getEvents());
    }
    
    public static void main(String[] args) {
        WorkerNode workerNode = new WorkerNode(args[0], args[1], Integer.parseInt(args[2]));
        workerNode.start();
        System.out.println("Worker node started");
    }
}
