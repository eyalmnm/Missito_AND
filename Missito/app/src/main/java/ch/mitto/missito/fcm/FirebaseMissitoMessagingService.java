package ch.mitto.missito.fcm;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class FirebaseMissitoMessagingService extends FirebaseMessagingService {

    private final static String TAG = FirebaseMissitoMessagingService.class.getSimpleName();

    public FirebaseMissitoMessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received message from: " + remoteMessage.getFrom());
    }
}
