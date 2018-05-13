package ch.mitto.missito.ui.tabs.chat.adapter;

import java.util.Date;

import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.db.model.MessageType;

public class ChatMessage {

    public Direction direction;
    public Date date;
    public MessageType type;
    public MessageInGroupType inGroupType = MessageInGroupType.SINGLE;

    public String text;
    public String serverId;
    public String id;
    public ChatMessageAttachment attachmentRec;
    public float progress;
    public boolean isLoading;


//    public ChatMessage(){
//        this(Direction.OUTGOING, new Date(), MessageType.TEXT);
//    }

    public ChatMessage(Direction direction, Date date, MessageType type, String text,
                       String serverId, String id, AttachmentRec attachmentRec) {
        this.direction = direction;
        this.date = date;
        this.type = type;
        this.text = text;
        this.serverId = serverId;
        this.id = id;
        this.attachmentRec = attachmentRec == null ? null : new ChatMessageAttachment(attachmentRec);
    }

    public ChatMessage(MessageRec messageRec, Direction direction) {
        this(direction, new Date(messageRec.timestamp), messageRec.getType(), messageRec.body,
                messageRec.serverMsgId, messageRec.localMsgId, messageRec.attach);
    }

    public static ChatMessage make(MessageRec messageRec, MissitoContact companion) {
        // TODO: consider deviceId
        if (messageRec.destUid != null && messageRec.destUid.equals(companion.phone)) {
            return new OutgoingChatMessage(messageRec);
        } else {
            return new IncomingChatMessage(messageRec, companion);
        }
    }


    public enum Direction {
        INCOMING, OUTGOING
    }

    public enum MessageInGroupType {
        SINGLE, FIRST, LAST, MIDDLE
    }

}
