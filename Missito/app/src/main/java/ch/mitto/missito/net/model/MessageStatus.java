package ch.mitto.missito.net.model;

import java.io.Serializable;

public class MessageStatus implements Serializable {
    public String msgId;
    public String status;

    public MessageStatus(String msgId, String status) {
        this.msgId = msgId;
        this.status = status;
    }
}
