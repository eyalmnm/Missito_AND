package ch.mitto.missito;

import android.ch.mitto.missito.R;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.NetworkType;

import ch.mitto.missito.net.ConnectionManager;
import ch.mitto.missito.security.AppStateChangeReceiver;
import ch.mitto.missito.security.StateListener;
import ch.mitto.missito.services.Contacts;
import ch.mitto.missito.services.ServiceFetchListener;
import io.realm.Realm;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Application class for holding some common instances
 */
public class Application extends MultiDexApplication implements StateListener {

    public ConnectionManager connectionManager;
    public static Application app;
    public Contacts contacts;
    public static Fetch WiFiFetch;
    public static Fetch allNetworkFetch;
    public AppStateChangeReceiver stateListener;
    private static final String LOG_TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Realm.init(this);
        contacts = new Contacts();
        stateListener = new AppStateChangeReceiver(this);
        setListener(this);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setFontAttrId(R.attr.fontPath)
                .build());
        if (connectionManager == null) {
            initConnectionManager();
            Log.d(LOG_TAG, "Connection Manager was initialized.");
        }
        initFetch();
    }

    @Override
    public void onAppEnterForeground() {
        Application.app.connectionManager.connectToMqtt();
    }

    @Override
    public void onAppEnterBackground() {
        Application.app.connectionManager.disconnectFromMqtt();
    }

    public void setListener(StateListener listener) {
        stateListener.setListener(listener);
    }

    public void initConnectionManager() {
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(app.getApplicationContext());
            connectionManager.init();
        }
    }

    private void initFetch(){
        WiFiFetch = new Fetch.Builder(this, "wifi_fetcher")
                .setDownloadConcurrentLimit(4)
                .enableRetryOnNetworkGain(true)
                .setGlobalNetworkType(NetworkType.WIFI_ONLY)
                .build();
        allNetworkFetch = new Fetch.Builder(this, "universal_fetcher")
                .setDownloadConcurrentLimit(4)
                .enableRetryOnNetworkGain(true)
                .setGlobalNetworkType(NetworkType.ALL)
                .build();
        ServiceFetchListener serviceFetchListener = new ServiceFetchListener();
        WiFiFetch.addListener(serviceFetchListener);
        allNetworkFetch.addListener(serviceFetchListener);
    }
}
