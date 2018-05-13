package ch.mitto.missito.ui.tabs.settings;

import android.app.ProgressDialog;
import android.ch.mitto.missito.R;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.Application;
import ch.mitto.missito.net.ConnectionManager;
import ch.mitto.missito.net.model.ProfileSettings;
import ch.mitto.missito.util.PrefsHelper;

public class PrivacySettingsActivity extends AppCompatActivity {

    private static final String TAG = PrivacySettingsActivity.class.getSimpleName();

    @BindView(R.id.show_user_status)
    CheckBox showStatus;

    @BindView(R.id.send_seen_status)
    CheckBox sendSeenStatus;

    @BindView(R.id.show_user_status_subtitle)
    TextView showUserStatusSubtitle;

    private ProgressDialog progressDialog;
    private final int SHOW_STATUS_SWITCH_DELAY = 1000 * 60 * 60 * 24;

    private Handler handler = new Handler();
    private Runnable subtitleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateItems();
            handler.postDelayed(subtitleUpdateRunnable, 60000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_privacy_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.privacy_label);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ButterKnife.bind(this);
        ProfileSettings ps = Application.app.connectionManager.profileSettings;
        if (ps == null) {
            final AlertDialog.Builder retryDialogBuilder = new AlertDialog.Builder(this);
            retryDialogBuilder.setTitle(R.string.something_went_wrong)
                    .setMessage(R.string.alert_something_went_wrong)
                    .setCancelable(false)
                    .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int i) {
                            showProgressDialog();
                            Application.app.connectionManager.apiRequests.getProfileSettings(new Response.Listener<ProfileSettings>() {
                                @Override
                                public void onResponse(ProfileSettings response) {
                                    hideProgressDialog();
                                    Application.app.connectionManager.profileSettings = response;
                                    ProfileSettings ps = Application.app.connectionManager.profileSettings;
                                    showStatus.setChecked(ps.presenceStatus);
                                    sendSeenStatus.setChecked(ps.messageStatus);
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e(TAG, "Get profile ERROR", error);
                                    hideProgressDialog();
                                    dialogInterface.cancel();
                                    retryDialogBuilder.show();
                                }
                            });
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            finish();
                        }
                    })
                    .create()
                    .show();
        } else {
            showStatus.setChecked(ps.presenceStatus);
            sendSeenStatus.setChecked(ps.messageStatus);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        subtitleUpdateRunnable.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    private void updateItems() {
        showStatus.setEnabled(canChangeShowStatusSetting());
        showUserStatusSubtitle.setText(getShowStatusRemainingTimeSubtitle());
    }

    private String getShowStatusRemainingTimeSubtitle() {
        if (canChangeShowStatusSetting()) {
            return getString(R.string.show_online_status_description);
        }

        String subtitle = getString(R.string.youll_be_able_change_setting);
        long remainingTime = (PrefsHelper.getOnlineStatusChangeTime() - System.currentTimeMillis()) / 60000;
        int timeStrResId;

        if (remainingTime > 60) {
            timeStrResId = R.plurals.n_hours;
            remainingTime = Math.round(remainingTime / 60.0);
        } else {
            timeStrResId = R.plurals.n_min;
        }
        String timeStr = String.format(getResources().getQuantityString(timeStrResId, (int) remainingTime), remainingTime);
        return String.format(subtitle, timeStr);
    }

    private boolean canChangeShowStatusSetting() {
        long lastTimeShowStatus = PrefsHelper.getOnlineStatusChangeTime();
        return ((lastTimeShowStatus - System.currentTimeMillis()) <= 0);
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

    @OnClick(R.id.show_user_status)
    public void toggleStatusVisibility() {
        showProgressDialog();
        final ConnectionManager cm = Application.app.connectionManager;
        cm.apiRequests.toggleStatusVisibility(showStatus.isChecked(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        cm.profileSettings.presenceStatus = showStatus.isChecked();
                        PrefsHelper.setOnlineStatusChangeTime(System.currentTimeMillis() + SHOW_STATUS_SWITCH_DELAY);
                        updateItems();
                        hideProgressDialog();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        showErrorDialog();
                        showStatus.setChecked(!showStatus.isChecked());
                    }
                });
    }

    @OnClick(R.id.send_seen_status)
    public void allowSendMessageSeenStatus() {
        showProgressDialog();
        final ConnectionManager cm = Application.app.connectionManager;
        cm.apiRequests.allowSendMessageSeenStatus(sendSeenStatus.isChecked(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        cm.profileSettings.messageStatus = sendSeenStatus.isChecked();
                        hideProgressDialog();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        showErrorDialog();
                        sendSeenStatus.setChecked(!sendSeenStatus.isChecked());
                    }
                });
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.unable_to_send_privacy_settings)
                .setCancelable(false)
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            // TODO: refactor
            progressDialog = ProgressDialog.show(this, "", "In progress ...");
            progressDialog.setCancelable(false);
        }

        if (progressDialog.isShowing()) {
            return;
        }

        progressDialog.show();
    }

}
