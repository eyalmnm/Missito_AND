package ch.mitto.missito.net.model;

import java.io.Serializable;
import java.util.ArrayList;

import ch.mitto.missito.db.model.attach.ContactAttachRec;
import ch.mitto.missito.db.model.common.RealmString;
import io.realm.RealmList;

public class ContactAttachment implements Serializable {

    public String name;
    public String surname;
    public ArrayList<String> phones = new ArrayList<>();
    public ArrayList<String> emails = new ArrayList<>();
    public String notes;
    public String avatar;

    public ContactAttachment() {
    }

    public ContactAttachment(ContactAttachRec contactAttachRec) {
        name = contactAttachRec.name;
        surname = contactAttachRec.surname;
        phones.addAll(getList(contactAttachRec.phones));
        emails.addAll(getList(contactAttachRec.emails));
        notes = contactAttachRec.notes;
        avatar = contactAttachRec.avatar;
    }

    private ArrayList<String> getList(RealmList<RealmString> list) {
        ArrayList<String> realmList = new ArrayList<>();
        for (RealmString item : list) {
            realmList.add(item.string);
        }
        return realmList;
    }
}
