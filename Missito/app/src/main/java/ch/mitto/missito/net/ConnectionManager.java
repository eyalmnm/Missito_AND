package ch.mitto.missito.net;

import android.ch.mitto.missito.BuildConfig;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.auth0.android.jwt.JWT;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.model.message.ContactData;
import ch.mitto.missito.net.broker.MQTTConnectionManager;
import ch.mitto.missito.net.broker.model.IncomingMessage;
import ch.mitto.missito.net.model.Attachment;
import ch.mitto.missito.net.model.IncomingMessageStatus;
import ch.mitto.missito.net.model.MessageBody;
import ch.mitto.missito.net.model.MessageIdResponse;
import ch.mitto.missito.net.model.OTPCheckResponse;
import ch.mitto.missito.net.model.OutgoingMessage;
import ch.mitto.missito.net.model.OutgoingMessageStatus;
import ch.mitto.missito.net.model.ProfileSettings;
import ch.mitto.missito.net.model.Qos;
import ch.mitto.missito.net.model.UpdateUserNameObj;
import ch.mitto.missito.net.model.UpdatedMessages;
import ch.mitto.missito.net.webapi.APIRequests;
import ch.mitto.missito.security.signal.SignalProto;
import ch.mitto.missito.security.signal.model.IdentityData;
import ch.mitto.missito.security.signal.model.NewSessionData;
import ch.mitto.missito.security.signal.model.OTPKeysData;
import ch.mitto.missito.security.signal.model.SignedPreKeyData;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.MissitoRealmMigration;
import ch.mitto.missito.util.PrefsHelper;
import ch.mitto.missito.util.RealmDBHelper;
import ch.mitto.missito.util.SecureStorageHelper;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;
import io.realm.RealmResults;

import static ch.mitto.missito.fcm.FirebaseInstanceIdMissitoService.CLOUD_TOKEN_UPDATE;
import static ch.mitto.missito.net.ConnectionChangeReceiver.BROADCAST_NETWORK_STATUS;
import static ch.mitto.missito.net.broker.MQTTConnectionManager.MSG_STATUS_UPDATE_EVENT;
import static ch.mitto.missito.net.broker.MQTTConnectionManager.MSG_STATUS_UPDATE_ID;

public class ConnectionManager {

    private static final int SCHEMA_VERSION = 3;
    private static final String LOG_TAG = ConnectionManager.class.getSimpleName();
    public AuthState authState = AuthState.LOGGED_OUT;

    public APIRequests apiRequests;
    public MQTTConnectionManager mqttManager;
    private SignalProto signalProto;
    public String uid;
    public int deviceId;
    public ProfileSettings profileSettings;
    public Realm realm;

    public String backendToken;
    private Context context;

