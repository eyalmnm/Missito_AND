package ch.mitto.missito.security.signal;

import android.util.Base64;
import android.util.Log;

import org.whispersystems.libsignal.DecryptionCallback;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.state.SessionStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.mitto.missito.net.broker.model.IncomingMessage;
import ch.mitto.missito.net.model.OutgoingMessage;
import ch.mitto.missito.net.model.Qos;
import ch.mitto.missito.security.signal.model.NewSessionData;
import ch.mitto.missito.security.signal.store.RealmDBHelper;
import ch.mitto.missito.security.signal.store.RealmIdentityKeyStore;
import ch.mitto.missito.security.signal.store.RealmPreKeyStore;
import ch.mitto.missito.security.signal.store.RealmSessionStore;
import ch.mitto.missito.security.signal.store.RealmSignedPreKeyStore;
import ch.mitto.missito.security.signal.store.model.LocalIdentityData;
import ch.mitto.missito.security.signal.store.model.RegistrationIdData;
import ch.mitto.missito.util.PrefsHelper;

import static org.whispersystems.libsignal.protocol.CiphertextMessage.PREKEY_TYPE;

public class SignalProto {

    private static final String LOG_TAG = SignalProto.class.getSimpleName();

    private String userId;

    private SignedPreKeyRecord signedPreKey;
    private int signedPrekeyId = 1;

    private SessionStore sessionStore = new RealmSessionStore();
    private PreKeyStore preKeyStore = new RealmPreKeyStore();
    private SignedPreKeyStore signedPreKeyStore = new RealmSignedPreKeyStore();
    private IdentityKeyStore identityStore = new RealmIdentityKeyStore();

    private Map<SignalProtocolAddress, SessionCipher> ciphers = new HashMap<>();

    private RealmDBHelper dbHelper = new RealmDBHelper();


    public SignalProto(String userId) {
        this.userId = userId;
    }

    public int getRegistrationId() {
        return identityStore.getLocalRegistrationId();
    }

    public String getIdentityPublicKey() {
        byte[] keyData = identityStore.getIdentityKeyPair().getPublicKey().serialize();
        return Base64.encodeToString(keyData, Base64.NO_WRAP);
    }

    public String getSignedPreKeyPublicKey() {
        try {
            byte[] keyData = signedPreKeyStore.loadSignedPreKey(signedPrekeyId).getKeyPair().getPublicKey().serialize();
            return Base64.encodeToString(keyData, Base64.NO_WRAP);
        } catch (InvalidKeyIdException e) {
            Log.e(LOG_TAG, "Can't load signed pre key with id=" + signedPrekeyId);
            return null;
        }
    }

    public String getSignedPreKeySignature() {
        try {
            byte[] sigData = signedPreKeyStore.loadSignedPreKey(signedPrekeyId).getSignature();
            return Base64.encodeToString(sigData, Base64.NO_WRAP);
        } catch (InvalidKeyIdException e) {
            Log.e(LOG_TAG, "Can't load signed pre key with id=" + signedPrekeyId);
            return null;
        }
    }

    public int getSignedPreKeyId() {
        return signedPrekeyId;
    }

    public List<String> getOTPKeys(int startId) {
        List<String> keys = new ArrayList<>();
        try {
            for (int keyId = startId; keyId < dbHelper.getNextOTPKId(); keyId++) {
                byte[] keyData = preKeyStore.loadPreKey(keyId).getKeyPair().getPublicKey().serialize();
                keys.add(Base64.encodeToString(keyData, Base64.NO_WRAP));
            }
        } catch (InvalidKeyIdException e) {
            // TODO: what else can we do here? Skip some keys and return available with new startId?
            return Collections.emptyList();
        }
        return keys;
    }

