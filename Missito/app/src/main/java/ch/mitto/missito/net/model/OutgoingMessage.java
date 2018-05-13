package ch.mitto.missito.net.model;

import ch.mitto.missito.util.Helper;

public class OutgoingMessage {

    public String destUid;
    public int destDeviceId;
    public String data;
    public String type;
    public Qos qos;

    public OutgoingMessage(String destUid, int destDeviceId, String data, String type, Qos qos) {
        this.destUid = Helper.removePlus(destUid);
        this.destDeviceId = destDeviceId;
        this.data = data;
        this.type = type;
        this.qos = qos;
    }
}
