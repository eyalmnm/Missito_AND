package ch.mitto.missito.security.signal.store.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class NextOTPKIdData extends RealmObject {

    @PrimaryKey
    private int id = 1;            // we store only one object

    private int nextId;

    public NextOTPKIdData(int nextId) {
        this.nextId = nextId;
    }

    public NextOTPKIdData() {
    }

    public int getNextId() {
        return nextId;
    }
}
