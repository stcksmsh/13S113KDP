package io.github.stcksmsh.kdp.distBuffer;

import io.github.stcksmsh.kdp.common.Logger;
import io.github.stcksmsh.kdp.common.NetworkMessage;
import rs.ac.bg.etf.sleep.simulation.Event;
import rs.ac.bg.etf.sleep.simulation.Netlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Part of the distributed buffer system
 * Manages local buffer instances and communicates with the router
 * Only used on a worker node, it receives messages fromthe router and forwards them
 * to the local buffer instances which work the same job
 *
 * @see DistributedSimBuffer
 * @see DistributedSimBufferRouter
 */
public class DistributedSimBufferManager<T> {
    private final Logger logger;
    private final String TAG;
    private final Map<String, DistributedSimBuffer<T>> buffers;
    private final Map<String, Netlist<T>> netLists;
    private final Consumer<NetworkMessage.EventListMessage<T>> sendEvents;

    /**
     * Initialises the manager
     *
     * @param logger The logger to use
     */
    public DistributedSimBufferManager(Logger logger, Consumer<NetworkMessage.EventListMessage<T>> sendEvents) {
        this.logger = logger;
        this.TAG = Logger.getTAG();
        this.sendEvents = sendEvents;
        this.buffers = new HashMap<>();
        this.netLists = new HashMap<>();
    }

    /**
     * Used by the manager node to give events to the buffers
     *
     * @param jobId The ID of the job
     * @param events The events to give
     */
    public void giveEvents(String jobId, List<Event<T>> events) {
        logger.I(TAG, "Received events for job " + jobId + " with size " + events.size());
        DistributedSimBuffer<T> buffer = buffers.get(jobId);
        Netlist<T> netlist = netLists.get(jobId);
        if (buffer == null) {
            logger.E(TAG, "No buffer for job " + jobId);
            return;
        }
        if (netlist == null) {
            logger.E(TAG, "No netlist for job " + jobId);
            return;
        }
        List<Event<T>> filteredEvents = new ArrayList<>();
        for(Event<T> e : events){
            long dstId = e.getDstID();
            if(netlist.getComponent(dstId) == null){
                logger.E(TAG, "Event destination " + dstId + " not found in netlist");
                continue;
            }
            filteredEvents.add(e);
        }
        buffer.giveEvents(filteredEvents);
    }

    /**
     * Creates a new job
     * @param jobId The id of the job
     * @param netlist The netlist to simulate
     * @return The buffer for the job
     */
    public DistributedSimBuffer<T> newJob(String jobId, Netlist<T> netlist) {
        logger.I(TAG, "Received new job with ID: " + jobId);
        netLists.put(jobId, netlist);
        DistributedSimBuffer<T> buffer = new DistributedSimBuffer<>(events -> {
            List<Event<T>> filteredEvents = new ArrayList<>();
            for (Event<T> e : events) {
                long srcId = e.getSrcID();
                long dstId = e.getDstID();
                if(netlist.getComponent(srcId) == null){
                    logger.E(TAG, "Event source " + srcId + " not found in netlist");
                    continue;
                }
                if(netlist.getComponent(dstId) == null){
                    filteredEvents.add(e);
                }
            }
            if(!filteredEvents.isEmpty()){
                sendEvents.accept(new NetworkMessage.EventListMessage<>(
                        new EventList<>(jobId, filteredEvents)
                ));
            }
        });
        buffers.put(jobId, buffer);
        return buffer;
    }

    public void removeJob(String jobId) {
        logger.I(TAG, "Removing job " + jobId);
        buffers.remove(jobId);
        netLists.remove(jobId);
    }

}