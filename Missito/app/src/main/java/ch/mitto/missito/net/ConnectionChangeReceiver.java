package ch.mitto.missito.net;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = ConnectionChangeReceiver.class.getSimpleName();
    private static boolean isOnline;

    public final static String BROADCAST_NETWORK_STATUS = "network_status";
    public final static String CONNECTION_STATUS_UPDATE = "connection_status_update";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (updateState(context)) {
            Log.d(LOG_TAG, "Internet connection restored");
        } else {
            Log.d(LOG_TAG, "Internet connection lost");
        }

        LocalBroadcastManager
                .getInstance(context)
                .sendBroadcast(new Intent(BROADCAST_NETWORK_STATUS).putExtra(CONNECTION_STATUS_UPDATE, isOnline));
    }

    public static boolean updateState(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager
                .getActiveNetworkInfo();

        return (isOnline = (networkInfo != null && networkInfo.isConnected()));
    }

    public static boolean isOnline() {
        return isOnline;
    }
}
