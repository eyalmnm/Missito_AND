package ch.mitto.missito.db.model;

import java.io.Serializable;
import java.util.UUID;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.net.broker.model.IncomingMessage;
import ch.mitto.missito.net.model.*;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


public class MessageRec extends RealmObject implements Serializable {

    @PrimaryKey
    public String localMsgId;

    public String serverMsgId;
    public long timestamp;
    public String body;
    public String uniqueId;
    public String groupId;
    public String threadId;
    public String senderUid;
    public int senderDeviceId;
    public String destUid;
    public int destDeviceId;
    public String contentType;
    public String incomingStatus;
    public String outgoingStatus;
    public AttachmentRec attach;

    public MessageRec() {
    }

    public MessageRec(IncomingMessage message, MessageBody body, IncomingMessageStatus incomingStatus, OutgoingMessageStatus outgoingStatus) {
        serverMsgId = message.id;
        localMsgId = UUID.randomUUID().toString();
        timestamp = message.timeSent * 1000;   //converting seconds to milliseconds
        senderUid = message.senderUid;
        senderDeviceId = message.senderDeviceId;
        destUid = Application.app.connectionManager.uid;
        destDeviceId = Application.app.connectionManager.deviceId;
        this.incomingStatus = incomingStatus == null ? null : incomingStatus.value;
        this.outgoingStatus = outgoingStatus == null ? null : outgoingStatus.value;
        if (body != null) {
            uniqueId = body.uniqueId;
            this.body = body.text;
            attach = AttachmentRec.make(body.attach);
        }
    }

    public MessageType getType() {
        if (attach == null) {
            return MessageType.TEXT;
        }
        if (attach.hasAudio()) {
            return MessageType.AUDIO;
        } else if (attach.hasLocations()) {
            return MessageType.GEO;
        } else if (attach.hasImages()) {
            return MessageType.IMAGE;
        } else if (attach.hasContacts()) {
            return MessageType.CONTACT;
        } else if (attach.hasVideo()) {
            return MessageType.VIDEO;
        }

        return null;
    }

    public void setAttach(AttachmentRec attach){
        this.attach = attach;
    }

    public void setBody(String body){
        this.body = body;
    }

    public static Builder getBuilder() {
        return new MessageRec().new Builder();
    }

    public class Builder {

        private Builder() {
        }

        public Builder setLocalMsgId(String localMsgId) {
            MessageRec.this.localMsgId = localMsgId;
            return this;
        }

        public Builder setServerMsgId(String serverMsgId) {
            MessageRec.this.serverMsgId = serverMsgId;
            return this;
        }

        public Builder setGroupId(String groupId) {
            MessageRec.this.groupId = groupId;
            return this;
        }

        public Builder setThreadId(String threadId) {
            MessageRec.this.threadId = threadId;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            MessageRec.this.timestamp = timestamp;
            return this;
        }

        public Builder setSenderUid(String uid) {
            MessageRec.this.senderUid = uid;
            return this;
        }

        public Builder setSenderDeviceId(int deviceId) {
            MessageRec.this.senderDeviceId = deviceId;
            return this;
        }

        public Builder setDestUid(String uid) {
            MessageRec.this.destUid = uid;
            return this;
        }

        public Builder setDestDeviceId(int deviceId) {
            MessageRec.this.destDeviceId = deviceId;
            return this;
        }

        public Builder setBody(String body) {
            MessageRec.this.body = body;
            return this;
        }

        public Builder setContentType(String contentType) {
            MessageRec.this.contentType = contentType;
            return this;
        }

        public Builder setIncominStatus(IncomingMessageStatus status) {
            MessageRec.this.incomingStatus = status.value;
            return this;
        }

        public Builder setOutgoingStatus(OutgoingMessageStatus status) {
            MessageRec.this.outgoingStatus = status.value;
            return this;
        }

        public Builder setAttach(AttachmentRec attach) {
            MessageRec.this.attach = attach;
            return this;
        }

        public MessageRec build() {
            MessageRec.this.localMsgId = UUID.randomUUID().toString();
            return MessageRec.this;
        }

    }
}
