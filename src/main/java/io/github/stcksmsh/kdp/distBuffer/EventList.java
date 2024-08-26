package io.github.stcksmsh.kdp.distBuffer;

import rs.ac.bg.etf.sleep.simulation.Event;

import java.io.Serializable;
import java.util.List;

public class EventList<T> implements Serializable {
    private final String jobId;
    private final List<Event<T>> events;

    public EventList(String jobId, List<Event<T>> events) {
        this.jobId = jobId;
        this.events = events;
    }

    public String getJobId() {
        return jobId;
    }

    public List<Event<T>> getEvents() {
        return events;
    }

}
