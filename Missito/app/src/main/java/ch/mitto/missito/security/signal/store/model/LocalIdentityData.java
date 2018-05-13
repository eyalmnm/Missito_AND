package ch.mitto.missito.security.signal.store.model;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class LocalIdentityData extends RealmObject {

    @PrimaryKey
    private int id = 1;            // we store only one object

    @Required
    private byte[] keyPair;

    public LocalIdentityData(IdentityKeyPair keyPair) {
        this.keyPair = keyPair.serialize();
    }

    public LocalIdentityData() {
    }

    public IdentityKeyPair getKeyPair() throws InvalidKeyException {
        return new IdentityKeyPair(keyPair);
    }

}
