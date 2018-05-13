package ch.mitto.missito.ui.tabs.chat.adapter;

import java.util.Date;

import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.db.model.MessageType;

public class IncomingChatMessage extends ChatMessage {

    public MissitoContact senderContact;
    public int senderDeviceId;

    public IncomingChatMessage(Direction direction, Date date, MessageType type, int senderDeviceId, String text,
                       String serverId, String id, AttachmentRec attachmentRec,
                               MissitoContact senderContact) {
        super(direction, date, type, text, serverId, id, attachmentRec);
        this.senderContact = senderContact;
        this.senderDeviceId = senderDeviceId;
    }

    public IncomingChatMessage(MessageRec messageRec, MissitoContact senderContact) {
        super(messageRec, Direction.INCOMING);
        this.senderContact = senderContact;
        this.senderDeviceId = messageRec.senderDeviceId;
    }
}
