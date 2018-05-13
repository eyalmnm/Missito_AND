package ch.mitto.missito.security.signal.store.model;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.IOException;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class LocalSignedPreKeyData extends RealmObject {

    @PrimaryKey
    private int id;

    @Required
    private byte[] keyRecord;

    public LocalSignedPreKeyData(int id, SignedPreKeyRecord keyRecord) {
        this.id = id;
        this.keyRecord = keyRecord.serialize();
    }

    public LocalSignedPreKeyData() {
    }

    public SignedPreKeyRecord getKeyRecord() throws InvalidKeyException, IOException {
        return new SignedPreKeyRecord(keyRecord);
    }

    public int getId() {
        return id;
    }
}
