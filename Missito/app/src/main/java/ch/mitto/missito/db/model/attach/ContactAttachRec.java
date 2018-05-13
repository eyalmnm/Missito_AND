package ch.mitto.missito.db.model.attach;


import java.io.Serializable;
import java.util.ArrayList;

import ch.mitto.missito.db.model.common.RealmString;
import ch.mitto.missito.net.model.ContactAttachment;
import io.realm.RealmList;
import io.realm.RealmObject;

public class ContactAttachRec extends RealmObject implements Serializable {

    public String name;
    public String surname;
    public RealmList<RealmString> phones = new RealmList<>();
    public RealmList<RealmString> emails = new RealmList<>();
    public String notes;
    public String avatar;

    public ContactAttachRec() {
    }

    public ContactAttachRec(String name, RealmList<RealmString> phones, RealmList<RealmString> emails, String avatar) {
        this.name = name;
        this.phones = phones;
        this.emails = emails;
        this.avatar = avatar;
    }

    public ContactAttachRec(ContactAttachment contactAttachment) {
        name = contactAttachment.name;
        surname = contactAttachment.surname;
        phones.addAll(getRealmList(contactAttachment.phones));
        emails.addAll(getRealmList(contactAttachment.emails));
        notes = contactAttachment.notes;
        avatar = contactAttachment.avatar;
    }

    private RealmList<RealmString> getRealmList(ArrayList<String> list) {
        RealmList<RealmString> realmList = new RealmList<>();
        for (String item : list) {
            realmList.add(new RealmString(item));
        }
        return realmList;
    }
}
