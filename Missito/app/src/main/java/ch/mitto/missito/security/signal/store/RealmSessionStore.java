package ch.mitto.missito.security.signal.store;

import android.util.Log;

import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;

import java.io.IOException;
import java.util.List;

import ch.mitto.missito.security.signal.store.model.SessionData;

public class RealmSessionStore implements SessionStore {

    private static final String LOG_TAG = RealmSessionStore.class.getSimpleName();

    private RealmDBHelper dbHelper = new RealmDBHelper();

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        SessionData sessionData = dbHelper.getSessionData(
                SessionData.calcId(address));
        if (sessionData == null) {
            return new SessionRecord();
        }
        try {
            return sessionData.getSessionRecord();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't parse session record for " + address, e);
            return new SessionRecord();
        }
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        return dbHelper.getSubDeviceSessions(name);
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        dbHelper.saveSessionData(new SessionData(address, record));
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        return dbHelper.sessionDataExists(SessionData.calcId(address));
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        dbHelper.removeSessionData(SessionData.calcId(address));
    }

    @Override
    public void deleteAllSessions(String name) {
        dbHelper.removeAllSessionData(name);
    }

}
