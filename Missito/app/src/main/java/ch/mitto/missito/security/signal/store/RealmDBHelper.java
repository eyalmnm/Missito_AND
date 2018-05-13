package ch.mitto.missito.security.signal.store;

import java.util.ArrayList;
import java.util.List;

import ch.mitto.missito.Application;
import ch.mitto.missito.security.signal.store.model.LocalIdentityData;
import ch.mitto.missito.security.signal.store.model.LocalSignedPreKeyData;
import ch.mitto.missito.security.signal.store.model.NextOTPKIdData;
import ch.mitto.missito.security.signal.store.model.PreKeyData;
import ch.mitto.missito.security.signal.store.model.RegistrationIdData;
import ch.mitto.missito.security.signal.store.model.RemoteIdentityData;
import ch.mitto.missito.security.signal.store.model.SessionData;
import io.realm.Realm;
import io.realm.RealmResults;

public class RealmDBHelper {

    public RealmDBHelper() {
    }

    public void saveLocalIdentityData(final LocalIdentityData identityData) {

        // TODO: how to handle errors?
        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(identityData);
            }
        });
    }

    public LocalIdentityData getLocalIdentityData() {
        Realm realm = Application.app.connectionManager.realm;
        LocalIdentityData result = realm.where(LocalIdentityData.class).findFirst();
        return result;
    }

    public void saveLocalSignedPreKeyData(final LocalSignedPreKeyData preKeyData) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(preKeyData);
            }
        });
    }

    public LocalSignedPreKeyData getLocalSignedPreKeyData(int id) {
        Realm realm = Application.app.connectionManager.realm;
        LocalSignedPreKeyData result = realm.where(LocalSignedPreKeyData.class)
                .equalTo("id", id).findFirst();
        return result;
    }

    public List<LocalSignedPreKeyData> getLocalSignedPreKeyDataList() {
        Realm realm = Application.app.connectionManager.realm;
        List<LocalSignedPreKeyData> result = realm.where(LocalSignedPreKeyData.class).findAll();
        return result;
    }

    public boolean localSignedPreKeyDataExists(int id) {
        Realm realm = Application.app.connectionManager.realm;
        boolean result = realm.where(LocalSignedPreKeyData.class)
                .equalTo("id", id).findFirst() != null;
        return result;
    }

    public void removeLocalSignedPreKeyData(final int id) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(LocalSignedPreKeyData.class)
                        .equalTo("id", id).findAll().deleteAllFromRealm();
            }
        });
    }

    public void saveRegistrationIdData(final RegistrationIdData data) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(data);
            }
        });
    }

    public RegistrationIdData getRegistrationIdData() {
        Realm realm = Application.app.connectionManager.realm;
        RegistrationIdData result = realm.where(RegistrationIdData.class).findFirst();
        return result;
    }

    public RemoteIdentityData getRemoteIdentityData(String id) {
        Realm realm = Application.app.connectionManager.realm;
        RemoteIdentityData result = realm.where(RemoteIdentityData.class)
                .equalTo("id", id).findFirst();
        return result;
    }

    public void saveRemoteIdentityData(final RemoteIdentityData data) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(data);
            }
        });
    }

    public PreKeyData getPreKeyData(int id) {
        Realm realm = Application.app.connectionManager.realm;
        PreKeyData result = realm.where(PreKeyData.class)
                .equalTo("id", id).findFirst();
        return result;
    }

    public void savePreKeyData(final PreKeyData data) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(data);
            }
        });
    }

    public void removePreKeyData(final int id) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(PreKeyData.class)
                        .equalTo("id", id).findAll().deleteAllFromRealm();
            }
        });
    }


    public SessionData getSessionData(String id) {
        Realm realm = Application.app.connectionManager.realm;
        SessionData result = realm.where(SessionData.class)
                .equalTo("id", id).findFirst();
        return result;
    }

    public void saveSessionData(final SessionData data) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(data);
            }
        });
    }

    public void removeSessionData(final String id) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(SessionData.class)
                        .equalTo("id", id).findAll().deleteAllFromRealm();
            }
        });

    }

    public List<Integer> getSubDeviceSessions(String name) {
        Realm realm = Application.app.connectionManager.realm;
        List<SessionData> sessions = realm.where(SessionData.class)
                .equalTo("name", name).findAll();
        List<Integer> deviceList = new ArrayList<>(sessions.size());
        for (SessionData data : sessions) {
            deviceList.add(data.getDeviceId());
        }
        return deviceList;
    }

    public void removeAllSessionData(final String name) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(SessionData.class)
                        .equalTo("name", name).findAll().deleteAllFromRealm();
            }
        });
    }

    public boolean sessionDataExists(String id) {
        Realm realm = Application.app.connectionManager.realm;
        boolean result = realm.where(SessionData.class)
                .equalTo("id", id).findFirst() != null;
        return result;
    }

    public int getNextOTPKId() {
        Realm realm = Application.app.connectionManager.realm;
        NextOTPKIdData result = realm.where(NextOTPKIdData.class)
                .findFirst();
        return result == null ? 1 : result.getNextId();
    }

    public void saveNextOTPKId(final int nextId) {

        Realm realm = Application.app.connectionManager.realm;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(new NextOTPKIdData(nextId));
            }
        });

    }

}