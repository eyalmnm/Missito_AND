package ch.mitto.missito.security.signal.store.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class RegistrationIdData extends RealmObject {

    @PrimaryKey
    private int id = 1;            // we store only one object

    private int registrationId;


    public RegistrationIdData(int registrationId) {
        this.registrationId = registrationId;
    }

    public RegistrationIdData() {
    }

    public int getRegistrationId() {
        return registrationId;
    }
}
