package ch.mitto.missito.net.broker.model;

import java.io.Serializable;

/**
 * Created by usr1 on 1/3/18.
 */

public class ContactEntry implements Serializable {

    public String userId;
    public int deviceId;

    public ContactEntry(String userId, int deviceId) {
        this.userId = userId;
        this.deviceId = deviceId;
    }
}
