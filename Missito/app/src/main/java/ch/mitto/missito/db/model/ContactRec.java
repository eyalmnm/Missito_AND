package ch.mitto.missito.db.model;

import java.util.Date;

import ch.mitto.missito.Application;
import ch.mitto.missito.services.model.MissitoContact;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class ContactRec extends RealmObject {

    public static long NEW_CONTACT_TIME_MS = 12 * 60 * 60 * 1000;

    public String name;
    @PrimaryKey
    public String phone;
    public int lastActiveDeviceId;
    @Ignore
    public boolean isOnline;
    public boolean blocked;
    public int unreadCount;
    public long lastSeen;
    public Date availableSince;
    public String avatarPath;

    public ContactRec() {
    }

    public ContactRec(String name, String phone, int deviceId, Date availableSince) {
        this.name = name;
        this.phone = phone;
        this.availableSince = availableSince;
        this.avatarPath = Application.app.contacts.getContactAvatarPath(phone);
        this.lastActiveDeviceId = deviceId;
    }

    public ContactRec(MissitoContact contact, int deviceId) {
        this.name = contact.name;
        this.phone = contact.phone;
        this.isOnline = contact.isOnline;
        this.blocked = contact.blocked;
        this.unreadCount = contact.unreadCount;
        this.lastSeen = contact.lastSeen;
        this.availableSince = contact.availableSince;
        this.avatarPath = contact.avatarPath;
        this.lastActiveDeviceId = deviceId;
    }

    public boolean isNew() {
        return availableSince != null &&
                System.currentTimeMillis() - availableSince.getTime() < NEW_CONTACT_TIME_MS;
    }
}
