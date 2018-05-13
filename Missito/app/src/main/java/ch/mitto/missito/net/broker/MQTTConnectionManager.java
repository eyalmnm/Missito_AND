package ch.mitto.missito.net.broker;

import android.ch.mitto.missito.BuildConfig;
import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.ContactRec;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.net.TLSSocketFactory;
import ch.mitto.missito.net.broker.model.ContactEntry;
import ch.mitto.missito.net.broker.model.ContactsStatusModel;
import ch.mitto.missito.net.broker.model.IncomingMessage;
import ch.mitto.missito.net.broker.model.KeyStoreMessage;
import ch.mitto.missito.net.model.MessageBody;
import ch.mitto.missito.net.model.MessageStatus;
import ch.mitto.missito.security.signal.SignalProto;
import ch.mitto.missito.services.DownloadHelper;
import ch.mitto.missito.ui.MainActivity;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.NotificationHelper;
import ch.mitto.missito.util.PrefsHelper;
import ch.mitto.missito.util.RealmDBHelper;

public class MQTTConnectionManager {

    private static final String LOG_TAG = MQTTConnectionManager.class.getSimpleName();

    public static final String MSG_STATUS_UPDATE_EVENT = "msg_status_update";
    public static final String MSG_STATUS_UPDATE_STATUS = "msg_status_update_status";
    public static final String MSG_STATUS_UPDATE_ID = "msg_status_update_id";
    public static final String NEW_STATUS_UPDATE_EVENT = "status_key_evt";
    public static final String MQTT_CONNECTION_STATE_CHANGED = "mqtt_con_state_changed";
    public static final String KEY_MQTT_CONNECTION_STATE = "mqtt_con_state_key";

    private MqttAndroidClient mqttAndroidClient;

    private final String serverUri = BuildConfig.BROKER_PROTOCOL + "://" + BuildConfig.BROKER_HOST + ":" + BuildConfig.BROKER_PORT;

    private HashMap<String, IMqttMessageListener> subscriptions = new HashMap<>();


    public MQTTConnectionManager(final Context context, final String uid, int deviceId) {
        final String clientId = Helper.removePlus(uid) + "_" + deviceId;

        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);

