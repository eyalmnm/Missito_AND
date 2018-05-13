package ch.mitto.missito.services.model;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.res.Resources;

import java.io.Serializable;
import java.util.Date;

import ch.mitto.missito.db.model.ContactRec;
import ch.mitto.missito.net.ConnectionChangeReceiver;
import ch.mitto.missito.util.Helper;

import static ch.mitto.missito.db.model.ContactRec.NEW_CONTACT_TIME_MS;

public class MissitoContact implements Serializable {

    public String name;
    public String firstName;
    public String lastName;
    public String phone;
    public boolean isOnline;
    public boolean blocked;
    public boolean muted;
    public int unreadCount;
    public long lastSeen;
    public Date availableSince;
    public String avatarPath;

    public MissitoContact(String name, String phone, int unreadCount, Date availableSince, String avatar) {
        this.avatarPath = avatar;
        this.name = name;
        this.phone = phone;
        this.unreadCount = unreadCount;
        this.availableSince = availableSince;
    }

    public MissitoContact(ContactRec contact) {
        this(contact.name, contact.phone, contact.unreadCount, contact.availableSince, contact.avatarPath);

        this.isOnline = contact.isOnline;
        this.blocked = contact.blocked;
        this.lastSeen = contact.lastSeen;
    }

    public MissitoContact(String name, String firstname, String lastname, String phone, int unreadCount, Date availableSince, String avatar) {
        this(name, phone, unreadCount, availableSince, avatar);
        this.firstName = firstname;
        this.lastName = lastname;
    }

    public MissitoContact() {
    }

    public String getLastSeenLabel(Context context) {
        if (ConnectionChangeReceiver.isOnline()) {
            Resources res = context.getResources();
            String s = Helper.formatWhen(context, lastSeen * 1000);
            return String.valueOf(isOnline
                    ? res.getString(R.string.online)
                    : s);
        }

        return "";
    }

    @Override
    public boolean equals(Object contact) {
        if (!(contact instanceof MissitoContact)) {
            return false;
        }
        MissitoContact missitoContact = (MissitoContact) contact;
        return missitoContact.phone.equals(phone);
    }

    public boolean isNew() {
        return availableSince != null &&
                System.currentTimeMillis() - availableSince.getTime() < NEW_CONTACT_TIME_MS;
    }
}
