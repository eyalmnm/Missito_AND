package ch.mitto.missito.model.message;

import android.text.TextUtils;

import ch.mitto.missito.util.Helper;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by usr1 on 12/27/17.
 */

public class ContactData extends RealmObject {
    @PrimaryKey
    public String phone;
    public String firstName;
    public String lastName;

    public ContactData() {

    }

    public ContactData(String phone, String firstName, String lastName) {
        this.phone = phone;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public ContactData(ContactData contactData) {
        this(contactData.phone, contactData.firstName, contactData.lastName);
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ContactData)) {
            return false;
        }
        ContactData contactData = (ContactData) obj;
        if (!TextUtils.equals(Helper.removePlus(contactData.phone), Helper.removePlus(phone))) {
            return false;
        }
        return TextUtils.equals(lastName, contactData.lastName) &&
                TextUtils.equals(firstName, contactData.firstName);
    }
}
