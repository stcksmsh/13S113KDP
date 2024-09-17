package io.github.stcksmsh.kdp.distBuffer;

import io.github.stcksmsh.kdp.common.Logger;
import io.github.stcksmsh.kdp.common.NetworkMessage;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * Part of the distributed buffer system
 * Manages the routing of messages between buffer instances
 * Only used on a server node, it receives messages from managers and forwards them
 * to other managers which work the same job on other nodes
 *
 * @see DistributedSimBuffer
 * @see DistributedSimBufferManager
 */
public class DistributedSimBufferRouter{
    private final Logger logger;
    private final String TAG;
    /**
     * Maps manager ids to their sockets
     */
    private final Map<String, Consumer<NetworkMessage>> managerStreams;
    /**
     * Maps manager ids to the jobs they manage
     */
    public Map<String, List<String>> managerJobs;
    /**
     * Maps job ids to the manager that manages them
     */
    private final Map<String, List<String>> jobManagers;


    public DistributedSimBufferRouter(Logger logger) {
        this.logger = logger;
        this.TAG = Logger.getTAG();
        this.managerStreams = new ConcurrentHashMap<>();
        this.managerJobs = new ConcurrentHashMap<>();
        this.jobManagers = new ConcurrentHashMap<>();
    }

    /**
     * Adds a manager to the router
     * @param managerId The ID of the manager
     * @param managerStream The output stream to the manager
     */
    public void addManager(String managerId, Consumer<NetworkMessage> managerStream) {
        logger.D(TAG, "Adding manager '" + managerId + "'");
        managerStreams.put(managerId, managerStream);
        managerJobs.put(managerId, new CopyOnWriteArrayList<>());
    }

    /**
     * Adds a job to the router
     * @param jobId The ID of the job
     * @param managerIds The IDs of the managers that manage the job
     */
    public void addJob(String jobId, List<String> managerIds) {
        logger.D(TAG, "Adding job '" + jobId + "' with managers " + managerIds);
        jobManagers.put(jobId, new CopyOnWriteArrayList<>(managerIds));
        for (String managerId : managerIds) {
            managerJobs.get(managerId).add(jobId);
        }
    }

    /**
     * Removes a job from the router
     * @param jobId The ID of the job
     */
    public void removeJob(String jobId) {
        logger.D(TAG, "Removing job '" + jobId + "'");
        List<String> managers = jobManagers.get(jobId);
        if(managers == null){
            logger.E(TAG, "Tried to remove unknown job '" + jobId + "'");
            return;
        }
        for (String managerId : managers) {
            managerJobs.get(managerId).remove(jobId);
        }
        jobManagers.remove(jobId);
    }

    /**
     * Handles an event list message
     * @param message The message to handle
     */
    public void handleEventList(NetworkMessage message, String workerId) {
        if(!(message instanceof NetworkMessage.EventListMessage)){
            logger.E(TAG, "Received non-event list message");
            return;
        }
        NetworkMessage.EventListMessage<Object> eventListMessage = (NetworkMessage.EventListMessage<Object>) message;
        logger.D(TAG, "Received event list for job '" + eventListMessage.getEventList().getJobId() + "' from manager '" + workerId + "'");
        EventList<Object> eventList = eventListMessage.getEventList();
        if(!jobManagers.containsKey(eventList.getJobId())){
            logger.E(TAG, "Received event list for unknown job '" + eventList.getJobId() + "'");
            return;
        }
        for (String managerId : jobManagers.get(eventList.getJobId())) {
            /// TODO: Only send event to the worker which has the component with the matching destId
            try {
                Consumer<NetworkMessage> managerStream = managerStreams.get(managerId);
                synchronized (managerStream) {
                    managerStream.accept(message);
                }
            } catch (Exception e) {
                logger.E(TAG, "Failed to forward event list to manager");
                logger.E(e);
            }
        }
    }
}
