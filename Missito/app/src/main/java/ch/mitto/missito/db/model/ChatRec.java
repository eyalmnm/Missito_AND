package ch.mitto.missito.db.model;

import java.io.Serializable;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ChatRec extends RealmObject implements Serializable {

    @PrimaryKey
    public String id;
    public MessageRec lastMessage;
    public int unreadCount;
    public RealmList<ContactRec> participants = new RealmList<>();

    public ChatRec() {
    }

    public ChatRec(MessageRec lastMessage, String id, int unreadCount, RealmList<ContactRec> participants) {
        this.lastMessage = lastMessage;
        this.id = id;
        this.participants = participants;
        this.unreadCount = unreadCount;
    }
}
