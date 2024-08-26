package io.github.stcksmsh.kdp.distBuffer;

import io.github.stcksmsh.kdp.common.Logger;
import io.github.stcksmsh.kdp.common.NetworkMessage;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.List;

/**
 * Part of the distributed buffer system
 * Manages the routing of messages between buffer instances
 * Only used on a server node, it receives messages from managers and forwards them
 * to other managers which work the same job on other nodes
 *
 * @see DistributedSimBuffer
 * @see DistributedSimBufferManager
 */
public class DistributedSimBufferRouter<T>{
    private final Logger logger;
    private final String TAG;
    /**
     * Maps manager ids to their sockets
     */
    private Map<String, ObjectOutputStream> managerStreams;
    /**
     * Maps manager ids to the jobs they manage
     */
    private Map<String, List<String>> managerJobs;
    /**
     * Maps job ids to the manager that manages them
     */
    private Map<String, List<String>> jobManagers;


    public DistributedSimBufferRouter(Logger logger) {
        this.logger = logger;
        this.TAG = Logger.getTAG();
        this.managerStreams = Map.of();
    }

    /**
     * Adds a manager to the router
     * @param managerId The ID of the manager
     * @param managerStream The output stream to the manager
     */
    public void addManager(String managerId, ObjectOutputStream managerStream) {
        managerStreams.put(managerId, managerStream);
        managerJobs.put(managerId, List.of());
    }

    /**
     * Adds a job to the router
     * @param jobId The ID of the job
     * @param managerIds The IDs of the managers that manage the job
     */
    public void addJob(String jobId, List<String> managerIds) {
        jobManagers.put(jobId, managerIds);
        for (String managerId : managerIds) {
            managerJobs.get(managerId).add(jobId);
        }
    }

    /**
     * Handles an event list message
     * @param message The message to handle
     */
    public void handleEventList(NetworkMessage.EventListMessage<T> message) {
        EventList<T> eventList = message.getEventList();
        for (String managerId : jobManagers.get(eventList.getJobId())) {
            try {
                ObjectOutputStream managerStream = managerStreams.get(managerId);
                synchronized (managerStream) {
                    managerStream.writeObject(message);
                }
            } catch (Exception e) {
                logger.E(TAG, "Failed to forward event list to manager");
                logger.E(e);
            }
        }
    }
}
