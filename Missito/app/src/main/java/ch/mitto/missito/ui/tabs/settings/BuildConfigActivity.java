package ch.mitto.missito.ui.tabs.settings;

import android.ch.mitto.missito.BuildConfig;
import android.ch.mitto.missito.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.net.broker.MQTTConnectionManager;

import static ch.mitto.missito.net.broker.MQTTConnectionManager.KEY_MQTT_CONNECTION_STATE;
import static ch.mitto.missito.net.broker.MQTTConnectionManager.MQTT_CONNECTION_STATE_CHANGED;

public class BuildConfigActivity extends AppCompatActivity {

    private static final String LOG_TAG = BuildConfigActivity.class.getSimpleName();

    @BindView(R.id.app_version_label)
    TextView appVersionLabel;
    @BindView(R.id.app_url_label)
    TextView apiUrlLabel;
    @BindView(R.id.mqtt_host_label)
    TextView mqttHostLabel;
    @BindView(R.id.mqtt_login_label)
    TextView mqttLoginLabel;
    @BindView(R.id.mqtt_client_id_label)
    TextView mqttClientIdLabel;
    @BindView(R.id.mqtt_status_label)
    TextView mqttStatusLabel;

    private BroadcastReceiver mqttConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String state = intent.getExtras().getString(KEY_MQTT_CONNECTION_STATE);

            if (state != null) {
                mqttStatusLabel.setText(getString(R.string.mqtt_connection_state, state));
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_build_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.about_label);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ButterKnife.bind(this);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mqttConnectionStateReceiver, new IntentFilter(MQTT_CONNECTION_STATE_CHANGED));

        MQTTConnectionManager mqttManager = Application.app.connectionManager.mqttManager;
        appVersionLabel.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        apiUrlLabel.setText(getString(R.string.app_url, BuildConfig.API_ENDPOINT));
        mqttHostLabel.setText(getString(R.string.mqtt_host, mqttManager.getServerURI()));
        mqttLoginLabel.setText(getString(R.string.mqtt_login, Application.app.connectionManager.uid));
        mqttClientIdLabel.setText(getString(R.string.mqtt_client_id, mqttManager.getClientId()));
        mqttStatusLabel.setText(getString(R.string.mqtt_connection_state, getString(mqttManager.isConnected() ? R.string.mqtt_connected : R.string.mqtt_disconnected)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttConnectionStateReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
