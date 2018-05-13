package ch.mitto.missito.net.broker.model;

import java.io.Serializable;

public class OfflineContact extends ContactEntry implements Serializable {

    public long lastSeen;

    public OfflineContact(String userId, int deviceId, long lastSeen) {
        super(userId, deviceId);
        this.lastSeen = lastSeen;
    }
}
