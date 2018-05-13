package ch.mitto.missito.net.model;

public enum IncomingMessageStatus {

    RECEIVED("received"),
    RECEIVED_ACKNOWLEDGED("rcv_ack"),
    SEEN("seen"),
    SEEN_ACKNOWLEDGED("seen_ack"),
    FAILED("failed");

    public String value;

    IncomingMessageStatus(String value) {
        this.value = value;
    }
}
