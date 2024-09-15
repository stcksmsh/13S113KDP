package io.github.stcksmsh.kdp.common;

import io.github.stcksmsh.kdp.distBuffer.EventList;
import rs.ac.bg.etf.sleep.simulation.Netlist;

import java.io.Serial;
import java.io.Serializable;

abstract public class NetworkMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        SIGN_ON_REQUEST,
        SIGN_ON_RESPONSE,
        EVENT_LIST,
        NEW_JOB,
        PING_REQUEST,
        PING_RESPONSE,
    }

    abstract public MessageType getType();

    public static class SignOnRequest extends NetworkMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public MessageType getType() {
            return MessageType.SIGN_ON_REQUEST;
        }
    }

    public static class SignOnResponse extends NetworkMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public MessageType getType() {
            return MessageType.SIGN_ON_RESPONSE;
        }
    }

    public static class PingRequest extends NetworkMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public MessageType getType() {
            return MessageType.PING_REQUEST;
        }
    }

    public static class PingResponse extends NetworkMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public MessageType getType() {
            return MessageType.PING_RESPONSE;
        }
    }

    public static class EventListMessage<T> extends NetworkMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        private final EventList<T> eventList;

        public EventListMessage(EventList<T> events) {
            this.eventList = events;
        }

        public EventList<T> getEventList() {
            return eventList;
        }

        @Override
        public MessageType getType() {
            return MessageType.EVENT_LIST;
        }
    }

    public static class NewJobMessage<T> extends NetworkMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String jobId;

        private final Netlist<T> Netlist;

        public NewJobMessage(String jobId, Netlist<T> Netlist) {
            this.jobId = jobId;
            this.Netlist = Netlist;
        }

        public String getJobId() {
            return jobId;
        }

        public Netlist<T> getNetList() {
            return Netlist;
        }

        @Override
        public MessageType getType() {
            return MessageType.NEW_JOB;
        }
    }

}
