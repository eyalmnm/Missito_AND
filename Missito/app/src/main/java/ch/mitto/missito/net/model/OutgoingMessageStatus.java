package ch.mitto.missito.net.model;

import java.util.HashMap;

public enum OutgoingMessageStatus {

    OUTGOING("outgoing"),
    SENT("sent"),
    RECEIVED("received"),
    SEEN("seen");

    public String value;
    private static HashMap<String, OutgoingMessageStatus> valueMap = new HashMap<>(4);

    static {
        for (OutgoingMessageStatus status : OutgoingMessageStatus.values()) {
            valueMap.put(status.value, status);
        }
    }

    OutgoingMessageStatus(String value) {
        this.value = value;
    }

    public static OutgoingMessageStatus fromString(String string) {
        return valueMap.get(string);
    }
}
