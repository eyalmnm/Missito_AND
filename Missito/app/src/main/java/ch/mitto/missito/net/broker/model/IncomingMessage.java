package ch.mitto.missito.net.broker.model;

import java.io.Serializable;

public class IncomingMessage implements Serializable {

    public String id;
    public String senderUid;
    public int senderDeviceId;
    public String msg;
    public String msgType;
    public long timeSent;

}
