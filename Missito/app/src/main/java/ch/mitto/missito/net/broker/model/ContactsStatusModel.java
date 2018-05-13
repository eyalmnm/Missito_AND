package ch.mitto.missito.net.broker.model;


import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;

import ch.mitto.missito.util.Helper;

public class ContactsStatusModel implements Serializable {

    public String msgType;
    public BodyMessage msg;

    // Tmp method. Used to add "+" sign for all phone numbers provided by backend
    public static ContactsStatusModel fromJson(String json) {
        ContactsStatusModel csm = new Gson().fromJson(json, ContactsStatusModel.class);
        if (csm.msg != null) {
            csm.msg.blocked = Helper.addPlus(csm.msg.blocked);
            csm.msg.muted = Helper.addPlus(csm.msg.muted);

            if (csm.msg.online != null) {
                for (ContactEntry contactEntry : csm.msg.online) {
                    contactEntry.userId = Helper.addPlus(contactEntry.userId);
                }
            }

            if (csm.msg.offline != null) {
                for (OfflineContact offlineContact : csm.msg.offline) {
                    offlineContact.userId = Helper.addPlus(offlineContact.userId);
                }
            }
        }
        return csm;
    }

    public class BodyMessage implements Serializable {
        public ArrayList<ContactEntry> online = new ArrayList<>();
        public ArrayList<OfflineContact> offline = new ArrayList<>();
        public ArrayList<ContactEntry> blocked = new ArrayList<>();
        public ArrayList<ContactEntry> muted = new ArrayList<>();
    }
}
