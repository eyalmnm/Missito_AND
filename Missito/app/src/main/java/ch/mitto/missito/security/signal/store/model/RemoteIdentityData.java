package ch.mitto.missito.security.signal.store.model;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class RemoteIdentityData extends RealmObject {

    @PrimaryKey
    private String id;

    @Required
    private String name;

    private int deviceId;

    @Required
    private byte[] key;

    public RemoteIdentityData(SignalProtocolAddress address, IdentityKey identityKey) {
        key = identityKey.serialize();
        name = address.getName();
        deviceId = address.getDeviceId();
        id = calcId(name, deviceId);
    }

    public RemoteIdentityData() {
    }

    public static String calcId(String name, int deviceId) {
        return name + "_" + deviceId;
    }

    public SignalProtocolAddress getSignalProtocolAddress() {
        return new SignalProtocolAddress(name, deviceId);
    }

    public IdentityKey getIdentityKey() throws InvalidKeyException {
        return new IdentityKey(Curve.decodePoint(key, 0));
    }
}
