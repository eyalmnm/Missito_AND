package ch.mitto.missito.ui.tabs.chats.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.ChatRec;
import ch.mitto.missito.db.model.ContactRec;
import ch.mitto.missito.services.model.MissitoContact;

public class Chat implements Serializable {

    public String id;
    public Message lastMessage;
    public int unreadCount;
    public List<MissitoContact> participants = new ArrayList<>();
    public boolean isTyping;

    public Chat(String id, Message lastMessage, int unreadCount, List<MissitoContact> participants) {
        this.id = id;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.participants = participants;
    }

    public Chat(ChatRec chatRec) {
        id = chatRec.id;
        lastMessage = new Message(chatRec.lastMessage);
        unreadCount = chatRec.unreadCount;
        for (ContactRec participant : chatRec.participants) {
            participants.add(Application.app.contacts.missitoContactsByPhone.get(participant.phone));
        }
    }
}