    private BroadcastReceiver cloudTokenUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PrefsHelper.getReportCloudTokenFlag() && backendToken != null) {
                updateCloudToken();
            }
        }
    };

    private BroadcastReceiver sendOutgoingMessagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resendUndeliveredMessages();
            resendIncomingStatuses();
        }
    };

    public void resendUndeliveredMessages() {
        if (realm != null && ConnectionChangeReceiver.isOnline()) {
            RealmResults<MessageRec> outgoingMessages = realm
                    .where(MessageRec.class)
                    .contains("outgoingStatus", OutgoingMessageStatus.OUTGOING.value)
                    .findAll();

            for (MessageRec outgoingMessage : outgoingMessages) {
                Application.app.connectionManager.sendMessage(outgoingMessage, false);
                Log.d(LOG_TAG, String.format("Sending message [%1$s] to %2$s_%3$d",
                        outgoingMessage.localMsgId, outgoingMessage.destUid, outgoingMessage.destDeviceId));
            }
        }
    }

    public void resendIncomingStatuses() {
        if (realm != null && ConnectionChangeReceiver.isOnline()) {
            RealmQuery<MessageRec> incomingMessagesQuery = realm
                    .where(MessageRec.class)
                    .equalTo("incomingStatus", IncomingMessageStatus.SEEN.value);

            RealmResults<MessageRec> incomingMessages = incomingMessagesQuery.findAll();

            for (MessageRec incomingMessage : incomingMessages) {
                Log.d(LOG_TAG, String.format("incomingMessage text: %s", incomingMessage.body));
            }

            if (!incomingMessages.isEmpty()) {
                Application.app.connectionManager.confirmMessagesSeen(incomingMessages);
                Log.d(LOG_TAG, String.format("Confirming messages seen: %s", incomingMessages.toString()));
            }
        }
    }

    public void confirmMessageReceived(final IncomingMessage incomingMessage) {
        apiRequests.setMessagesStatus(Collections.singletonList(incomingMessage.id), null, new Response.Listener<UpdatedMessages>() {
            @Override
            public void onResponse(UpdatedMessages response) {
                if (response.updated.length > 0 && response.updated[0].equals(incomingMessage.id)) {
                    Log.d(LOG_TAG, String.format("Confirm message received [%1$s] ", incomingMessage.id));
                    RealmDBHelper.setIncomingMsgStatus(incomingMessage.id, IncomingMessageStatus.RECEIVED_ACKNOWLEDGED);
                } else {
                    Log.d(LOG_TAG, String.format("Not allowed to confirm message as received [%s]", incomingMessage.id));
                    RealmDBHelper.setIncomingMsgStatus(incomingMessage.id, IncomingMessageStatus.FAILED);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w(LOG_TAG, String.format("Could not confirm message received [%1$s]", incomingMessage.id), error);
            }
        });
    }

    public void confirmMessageSeen(final MessageRec incomingMessage) {
        apiRequests.setMessagesStatus(null, Collections.singletonList(incomingMessage.serverMsgId), new Response.Listener<UpdatedMessages>() {
            @Override
            public void onResponse(UpdatedMessages response) {
                if (response.updated.length > 0 && response.updated[0].equals(incomingMessage.serverMsgId)) {
                    Log.d(LOG_TAG, String.format("Confirm message seen [%1$s] ", incomingMessage.serverMsgId));
                    RealmDBHelper.setIncomingMsgStatus(incomingMessage.serverMsgId, IncomingMessageStatus.SEEN_ACKNOWLEDGED);
                } else {
                    Log.d(LOG_TAG, String.format("Not allowed to confirm message as seen [%s]", incomingMessage.serverMsgId));
                    RealmDBHelper.setIncomingMsgStatus(incomingMessage.serverMsgId, IncomingMessageStatus.FAILED);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w(LOG_TAG, String.format("Could not confirm message seen [%1$s]", incomingMessage.serverMsgId), error);
            }
        });
    }

    public void confirmMessagesSeen(final List<MessageRec> messages) {
        final ArrayList<String> serverMessageIds = new ArrayList<>(messages.size());
        for (MessageRec message : messages) {
            serverMessageIds.add(message.serverMsgId);
            RealmDBHelper.setIncomingMsgStatus(message.serverMsgId, IncomingMessageStatus.SEEN);
        }

        apiRequests.setMessagesStatus(null, serverMessageIds, new Response.Listener<UpdatedMessages>() {
            @Override
            public void onResponse(UpdatedMessages response) {
                List<String> updated = Arrays.asList(response.updated);

                RealmDBHelper.setIncomingMsgStatus(updated, IncomingMessageStatus.SEEN_ACKNOWLEDGED);
                for (MessageRec message : messages) {
                    if (!updated.contains(message.serverMsgId)) {
                        RealmDBHelper.setIncomingMsgStatus(message.serverMsgId, IncomingMessageStatus.FAILED);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w(LOG_TAG, String.format("Could not confirm messages  seen [%1$s]", serverMessageIds), error);
            }
        });
    }

    public ConnectionManager(Context context) {
        Log.d(LOG_TAG, "ConnectionManager constructor");
        this.context = context;
    }

    public void init() {
        apiRequests = new APIRequests(context, BuildConfig.API_ENDPOINT);

        uid = SecureStorageHelper.getUserId();
        deviceId = PrefsHelper.getDeviceId(uid);
        backendToken = SecureStorageHelper.getUserToken();
        if (uid == null || backendToken == null || deviceId <= 0) {
            uid = null;
            backendToken = null;
            deviceId = 0;
        } else {
            onLogin();
        }

        ConnectionChangeReceiver.updateState(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(cloudTokenUpdateReceiver,
                new IntentFilter(CLOUD_TOKEN_UPDATE));
        LocalBroadcastManager.getInstance(context).registerReceiver(sendOutgoingMessagesReceiver,
                new IntentFilter(BROADCAST_NETWORK_STATUS));
    }

    private void setupRealm() {
        byte[] key = SecureStorageHelper.getRealmKey(uid);
        if (key == null) {
            key = new byte[64];
            new SecureRandom().nextBytes(key);
            SecureStorageHelper.saveRealmKey(uid, key);
        }
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(SCHEMA_VERSION)
                .name(uid)
                .migration(new MissitoRealmMigration() )
                .encryptionKey(key)
                .build();
        Realm.setDefaultConfiguration(config);
        realm = Realm.getDefaultInstance();
    }


    private void onLogin() {
        Log.d(LOG_TAG, "ConnectionManager.onLogin");
        authState = AuthState.LOGGED_IN;
        mqttManager = new MQTTConnectionManager(context, uid, deviceId);
        setupRealm();
        signalProto = new SignalProto(uid);
        signalProto.init();
        apiRequests.setBackendToken(backendToken);
        apiRequests.setBackendToken(backendToken);
        Application.app.contacts.loadMissitoContacts();
        getProfileSettings();

        if (PrefsHelper.getReportKeysFlag()) {
            reportAllKeys();
        }

        if (PrefsHelper.getReportCloudTokenFlag()) {
            updateCloudToken();
        }
        synchronizeContacts();
    }

    private void getProfileSettings() {
        apiRequests.getProfileSettings(new Response.Listener<ProfileSettings>() {
            @Override
            public void onResponse(ProfileSettings response) {
                profileSettings = response;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Get profile ERROR", error);
            }
        });
    }

    public void synchronizeContacts() {
        if (Helper.canReadContacts(Application.app)) {
            Application.app.contacts.loadSystemContacts();
            ArrayList<ContactData> allContacts = new ArrayList<>();
            for (Map.Entry<String, MissitoContact> entry : Application.app.contacts.systemContacts.entrySet()) {
                MissitoContact missitoContact = entry.getValue();
                allContacts.add(new ContactData(missitoContact.phone, missitoContact.firstName, missitoContact.lastName));
            }
            ArrayList<ContactData> savedContacts = RealmDBHelper.getAllContacts();
            checkAndAddNewContacts(savedContacts, allContacts);
            mqttManager.connect(uid + "_" + deviceId, backendToken);
        }
    }

    private void checkAndAddNewContacts(List<ContactData> storedContacts, List<ContactData> allContacts) {

        allContacts.removeAll(storedContacts);
        if (allContacts.size() != 0) {
            addContacts(allContacts);
        }
    }

    private void addContacts(final List<ContactData> contacts) {
        apiRequests.addContacts(contacts, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                RealmDBHelper.addAllContacts(contacts);
//                mqttManager.subscribeToStatus(); //see missito-backend #32
                Log.d(LOG_TAG, "Contacts added successfully");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Could not add contacts", error);
            }
        });
    }

    public void updateUserName(String userName, String phoneNumber) {
        if (userName == null || userName.isEmpty()) {
            return;
        }
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return;
        }
        UpdateUserNameObj updateUserNameObj = new UpdateUserNameObj(userName, phoneNumber);
        apiRequests.updateUserName(updateUserNameObj, new Response.Listener<Void>() {
            @Override
            public void onResponse(Void response) {
                Log.d("ConnectionManager", "updateUserName: " + response);
            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error occurs while trying to updateUserName", error);
            }
        });
    }

    public void getUserName(final GetUserNameHandler completion) {
        apiRequests.getUserName(new Response.Listener<UpdateUserNameObj>() {
            @Override
            public void onResponse(UpdateUserNameObj response) {
                String name = response.name;
                if (null != completion) {
                    completion.onResult(name);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error occurs while trying to getUserName", error);
                completion.onResult("");
            }
        });
    }


    public void checkOTP(final String phone, String token, String code, final OTPCheckHandler completion) {
        apiRequests.checkOTP(token, code, PrefsHelper.getDeviceId(phone), new Response.Listener<OTPCheckResponse>() {
            @Override
            public void onResponse(OTPCheckResponse response) {
                backendToken = response.token;
                String subjectField[] = new JWT(backendToken).getSubject().split("_");
                if (subjectField.length != 2) {
                    authState = AuthState.LOGGED_OUT;
                    // TODO customize this error
                    completion.onResult(new VolleyError());
                }
                deviceId = Integer.parseInt(subjectField[1]);
                PrefsHelper.setDeviceId(phone, deviceId);
                uid = subjectField[0];
                SecureStorageHelper.saveUserAuthData(uid, backendToken);
                // TODO: we should do this after MQTT connection. Add another state?
                onLogin();
                completion.onResult(null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                authState = AuthState.LOGGED_OUT;
                completion.onResult(error);
            }
        });
    }

    public void reportAllKeys() {
        IdentityData identityData = new IdentityData(signalProto.getRegistrationId(), signalProto.getIdentityPublicKey());
        apiRequests.storeIdentity(identityData, new Response.Listener<Void>() {
            @Override
            public void onResponse(Void response) {
                Log.d(LOG_TAG, "storeIdentity: ok");
                reportSignedPreKey();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "storeIdentity error", error);
            }
        });
    }

    public void updateOtpKeys() {
        signalProto.addPreKeys();
        reportSignedPreKey();
    }

    private void reportSignedPreKey() {
        SignedPreKeyData signedPreKeyData = new SignedPreKeyData(signalProto.getSignedPreKeyId(),
                signalProto.getSignedPreKeyPublicKey(),
                signalProto.getSignedPreKeySignature());
        apiRequests.storeSignedPreKey(signedPreKeyData, new Response.Listener<Void>() {
            @Override
            public void onResponse(Void response) {
                Log.d(LOG_TAG, "storeSignedPreKey: ok");
                reportOTPKeys();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "storeSignedPreKey error", error);
            }
        });
    }

    private void reportOTPKeys() {
        final int startId = PrefsHelper.getLastReportedOtpkId() + 1;
        final List<String> keyList = signalProto.getOTPKeys(startId);
        String[] keys = new String[keyList.size()];
        OTPKeysData otpKeysData = new OTPKeysData(startId, keyList.toArray(keys));

        apiRequests.storeOTPKeys(otpKeysData, new Response.Listener<Void>() {
            @Override
            public void onResponse(Void response) {
                Log.d(LOG_TAG, "storeOTPKeys: ok");
                PrefsHelper.saveLastReportedOtpkId(startId + keyList.size() - 1);
                // TODO: this makes sense only for first reportOTPKeys call, right?
                PrefsHelper.saveReportKeysFlag(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "storeOTPKeys error", error);
            }
        });
    }

    public void sendMessage(final MessageRec message, final boolean typing) {
        if (signalProto.sessionExists(message.destUid, message.destDeviceId)) {
            encryptAndSend(message, typing);
        } else {
            apiRequests.requestNewSession(message.destUid, message.destDeviceId,
                    new Response.Listener<NewSessionData>() {
                @Override
                public void onResponse(NewSessionData newSessionData) {
                    if (signalProto.buildSession(newSessionData, message.destUid, message.destDeviceId)) {
                        encryptAndSend(message, typing);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(LOG_TAG, "requestNewSession failed", error);
                }
            });
        }
    }

    public void updateCloudToken() {
        apiRequests.updateCloudToken(new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                PrefsHelper.setReportCloudTokenFlag(false);
                Log.d(LOG_TAG, "Cloud token updated Response: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "Cloud token update Error: ", error);
            }
        });
    }

    private void encryptAndSend(final MessageRec message, final boolean typing) {
        MessageBody body = new MessageBody();
        Qos qos = Qos.MANDATORY;
        body.text = message.body;
        body.uniqueId = message.uniqueId;
        if (body.uniqueId == null) {
            // This should happen for "typing" messages only
            body.uniqueId = UUID.randomUUID().toString();
        }
        body.attach = Attachment.make(message.attach);
        if (typing) {
            body.typing = "on";
            body.typingStart = System.currentTimeMillis();
            qos = Qos.TRANSIENT;
        }

        String json = new Gson().toJson(body, MessageBody.class);

        OutgoingMessage outgoingMessage = signalProto.encryptMessage(json, message.destUid, message.destDeviceId, qos);
        apiRequests.sendMessage(outgoingMessage, new Response.Listener<MessageIdResponse>() {
            @Override
            public void onResponse(final MessageIdResponse response) {
                Log.d(LOG_TAG, "Response: " + response);
                if (!typing) {
                    RealmDBHelper.setOutgoingMsgStatus(message.localMsgId, response.messageId, OutgoingMessageStatus.SENT);
                    Intent intent = new Intent(MSG_STATUS_UPDATE_EVENT);
                    intent.putExtra(MSG_STATUS_UPDATE_ID, message.localMsgId);
                    LocalBroadcastManager.getInstance(Application.app).sendBroadcast(intent);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error:", error);
            }
        });
    }

    public void decrypt(IncomingMessage incomingMessage, SignalProto.OnDecryptListener listener) {
        signalProto.decrypt(incomingMessage, listener);
    }

    public void connectToMqtt() {
        if (mqttManager != null && !mqttManager.isConnected()) {
            mqttManager.connect(uid + "_" + deviceId, backendToken);
        }
    }

    public void disconnectFromMqtt() {
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }

    public void logout() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(cloudTokenUpdateReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(sendOutgoingMessagesReceiver);
        deleteFCMToken();
        if (authState != AuthState.LOGGED_OUT) {
            apiRequests.deleteCloudToken(new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(LOG_TAG, "Cloud token deleted successfully");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(LOG_TAG, "Could not delete cloud token", error);
                }
            });
        }
        SecureStorageHelper.removeUserAuthData();
        authState = AuthState.LOGGED_OUT;
        if (mqttManager != null) {
            mqttManager.disconnect();
            mqttManager = null;
        }
        if (realm != null) {
            realm.close();
        }
        signalProto = null;
        apiRequests.setBackendToken(null);
        Realm.removeDefaultConfiguration();
        PrefsHelper.removeCountryCode();
        PrefsHelper.removeInvitationDate();
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private void deleteFCMToken() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    FirebaseInstanceId.getInstance().deleteInstanceId();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Could not delete firebase instance ID", e);
                }
                return null;
            }
        }.execute();
    }

    public enum AuthState {
        LOGGED_OUT, LOGGING_IN, LOGGED_IN
    }

    public interface OTPCheckHandler {
        void onResult(VolleyError error);
    }

    public interface GetUserNameHandler {
        void onResult(String name);
    }
}
