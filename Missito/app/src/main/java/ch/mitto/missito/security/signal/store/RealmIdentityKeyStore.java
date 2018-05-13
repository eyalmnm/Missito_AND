package ch.mitto.missito.security.signal.store;

import android.util.Log;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;

import ch.mitto.missito.security.signal.store.model.LocalIdentityData;
import ch.mitto.missito.security.signal.store.model.RegistrationIdData;
import ch.mitto.missito.security.signal.store.model.RemoteIdentityData;


public class RealmIdentityKeyStore implements IdentityKeyStore {

    private static final String LOG_TAG = RealmIdentityKeyStore.class.getSimpleName();

    private RealmDBHelper dbHelper = new RealmDBHelper();

    public RealmIdentityKeyStore() {
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        LocalIdentityData identityData = dbHelper.getLocalIdentityData();
        if (identityData == null) {
            return null;
        }
        try {
            return identityData.getKeyPair();
        } catch (InvalidKeyException e) {
            Log.e(LOG_TAG, "Can't parse identity key pair from DB", e);
            return null;
        }
    }

    @Override
    public int getLocalRegistrationId() {
        RegistrationIdData data = dbHelper.getRegistrationIdData();
        return data == null ? 0 : data.getRegistrationId();
    }

    @Override
    public void saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        dbHelper.saveRemoteIdentityData(new RemoteIdentityData(address, identityKey));
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        // Note: see comments for isTrustedIdentity in IdentityKeyStore protocol
        RemoteIdentityData data = dbHelper.getRemoteIdentityData(
                RemoteIdentityData.calcId(address.getName(), address.getDeviceId()));
        if (data == null) {
            return true;
        }
        IdentityKey stored;
        try {
            stored = data.getIdentityKey();
        } catch (InvalidKeyException e) {
            Log.e(LOG_TAG, "Can't parse remote identity key", e);
            // Should we remove the entry and return true here?
            return false;
        }
        return identityKey.equals(stored);

    }

}
