package io.github.stcksmsh.kdp.server;

import io.github.stcksmsh.kdp.common.*;
import io.github.stcksmsh.kdp.distBuffer.DistributedSimBufferRouter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rs.ac.bg.etf.sleep.simulation.Netlist;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerNode extends Node {
    private final String TAG;
    private ServerSocket serverSocket;
    private final DistributedSimBufferRouter<Object> router;
    private final int serverPort;
    private final Map<String, WorkerInfo> workers;
    private final Map<String, JobInfo> jobs;

    record JobInfo(List<String> workers, Netlist<Object> netlist) {
        static String generateJobId() {
            return UUID.randomUUID().toString();
        }
    }
    record WorkerInfo(SynchronizedObjectOutputStream workerOut, SynchronisedObjectInputStream workerIn, List<Integer> jobs) {
        static String generateWorkerId() {
            return UUID.randomUUID().toString();
        }
    }


    public ServerNode(String logFilename, int serverPort) {
        super(logFilename);
        this.TAG = Logger.getTAG();
        this.router = new DistributedSimBufferRouter<>(logger);
        this.serverPort = serverPort;
        this.workers = new ConcurrentHashMap<>();
        this.jobs = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        logger.I(TAG, "Starting server node");
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                serverSocket = new ServerSocket(serverPort);
                break;
            } catch (IOException e) {
                logger.E(TAG, "Failed to create server socket, retrying...");
                try {
                    Thread.sleep(RETRY_TIMEOUT);
                } catch (InterruptedException ex) {
                    logger.E(TAG, "Interrupted while waiting to retry");
                    logger.E(ex);
                }
            }
        }
        if (serverSocket == null) {
            logger.E(TAG, "Failed to create server socket");
            return;
        }
        running = true;
        new Thread(this::run).start();
    }

    private boolean running;

    private void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (IOException e) {
                logger.E(TAG, "Failed to accept client connection");
                logger.E(e);
            }
        }
    }

        private static final long PING_TIMEOUT = 1000;
    private void handleClient(@NotNull Socket clientSocket) {
        try {
            SynchronizedObjectOutputStream clientOutput = new SynchronizedObjectOutputStream(clientSocket.getOutputStream(), logger);
            SynchronisedObjectInputStream clientInput = new SynchronisedObjectInputStream(clientSocket.getInputStream(), logger);
            AtomicBoolean pingReceived = new AtomicBoolean(true);
            String newManagerId = WorkerInfo.generateWorkerId();
            Thread pingThread = new Thread(() -> {
                while (running) {
                    try {
                        Thread.sleep(PING_TIMEOUT);
                    } catch (InterruptedException e) {
                        try {
                            clientSocket.close();
                        } catch (IOException ex) {
                            logger.E(TAG, "Failed to close client socket");
                            logger.E(ex);
                        }
                        break;
                    }
                    if (!pingReceived.getAcquire()) {
                        logger.W(TAG, "Client " + clientSocket.getInetAddress() + " is not responding, closing connection");
                        running = false;
                        break;
                    }
                    pingReceived.setRelease(false);
                    clientOutput.writeObject(new NetworkMessage.PingRequest());
                }
            });
            boolean signedOn = false;
            while (clientSocket.isConnected()) {
                NetworkMessage message = (NetworkMessage) clientInput.readObject();
                if (message == null) {
                    logger.E(TAG, "Client " + clientSocket.getInetAddress() + " disconnected");
                    break;
                }
                switch (message.getType()) {
                    case SIGN_ON_REQUEST -> {
                        if (signedOn) {
                            logger.E(TAG, "Received sign on request after signing on");
                            break;
                        }
                        router.addManager(newManagerId, msg -> {
                            if (!clientOutput.writeObject(new NetworkMessage.EventListMessage<>(msg.getEventList()))) {
                                logger.E(TAG, "Failed to send event list to manager: " + newManagerId);
                            }
                        });
                        logger.I(TAG, "Manager signed on: " + newManagerId);
                        workers.put(newManagerId, new WorkerInfo(clientOutput, clientInput, new CopyOnWriteArrayList<>()));
                        clientOutput.writeObject(new NetworkMessage.SignOnResponse());
                        signedOn = true;
                        pingThread.start();
                    }
                    case EVENT_LIST -> {
                        if (!signedOn) {
                            logger.E(TAG, "Received event list before signing on");
                            break;
                        }
                        router.handleEventList((NetworkMessage.EventListMessage<Object>) message);
                    }
                    case PING_RESPONSE -> {
                        if (!signedOn) {
                            logger.E(TAG, "Received ping response before signing on");
                            break;
                        }
                        if (pingReceived.getAcquire()) {
                            logger.W(TAG, "Received unexpected ping response from manager");
                        } else {
                            pingReceived.setRelease(true);
                        }
                    }
                    default ->
                            logger.E(TAG, "Received unexpected message type '" + message.getType() + "' from manager " + clientSocket.getInetAddress());
                }
            }
            pingThread.interrupt();
        } catch (IOException e) {
            logger.E(TAG, "Failed to accept client connection");
            logger.E(e);
        }
    }

    @Nullable
    private String handleNewJob(Netlist<Object> netlist) {


        return null;
    }

    public static void main(@NotNull String[] args) {
        ServerNode serverNode = new ServerNode(args[0], Integer.parseInt(args[1]));
        serverNode.start();
        int cnt = 0;
        while (cnt < 2) {
            cnt = serverNode.router.managerJobs.size();
            serverNode.logger.I(serverNode.TAG, "Connected managers: " + cnt);
            try {
                Thread.sleep(RETRY_TIMEOUT);
            } catch (InterruptedException e) {
                serverNode.logger.E(serverNode.TAG, "Interrupted while waiting for managers to connect");
                serverNode.logger.E(serverNode.TAG, e);
            }
        }
        Netlist<Object> netlist = serverNode.loadNetlistFromFiles("", "");
        for (String key : serverNode.router.managerJobs.keySet().stream().toList()) {
            serverNode.logger.I(serverNode.TAG, "Manager: " + key + " has jobs: " + serverNode.router.managerJobs.get(key).size());
        }

    }

    @Nullable
    private Netlist<Object> loadNetlistFromFiles(String components,
                                                 String connections) {
        try {
            Netlist<Object> netlist = new Netlist<Object>();
            BufferedReader in = new BufferedReader(new FileReader(components));
            String s;
            while ((s = in.readLine()) != null) {
                String[] names = s.split(" ");
                netlist.addComponent(names);
            }
            in.close();
            in = new BufferedReader(new FileReader(connections));
            List<String[]> cc = new LinkedList<String[]>();
            while ((s = in.readLine()) != null) {
                String[] names = s.split(" ");
                cc.add(names);
            }
            in.close();
            String[][] con = new String[cc.size() - 1][];
            for (int i = 0; i < cc.size() - 1; i++) {
                con[i] = cc.get(i + 1);
            }
            netlist.addConnection(con);
            return netlist;
        } catch (IOException e) {
            logger.E(TAG, "Failed to load netlist from files '" + components + "' and '" + connections + "'");
            logger.E(TAG, e);
            return null;
        }
    }
}
