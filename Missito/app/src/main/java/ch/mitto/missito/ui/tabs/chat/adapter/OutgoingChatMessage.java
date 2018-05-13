package ch.mitto.missito.ui.tabs.chat.adapter;

import java.util.Date;

import ch.mitto.missito.db.model.MessageType;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.net.model.OutgoingMessageStatus;

public class OutgoingChatMessage extends ChatMessage {

    public OutgoingMessageStatus status;

    public OutgoingChatMessage(Direction direction, Date date, MessageType type, String text,
                       String serverId, String id, AttachmentRec attachmentRec,
                               OutgoingMessageStatus status) {
        super(direction, date, type, text, serverId, id, attachmentRec);
        this.status = status;
    }

    public OutgoingChatMessage(MessageRec messageRec) {
        super(messageRec, Direction.OUTGOING);
        status = OutgoingMessageStatus.fromString(messageRec.outgoingStatus);
    }

}
