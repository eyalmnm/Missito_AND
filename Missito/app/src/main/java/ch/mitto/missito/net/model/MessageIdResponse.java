package ch.mitto.missito.net.model;

import java.io.Serializable;

public class MessageIdResponse implements Serializable {

    public String messageId;

    public MessageIdResponse(String messageId) {
        this.messageId = messageId;
    }
}
