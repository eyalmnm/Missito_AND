package ch.mitto.missito.security.signal.store.model;

import org.whispersystems.libsignal.state.PreKeyRecord;

import java.io.IOException;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class PreKeyData extends RealmObject {

    @PrimaryKey
    private int id;

    @Required
    private byte[] keyRecord;

    public PreKeyData(int id, PreKeyRecord record) {
        this.id = id;
        keyRecord = record.serialize();
    }

    public PreKeyData() {
    }

    public int getId() {
        return id;
    }

    public PreKeyRecord getPreKeyRecord() throws IOException {
        return new PreKeyRecord(keyRecord);
    }
}
