package ch.mitto.missito.security.signal.store;

import android.util.Log;

import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;

import java.io.IOException;

import ch.mitto.missito.security.signal.store.model.PreKeyData;

public class RealmPreKeyStore implements PreKeyStore {

    private static final String LOG_TAG = RealmPreKeyStore.class.getSimpleName();

    private RealmDBHelper dbHelper = new RealmDBHelper();

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        PreKeyData data = dbHelper.getPreKeyData(preKeyId);
        if (data == null) {
            throw  new InvalidKeyIdException("Can't find pre key for id=" + preKeyId);
        }
        try {
            return data.getPreKeyRecord();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't parse pre key record for key id=" + preKeyId);
            throw  new InvalidKeyIdException("Can't parse pre key for id=" + preKeyId);
        }
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        dbHelper.savePreKeyData(new PreKeyData(preKeyId, record));
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return dbHelper.getPreKeyData(preKeyId) != null;
    }

    @Override
    public void removePreKey(int preKeyId) {
        dbHelper.removePreKeyData(preKeyId);
    }
}
