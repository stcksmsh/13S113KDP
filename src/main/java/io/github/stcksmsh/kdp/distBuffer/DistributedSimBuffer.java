package io.github.stcksmsh.kdp.distBuffer;

import rs.ac.bg.etf.sleep.simulation.Event;
import rs.ac.bg.etf.sleep.simulation.SimBuffer;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

/**
 * A synchronised distributed buffer for use with multiple remote machines
 * This class serves as the "front end" of the package
 * Used to get elements from and put elements into the buffer
 *
 * @param <T> The type
 *
 * @see DistributedSimBufferManager
 * @see DistributedSimBufferRouter
 *
 */
public class DistributedSimBuffer<T> implements SimBuffer<T> {
    private final PriorityBlockingQueue<Event<T>> queue;
    private final Consumer<List<Event<T>>> consumer;

    /**
     * Initialises the buffer
     * @param consumer used to "give" events to the manager
     * @see DistributedSimBufferManager
     */
    public DistributedSimBuffer(Consumer<List<Event<T>>> consumer) {
        this.queue = new PriorityBlockingQueue<>();
        this.consumer = consumer;
    }

    /**
     * Used by the manager to "give" events to this buffer instance
     * Used instead of putEvent because we only need to give the event to this instance
     */
    void receiveEvent(Event<T> event) {
        queue.add(event);
        queue.notify();
    }

    private void sendEvent(Event<T> event) {
        consumer.accept(List.of(event));
    }

    private void sendEvents(List<Event<T>> events) {
        consumer.accept(events);
    }

    /**
     * @see #receiveEvent(Event)  
     */
    void receiveEvents(List<Event<T>> events){
        queue.addAll(events);
        queue.notifyAll();
    }

    @Override
    public void putEvent(Event<T> event) {
        queue.add(event);
        queue.notify();
        sendEvent(event); // Send event to other nodes
    }

    @Override
    public void putEvents(List<Event<T>> events) {
        queue.addAll(events);
        queue.notifyAll();
        sendEvents(events);
    }

    @Override
    public Event<T> getEvent() {
        try{
            return queue.take();
        }catch (InterruptedException e){
            return null;
        }
    }

    @Override
    public List<Event<T>> getEvents() {
        List<Event<T>> events = new LinkedList<>();
        if (queue.isEmpty()) {
            // Wait for an event to be received
            return events;
        }
        try {
            events.add(queue.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return events;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public long getMinrank() {
        return queue.isEmpty() ? Long.MAX_VALUE : queue.peek().getlTime();
    }
}
