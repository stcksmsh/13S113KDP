package io.github.stcksmsh.kdp.distBuffer;

import io.github.stcksmsh.kdp.common.Logger;
import io.github.stcksmsh.kdp.common.NetworkMessage;
import rs.ac.bg.etf.sleep.simulation.Event;
import rs.ac.bg.etf.sleep.simulation.Netlist;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * Part of the distributed buffer system
 * Manages local buffer instances and communicates with the router
 * Only used on a worker node, it receives messages from the router and forwards them
 * to the local buffer instances which work the same job
 *
 * @see DistributedSimBuffer
 * @see DistributedSimBufferRouter
 */
public class DistributedSimBufferManager<T> {
    private final Logger logger;
    private final Socket routerSocket;
    private final String TAG;
    private ObjectOutputStream routerOutput;
    private ObjectInputStream routerInput;
    private Map<String, DistributedSimBuffer<T>> buffers;
    private Map<String, Netlist<T>> netLists;
    private boolean running;

    /**
     * Initialises the manager
     * @param routerSocket The socket to the router
     * @param logger The logger to use
     */
    public DistributedSimBufferManager(Socket routerSocket, Logger logger) {
        this.routerSocket = routerSocket;
        this.logger = logger;
        this.TAG = Logger.getTAG();
        this.buffers = Map.of();
        this.netLists = Map.of();
    }

    public void start(){
        try {
            routerOutput = new ObjectOutputStream(routerSocket.getOutputStream());
            routerInput = new ObjectInputStream(routerSocket.getInputStream());
        } catch (Exception e) {
            logger.E(TAG, "Failed to open streams to router");
            logger.E(e);
            return;
        }
        running = true;
        new Thread(this::run).start();
    }

    public void stop() {
        running = false;
        try {
            routerOutput.close();
            routerInput.close();
            routerSocket.close();
        } catch (Exception e) {
            logger.E(TAG, "Failed to close streams to router");
            logger.E(e);
        }
    }

    private void run() {
        while (running) {
            try {
                EventList<T> events = (EventList<T>) routerInput.readObject();
                String jobId = events.getJobId();
                DistributedSimBuffer<T> buffer = buffers.get(jobId);
                if(buffer == null) {
                    logger.E(TAG, STR."Received events for unknown job id: \{jobId}");
                    continue;
                }
                buffers.get(jobId).receiveEvents(events.getEvents());
            } catch (Exception e) {
                logger.E(TAG, "Failed to read from router");
                logger.E(e);
            }
        }
    }

    private void sendEvents(String jobId, List<Event<T>> events) {
        /// First see if any of the events are for local buffers
        Netlist<T> netList = netLists.get(jobId);
        /// Remove events that are for components that are in the NetList
        events.removeIf(event -> netList.getComponent(event.getDstID()) != null);
        /// If there are no events left, return
        if (events.isEmpty()) return;
        /// Then send the rest to the router
        try {
            EventList<T> eventList = new EventList<>(jobId, events);
            routerOutput.writeObject(
                    new NetworkMessage.EventListMessage<>(eventList)
            );
        } catch (Exception e) {
            logger.E(TAG, "Failed to send events to router");
            logger.E(e);
        }
    }

    /**
     * Creates a new job
     * @param jobId The id of the job
     * @param netlist The netlist to simulate
     * @return The buffer for the job
     */
    DistributedSimBuffer<T> createJob(String jobId, Netlist<T> netlist) {
        netLists.put(jobId, netlist);
        DistributedSimBuffer<T> buffer = new DistributedSimBuffer<T>(events -> sendEvents(jobId, events));
        buffers.put(jobId, buffer);
        return buffer;
    }

}