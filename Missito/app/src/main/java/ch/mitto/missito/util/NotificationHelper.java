package ch.mitto.missito.util;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.net.broker.model.IncomingMessage;

/**
 * Created by usr1 on 12/20/17.
 */

public class NotificationHelper {


    public static final String NEW_TYPING_NOTIFICATION = "notification_event";
    public static final String NEW_TYPING_UID_KEY = "uid";
    public static final String NEW_TYPING_DEV_ID_KEY = "dev_id";
    public static final String INCOMING_MESSAGE_SAVED = "message_saved_event";
    public static final String KEY_MESSAGE = "message_saved_key";
    public static final String LOADED_AND_DECRYPTED_IMAGE_EVENT = "loaded_and_decrypted_event";
    public static final String SUCCESFULLY_LOADED_AND_DECRYPTED_KEY = "succesfully_loaded_and_decrypted_key";
    public static final String MESSAGE_REC_KEY = "message_rec_key";

    public static void notifyMessageSaved(MessageRec message) {
        Intent intent = new Intent(INCOMING_MESSAGE_SAVED);
        intent.putExtra(KEY_MESSAGE, message);
        LocalBroadcastManager.getInstance(Application.app).sendBroadcast(intent);
    }

    public static void notifyNewTypingNotification(IncomingMessage message) {
        Intent intent = new Intent(NEW_TYPING_NOTIFICATION);
        intent.putExtra(NEW_TYPING_UID_KEY, message.senderUid);
        intent.putExtra(NEW_TYPING_DEV_ID_KEY, message.senderDeviceId);
        LocalBroadcastManager.getInstance(Application.app).sendBroadcast(intent);
    }

    public static void notifyDownloadedAndDecryptedImage(MessageRec messageRec, boolean succesfully) {
        Intent intent = new Intent(LOADED_AND_DECRYPTED_IMAGE_EVENT);
        intent.putExtra(MESSAGE_REC_KEY, messageRec);
        intent.putExtra(SUCCESFULLY_LOADED_AND_DECRYPTED_KEY, succesfully);
        LocalBroadcastManager.getInstance(Application.app).sendBroadcast(intent);
    }

}