    public void init() {
        if (identityStore.getLocalRegistrationId() <= 0) {
            // Initial setup - generate keys
            PrefsHelper.saveReportKeysFlag(true);
            IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
            int registrationId = KeyHelper.generateRegistrationId(true);
            dbHelper.saveRegistrationIdData(new RegistrationIdData(registrationId));
            dbHelper.saveLocalIdentityData(new LocalIdentityData(identityKeyPair));

            addPreKeys();
            try {
                signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, signedPrekeyId);
            } catch (InvalidKeyException e) {
                Log.e(LOG_TAG, "generateSignedPreKey call failed", e);
                return;
            }
            signedPreKeyStore.storeSignedPreKey(signedPreKey.getId(), signedPreKey);
        }
    }

    public void addPreKeys() {
        int otpkStartId = dbHelper.getNextOTPKId();
        List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(otpkStartId, 100);
        for (PreKeyRecord preKey : preKeys) {
            preKeyStore.storePreKey(preKey.getId(), preKey);
        }
        otpkStartId += 100;
        dbHelper.saveNextOTPKId(otpkStartId);
    }

    public boolean buildSession(NewSessionData newSessionData, String uid, int deviceId) {
        PreKeyBundle preKeyBundle = createPreKeyBundle(newSessionData, deviceId);
        if (preKeyBundle == null) {
            Log.w(LOG_TAG, "Can't build session: preKeyBundle is null");
            return false;
        }
        SignalProtocolAddress address = new SignalProtocolAddress(uid, deviceId);
        SessionBuilder sessionBuilder = new SessionBuilder(sessionStore, preKeyStore, signedPreKeyStore,
                identityStore, address);

        try {
            sessionBuilder.process(preKeyBundle);
        } catch (InvalidKeyException | UntrustedIdentityException e) {
            Log.e(LOG_TAG, "sessionBuilder.process(preKeyBundle) fail", e);
            return false;
        }
        return true;
    }

    private PreKeyBundle createPreKeyBundle(NewSessionData newSessionData, int deviceId) {
        byte[] otpkSerialized = Base64.decode(newSessionData.otpk.key, Base64.NO_WRAP);
        byte[] signedPKSerialized = Base64.decode(newSessionData.signedPreKey.key, Base64.NO_WRAP);
        byte[] signature = Base64.decode(newSessionData.signedPreKey.keySignature, Base64.NO_WRAP);
        byte[] identityKeySerialized = Base64.decode(newSessionData.identity.identityPK, Base64.NO_WRAP);

        ECPublicKey preKeyPublic;
        ECPublicKey signedPreKeyPublic;
        ECPublicKey identityKeyPublic;
        try {
            preKeyPublic = Curve.decodePoint(otpkSerialized, 0);
            signedPreKeyPublic = Curve.decodePoint(signedPKSerialized, 0);
            identityKeyPublic = Curve.decodePoint(identityKeySerialized, 0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Can't deserialize key", e);
            return null;
        }
        PreKeyBundle preKeyBundle = new PreKeyBundle(newSessionData.identity.regId,
                deviceId,
                newSessionData.otpk.keyId,
                preKeyPublic,
                newSessionData.signedPreKey.keyId,
                signedPreKeyPublic,
                signature,
                new IdentityKey(identityKeyPublic));
        return preKeyBundle;
    }

    public boolean sessionExists(String uid, int deviceId) {
        SignalProtocolAddress address = new SignalProtocolAddress(uid, deviceId);
        return sessionStore.containsSession(address);
    }

    private SessionCipher getCipher(SignalProtocolAddress address) {
        SessionCipher sessionCipher = ciphers.get(address.getName());
        if (sessionCipher == null) {
            sessionCipher = new SessionCipher(sessionStore, preKeyStore,
                    signedPreKeyStore, identityStore, address);
            ciphers.put(address, sessionCipher);
        }
        return sessionCipher;
    }

    public OutgoingMessage encryptMessage(String text, String destUid, int deviceId, Qos qos) {
        SignalProtocolAddress address = new SignalProtocolAddress(destUid, deviceId);

        SessionCipher sessionCipher = getCipher(address);
        CiphertextMessage encryptedMessage;
        try {
            encryptedMessage = sessionCipher.encrypt(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "sessionCipher.encrypt failed", e);
            return null;
        }

        byte[] encryptedBytes = encryptedMessage.serialize();
        String encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);

        return new OutgoingMessage(destUid, deviceId, encryptedBase64,
                encryptedMessage.getType() == PREKEY_TYPE ? "init" : "next", qos);
    }

    private void decryptMessage(byte[] data, SignalProtocolAddress senderAddr,
                                  DecryptionCallback callback) {
        SessionCipher sessionCipher = getCipher(senderAddr);

        SignalMessage message = null;
        try {
            message = new SignalMessage(data);
        } catch (InvalidMessageException | LegacyMessageException e) {
            Log.e(LOG_TAG, "Can't create SignalMessage from received bytes", e);
            return;
        }
        try {
            sessionCipher.decrypt(message, callback);
        } catch (DuplicateMessageException | LegacyMessageException |
                InvalidMessageException | NoSessionException e) {
            Log.e(LOG_TAG, "Decrypt failed", e);
        }
    }

    private void decryptPreKeyMessage(byte[] data, SignalProtocolAddress senderAddr,
                                        DecryptionCallback callback) {
        SessionCipher sessionCipher = getCipher(senderAddr);

        PreKeySignalMessage message = null;
        try {
            message = new PreKeySignalMessage(data);
        } catch (InvalidMessageException | InvalidVersionException e) {
            Log.e(LOG_TAG, "Can't create PreKeySignalMessage from received bytes");
            return;
        }
        try {
            sessionCipher.decrypt(message, callback);
        } catch (DuplicateMessageException | LegacyMessageException | InvalidKeyIdException |
                InvalidMessageException | UntrustedIdentityException | InvalidKeyException e) {
            Log.e(LOG_TAG, "Decrypt failed", e);
        }
    }

    public void decrypt(IncomingMessage incomingMessage, final OnDecryptListener listener) {

        DecryptionCallback callback = new DecryptionCallback() {
            @Override
            public void handlePlaintext(byte[] plaintext) {
                listener.onDecrypt(decodeString(plaintext));
            }

            private String decodeString(byte[] data) {
                try {
                    return data == null ? null : new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(LOG_TAG, "Can't restore string from decrypted bytes", e);
                    return null;
                }
            }
        };

        SignalProtocolAddress address = new SignalProtocolAddress(incomingMessage.senderUid,
                incomingMessage.senderDeviceId);
        byte[] data = Base64.decode(incomingMessage.msg, Base64.NO_WRAP);

        if ("init".equals(incomingMessage.msgType)) {
            decryptPreKeyMessage(data, address, callback);
        } else {
            decryptMessage(data, address, callback);
        }
    }

    public interface OnDecryptListener {
        void onDecrypt(String plaintext);
    }

}
