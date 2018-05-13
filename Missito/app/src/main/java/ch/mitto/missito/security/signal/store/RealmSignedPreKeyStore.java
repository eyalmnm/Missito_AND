package ch.mitto.missito.security.signal.store;

import android.util.Log;

import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import java.util.ArrayList;
import java.util.List;

import ch.mitto.missito.security.signal.store.model.LocalSignedPreKeyData;

public class RealmSignedPreKeyStore implements SignedPreKeyStore {

    private static final String LOG_TAG = RealmSignedPreKeyStore.class.getSimpleName();

    private RealmDBHelper dbHelper = new RealmDBHelper();

    public RealmSignedPreKeyStore() {
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        LocalSignedPreKeyData data = dbHelper.getLocalSignedPreKeyData(signedPreKeyId);
        try {
            return data.getKeyRecord();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse signed pre key from DB", e);
            return null;
        }
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        List<LocalSignedPreKeyData> results = dbHelper.getLocalSignedPreKeyDataList();
        List<SignedPreKeyRecord> keyRecords = new ArrayList<>(results.size());
        for (LocalSignedPreKeyData data : results) {
            try {
                keyRecords.add(data.getKeyRecord());
            } catch (Exception e) {
                Log.w(LOG_TAG, "Failed to parse signed pre key from DB", e);
            }
        }
        return keyRecords;
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        LocalSignedPreKeyData data = new LocalSignedPreKeyData(signedPreKeyId, record);
        dbHelper.saveLocalSignedPreKeyData(data);
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        return dbHelper.localSignedPreKeyDataExists(signedPreKeyId);
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        dbHelper.removeLocalSignedPreKeyData(signedPreKeyId);
    }
}
