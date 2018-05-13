package ch.mitto.missito.ui.tabs.chats.model;

import android.ch.mitto.missito.R;

import java.io.Serializable;
import java.util.EnumMap;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.net.model.Attachment;
import ch.mitto.missito.net.model.OutgoingMessageStatus;

public class Message implements Serializable {

    public enum TYPE {
        AUDIO, TEXT, LOCATION, CONTACT, IMAGE, VIDEO
    }

    public String localMsgId;

    public String serverMsgId;

    public long timestamp;

    public String body;
    public String groupId;
    public String threadId;
    public String from;
    public String to;
    public String contentType;
    public String incomingStatus;
    public String outgoingStatus;
    public Attachment attach;

    private EnumMap<TYPE, Integer> msgTypeIcons;
    private EnumMap<TYPE, String> typeMessages;

    public Message() {
        msgTypeIcons = new EnumMap<>(TYPE.class);
        msgTypeIcons.put(TYPE.AUDIO, R.drawable.ic_mic_message);
        msgTypeIcons.put(TYPE.LOCATION, R.drawable.ic_location_svg);
        msgTypeIcons.put(TYPE.IMAGE, R.drawable.ic_picture);
        msgTypeIcons.put(TYPE.CONTACT, R.drawable.ic_contact);
        msgTypeIcons.put(TYPE.VIDEO, R.drawable.ic_video);

        typeMessages = new EnumMap<>(TYPE.class);
        typeMessages.put(TYPE.AUDIO, Application.app.getString(R.string.shared_audio));
        typeMessages.put(TYPE.LOCATION, Application.app.getString(R.string.shared_location));
        typeMessages.put(TYPE.IMAGE, Application.app.getString(R.string.shared_image));
        typeMessages.put(TYPE.CONTACT, Application.app.getString(R.string.shared_contact));
        typeMessages.put(TYPE.VIDEO, Application.app.getString(R.string.shared_video));
    }

    public Message(MessageRec messageRec) {
        this();
        this.serverMsgId = messageRec.serverMsgId;
        this.localMsgId = messageRec.localMsgId;
        this.timestamp = messageRec.timestamp;
        // TODO: add deviceId fields
        this.from = messageRec.senderUid;
        this.to = messageRec.destUid;
        this.incomingStatus = messageRec.incomingStatus;
        this.outgoingStatus = messageRec.outgoingStatus;
        this.body = messageRec.body;
        this.attach = Attachment.make(messageRec.attach);
    }

    public TYPE getMessageType() {
        TYPE type = null;
        if (attach == null) {
            type = TYPE.TEXT;
            return type;
        }

        if (!attach.audio.isEmpty()) {
            type = TYPE.AUDIO;
        } else if (!attach.locations.isEmpty()) {
            type = TYPE.LOCATION;
        } else if (!attach.images.isEmpty()) {
            type = TYPE.IMAGE;
        } else if (!attach.contacts.isEmpty()) {
            type = TYPE.CONTACT;
        } else if (!attach.video.isEmpty()) {
            type = TYPE.VIDEO;
        }

        return type;
    }

    public int getMessageIcon() {
        TYPE messageType = getMessageType();
        if (msgTypeIcons.containsKey(messageType)) {
            return msgTypeIcons.get(messageType);
        } else if (outgoingStatus != null) {
            return outgoingStatus.equals(OutgoingMessageStatus.SEEN.toString().toLowerCase())
                    ? R.drawable.ic_seen
                    : R.drawable.ic_chat_sent;
        } else {
            return R.drawable.ic_chat;
        }
    }

    public String getMsgText() {
        TYPE messageType = getMessageType();
        return typeMessages.containsKey(messageType) ? typeMessages.get(messageType) : body;
    }
}
