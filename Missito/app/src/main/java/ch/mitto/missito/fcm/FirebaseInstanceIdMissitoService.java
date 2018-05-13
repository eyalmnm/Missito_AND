package ch.mitto.missito.fcm;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import ch.mitto.missito.Application;
import ch.mitto.missito.util.PrefsHelper;

public class FirebaseInstanceIdMissitoService extends FirebaseInstanceIdService {

    private static final String TAG = FirebaseInstanceIdMissitoService.class.getSimpleName();
    public static final String CLOUD_TOKEN_UPDATE = "cloud_token_update";

    @Override
    public void onTokenRefresh() {
        Log.d(TAG, "Cloud token received");
        PrefsHelper.setReportCloudTokenFlag(true);
        notifyTokenUpdated();
    }

    private void notifyTokenUpdated() {
        Intent intent = new Intent(CLOUD_TOKEN_UPDATE);
        LocalBroadcastManager.getInstance(Application.app).sendBroadcast(intent);
    }
}