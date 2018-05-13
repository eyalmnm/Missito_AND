package ch.mitto.missito.services;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.ContactRec;
import ch.mitto.missito.net.broker.model.ContactEntry;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.net.broker.model.ContactsStatusModel;
import ch.mitto.missito.net.broker.model.OfflineContact;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.RealmDBHelper;

public class Contacts {
    private final String LOG_TAG = Contacts.class.getSimpleName();

    public Map<String, MissitoContact> missitoContactsByPhone;
    public Map<String, MissitoContact> systemContacts;
    public Map<String, Integer> deviceIds;
    public ArrayList<MissitoContact> missitoContacts;

    public Contacts() {
        missitoContactsByPhone = new HashMap<>();
        systemContacts = new HashMap<>();
        deviceIds = new HashMap<>();
        missitoContacts = new ArrayList<>();
    }

    public void loadSystemContacts() {
        systemContacts = Helper.fetchContacts(Application.app);
    }

    public void loadMissitoContacts() {
        List<ContactRec> contacts = RealmDBHelper.getMissitoContacts();
        for (ContactRec contact : contacts) {
            missitoContactsByPhone.put(contact.phone, new MissitoContact(contact));
            if (!deviceIds.containsKey(contact.phone) && contact.lastActiveDeviceId != 0) {
                deviceIds.put(contact.phone, contact.lastActiveDeviceId);
            }
        }

        if (missitoContacts.isEmpty()) {
            missitoContacts.addAll(missitoContactsByPhone.values());
        }
    }

    public void addMissitoContacts(ArrayList<ContactEntry> contacts, Date availableSince) {
        Map<String, MissitoContact> missitoContacts = new HashMap<>();
        for (ContactEntry contact : contacts) {
            String phone = Helper.addPlus(contact.userId);
            missitoContacts.put(phone, new MissitoContact(getContactName(phone), phone,
                    0, availableSince, getContactAvatarPath(phone)));
        }
        missitoContactsByPhone.putAll(missitoContacts);
        this.missitoContacts.addAll(missitoContacts.values());
    }

    public boolean contactsLoaded() {
        return missitoContactsByPhone.size() != 0;
    }

    public void updateStatus(ContactsStatusModel contactsStatus) {

        if(contactsStatus.msg.online != null) {
            for (ContactEntry contactEntry : contactsStatus.msg.online) {
                MissitoContact missitoContact = missitoContactsByPhone.get(contactEntry.userId);
                if (missitoContact != null) {
                    missitoContact.isOnline = true;
                    deviceIds.put(contactEntry.userId, contactEntry.deviceId);
                    RealmDBHelper.updateContactLastActiveDeviceId(missitoContact, contactEntry.deviceId);
                }
            }
        }

        if(contactsStatus.msg.offline != null) {
            for (OfflineContact offlineContact : contactsStatus.msg.offline) {
                String number = offlineContact.userId;

                MissitoContact missitoContact = missitoContactsByPhone.get(number);
                if (missitoContact != null) {
                    missitoContact.isOnline = false;
                    missitoContact.lastSeen = offlineContact.lastSeen;
                    deviceIds.put(offlineContact.userId, offlineContact.deviceId);
                    RealmDBHelper.updateContactLastActiveDeviceId(missitoContact, offlineContact.deviceId);
                }
            }
        }

        for (ContactEntry contactEntry : contactsStatus.msg.blocked) {

            MissitoContact missitoContact = missitoContactsByPhone.get(contactEntry.userId);
            if (missitoContact != null) {
                missitoContact.blocked = true;
                deviceIds.put(contactEntry.userId, contactEntry.deviceId);
                RealmDBHelper.updateContactLastActiveDeviceId(missitoContact, contactEntry.deviceId);
            }
        }

        for (ContactEntry contactEntry : contactsStatus.msg.muted) {

            MissitoContact missitoContact = missitoContactsByPhone.get(contactEntry.userId);
            if (missitoContact != null) {
                missitoContact.muted = true;
                deviceIds.put(contactEntry.userId, contactEntry.deviceId);
                RealmDBHelper.updateContactLastActiveDeviceId(missitoContact, contactEntry.deviceId);
            }
        }

        missitoContacts.clear();
        missitoContacts.addAll(missitoContactsByPhone.values());
    }

    public void incrementContactUnreadCount(String phoneNumber) {
        MissitoContact contact = missitoContactsByPhone.get(phoneNumber);
        if (contact != null) {
            RealmDBHelper.incrementUnreadCount(contact);
        }
    }

    public void setContactsBlocked(List<String> phones, boolean blocked) {
        for (String phone : phones) {
            MissitoContact contact = missitoContactsByPhone.get(phone);
            if (contact != null) {
                contact.blocked = blocked;
            }
        }
    }

    public void setContactsMuted(List<String> phones, boolean muted) {
        for (String phone : phones) {
            MissitoContact contact = missitoContactsByPhone.get(phone);
            if (contact != null) {
                contact.muted = muted;
            }
        }
    }

    public void resetContactUnreadCount(String phoneNumber) {
        MissitoContact contact = missitoContactsByPhone.get(phoneNumber);
        if (contact != null) {
            RealmDBHelper.resetUnreadCount(contact);
        }
    }

    public String getContactName(String phoneNumber) {
        MissitoContact contact = systemContacts.get(phoneNumber);
        return contact == null ? phoneNumber : contact.name;
    }

    public String getContactAvatarPath(String phoneNumber) {
        MissitoContact contact = systemContacts.get(phoneNumber);
        return contact == null ? null : contact.avatarPath;
    }

    public boolean isBlocked(String phone) {
        MissitoContact contact = missitoContactsByPhone.get(phone);
        return contact != null && contact.blocked;
    }
}
