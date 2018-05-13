package ch.mitto.missito.db.model.common;

import io.realm.RealmObject;

public class RealmString extends RealmObject {

    public String string;

    public RealmString() {
    }

    public RealmString(String string) {
        this.string = string;
    }
}