        subscriptions.put("otp_keys/" + clientId, new KeyStoreSubscription());
        subscriptions.put("message/" + clientId, new IncomingMessageSubscription());
        subscriptions.put("status/" + clientId, new StatusSubscription(lbm));
        subscriptions.put("messageStatus/" + clientId, new MessageStatusSubscription(lbm));

        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId,
                new MemoryPersistence(), MqttAndroidClient.Ack.AUTO_ACK);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.i(LOG_TAG, (reconnect ? "Reconnected" : "Connected") + " to: " + serverURI);
                subscribe();
                Intent intent = new Intent(MQTT_CONNECTION_STATE_CHANGED);
                intent.putExtra(KEY_MQTT_CONNECTION_STATE, context.getResources().getString(R.string.mqtt_connected));
                lbm.sendBroadcast(intent);
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.w(LOG_TAG, "The Connection was lost.");
                Intent intent = new Intent(MQTT_CONNECTION_STATE_CHANGED);
                intent.putExtra(KEY_MQTT_CONNECTION_STATE, context.getResources().getString(R.string.mqtt_disconnected));
                lbm.sendBroadcast(intent);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(LOG_TAG, "Incoming message: " + new String(message.getPayload()));
                IMqttMessageListener handler = subscriptions.get(topic);
                if (handler != null) {
                    handler.messageArrived(topic, message);
                } else {
                    Log.w(LOG_TAG, String.format("Orphan message to topic [%1$s]", topic));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public String getClientId() {
        return mqttAndroidClient.getClientId();
    }

    public String getServerURI() {
        return mqttAndroidClient.getServerURI();
    }

    public boolean isConnected() {
        boolean isConnected = false;
        try {
            isConnected = mqttAndroidClient != null && mqttAndroidClient.isConnected();
        } catch (NullPointerException | IllegalArgumentException e) {
            Log.e(LOG_TAG, "Error MQTT isConnected:", e);
        }
        return isConnected;
    }

    private void subscribe() {
        try {
            final int size = subscriptions.size();
            mqttAndroidClient.subscribe(
                    subscriptions.keySet().toArray(new String[size]),
                    new int[size],
                    null,
                    new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.d(LOG_TAG, "MQTT topics subscribed");
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.w(LOG_TAG, "MQTT topics subscription failed", exception);
                        }
                    }

            );
        } catch (MqttException e) {
            Log.w(LOG_TAG, "Could not perform MQTT subscription", e);
        }
    }

    public void connect(String login, String password) {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            try {
                mqttConnectOptions.setSocketFactory(new TLSSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException except) {
                Log.e (LOG_TAG, "Catched error while trying to set SSL Socket... ", except);
            }
        }
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(login);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(LOG_TAG, "Failed to connect to: " + serverUri, exception);
                }
            });
        } catch (MqttException | NullPointerException ex) {
            Log.w(LOG_TAG, "Could not initialize MQTT connection", ex);
        }
    }

    public void disconnect() {
        try {
            mqttAndroidClient.disconnect();
        } catch (MqttException | NullPointerException e) {
            Log.e(LOG_TAG, "Error MQTT disconnect", e);
        }
    }

    /**
     * Decodes a JSONObject from a received MQTT message
     */
    private static abstract class JSONMessageSubscription implements IMqttMessageListener {

        abstract void onMessage(JSONObject json);

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                onMessage(new JSONObject(new String(message.getPayload(), "UTF-8")));
            } catch (JSONException | UnsupportedEncodingException e) {
                Log.w(LOG_TAG, "Can't decode received MQTT message to JSON", e);
            }
        }
    }

    /**
     * Subscription to contact status updates. Receives updates and broadcasts
     * them to given {@link LocalBroadcastManager}
     */
    private static class StatusSubscription extends JSONMessageSubscription {

        private LocalBroadcastManager broadcaster;

        StatusSubscription(LocalBroadcastManager lbm) {
            this.broadcaster = lbm;
        }

        @Override
        public void onMessage(JSONObject json) {
            ContactsStatusModel data = ContactsStatusModel.fromJson(json.toString());

            if (TextUtils.equals(data.msgType, "logout")) {
                Application.app.connectionManager.logout();
                Intent intent = new Intent(Application.app, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                Application.app.startActivity(intent);
                return;
            }

            ArrayList<ContactEntry> newContacts = Helper.getNewContacts(data);

            if (!newContacts.isEmpty()) {
                final Date availableSince = PrefsHelper.getFirstContactStatusUpdateFlag() ?
                        new Date(System.currentTimeMillis() - ContactRec.NEW_CONTACT_TIME_MS) :
                        new Date();
                Application.app.contacts.addMissitoContacts(newContacts, availableSince);
                RealmDBHelper.addMissitoContacts(newContacts, availableSince);
            }
            if (data != null) {
                Application.app.contacts.updateStatus(data);
                Intent intent = new Intent(NEW_STATUS_UPDATE_EVENT);
                broadcaster.sendBroadcast(intent);
            } else {
                Log.w(LOG_TAG, "Unrecognized status update " + json.toString());
            }
            PrefsHelper.setFirstContactStatusUpdateFlag(false);
        }
    }

    /**
     * Subscription to incoming messages. Receives messages and broadcasts
     * them to given {@link LocalBroadcastManager}
     */
    private static class IncomingMessageSubscription extends JSONMessageSubscription {

        private static final long TYPING_TIMEOUT = 3000;        // ms

        @Override
        public void onMessage(JSONObject json) {
            IncomingMessage data = new Gson().fromJson(json.toString(), IncomingMessage.class);
            if (data != null) {
                data.senderUid = Helper.addPlus(data.senderUid);
                processNewMessage(data);
            } else {
                Log.w(LOG_TAG, "Unrecognized message update " + json.toString());
            }
        }

        private void processNewMessage(final IncomingMessage incomingMessage) {

            Application.app.connectionManager.decrypt(incomingMessage, new SignalProto.OnDecryptListener() {
                @Override
                public void onDecrypt(String plaintext) {
                    if (plaintext == null) {
                        Log.w(LOG_TAG, String.format("Could not decrypt/decode message [%s]", incomingMessage.id));
                        return;
                    }

                    MessageBody messageBody = new Gson().fromJson(plaintext, MessageBody.class);

                    if (messageBody.typing == null) {
                        RealmDBHelper.saveIncomingMessage(messageBody, incomingMessage);
                        MessageRec messageRec = RealmDBHelper.getMessageByServerId(incomingMessage.id);
                        if (!messageRec.attach.isEmpty() ) {
                            DownloadHelper.downloadAttachment(messageRec, true);
                        }
                    } else if (System.currentTimeMillis() - messageBody.typingStart < TYPING_TIMEOUT) {
                        NotificationHelper.notifyNewTypingNotification(incomingMessage);
                    }
                }
            });
            Application.app.connectionManager.confirmMessageReceived(incomingMessage);

        }

    }

    /**
     * Subscription to keystore events.
     * When a "low OTPK count" event is received - post new set of keys to server.
     */
    private static class KeyStoreSubscription extends JSONMessageSubscription {

        @Override
        public void onMessage(JSONObject json) {
            KeyStoreMessage data = new Gson().fromJson(json.toString(), KeyStoreMessage.class);
            if (data == null) {
                Log.w(LOG_TAG, "Unrecognized keystore message " + json.toString());
            } else if (data.isOTPKLow()) {

                // Running in Main Thread because Realm objects can only be accessed on the thread they where created
                Handler mainHandler = new Handler(Application.app.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Application.app.connectionManager.updateOtpKeys();
                    }
                };
                mainHandler.post(myRunnable);
            }
        }
    }

    /**
     * Subscription to message status updates. Receives updates and broadcasts
     * them to given {@link LocalBroadcastManager}
     */
    private static class MessageStatusSubscription extends JSONMessageSubscription {

        private final LocalBroadcastManager broadcaster;

        MessageStatusSubscription(LocalBroadcastManager lbm) {
            this.broadcaster = lbm;
        }

        @Override
        public void onMessage(JSONObject json) {
            MessageStatus messageStatus = new Gson().fromJson(json.toString(), MessageStatus.class);
            String localMsgId = RealmDBHelper.setOutgoingMsgStatus(messageStatus.msgId, messageStatus.status);
            if (localMsgId != null) {
                Intent intent = new Intent(MSG_STATUS_UPDATE_EVENT);
                intent.putExtra(MSG_STATUS_UPDATE_STATUS, messageStatus.status);
                intent.putExtra(MSG_STATUS_UPDATE_ID, localMsgId);
                broadcaster.sendBroadcast(intent);
            }
        }
    }
}
