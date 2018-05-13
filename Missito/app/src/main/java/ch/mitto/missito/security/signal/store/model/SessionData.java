package ch.mitto.missito.security.signal.store.model;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionRecord;

import java.io.IOException;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class SessionData extends RealmObject {

    @PrimaryKey
    private String id;

    @Required
    private String name;

    private int deviceId;

    @Required
    private byte[] session;

    public SessionData(SignalProtocolAddress address, SessionRecord sessionRecord) {
        session = sessionRecord.serialize();
        name = address.getName();
        deviceId = address.getDeviceId();
        id = calcId(name, deviceId);
    }

    public SessionData() {
    }

    private static String calcId(String name, int deviceId) {
        return name + "_" + deviceId;
    }

    public static String calcId(SignalProtocolAddress address) {
        return calcId(address.getName(), address.getDeviceId());
    }

    public SessionRecord getSessionRecord() throws IOException {
        return new SessionRecord(session);
    }

    public String getName() {
        return name;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public SignalProtocolAddress getSignalProtocolAddress() {
        return new SignalProtocolAddress(name, deviceId);
    }
}
