package ch.mitto.missito.ui.tabs.chat;

import android.Manifest;
import android.app.ProgressDialog;
import android.ch.mitto.missito.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.Application;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.ui.tabs.chat.view.ClearHistoryDialog;
import ch.mitto.missito.util.AvatarWrapper;
import ch.mitto.missito.util.Helper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static ch.mitto.missito.net.broker.MQTTConnectionManager.NEW_STATUS_UPDATE_EVENT;

public class ContactInfoActivity extends AppCompatActivity {

    public static final String CONTACT_PHONE_KEY = "contact_for_info_key";
    private static final int REQUEST_CALL_PERMISSION = 1;
    private static final String LOG_TAG = ContactInfoActivity.class.getSimpleName();

    @BindView(R.id.contact_avatar)
    RoundedImageView avatar;
    @BindView(R.id.initials_txt)
    TextView contactInitials;
    @BindView(R.id.contact_name)
    TextView contactName;
    @BindView(R.id.contact_phone)
    TextView contactPhone;
    @BindView(R.id.contact_status)
    TextView contactStatus;
    @BindView(R.id.phone_number_type)
    TextView phoneNumberType;
    @BindView(R.id.blocked_label)
    TextView userBlocked;
    @BindView(R.id.shared_media_count)
    TextView shradeMediaCount;
    @BindView(R.id.mute_switch)
    Switch muteSwitch;
    private AvatarWrapper avatarWrapper;
    private MissitoContact contact;
    private ProgressDialog progressDialog;

    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            contact = Application.app.contacts.missitoContactsByPhone.get(contact.phone);
            updateBlockedLabel();
            updateStatusLabel();
            updateMuteSwitch();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contact = Application.app.contacts.missitoContactsByPhone.get(getIntent().getStringExtra(CONTACT_PHONE_KEY));
        setContentView(R.layout.contact_info_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.contact_info);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ButterKnife.bind(this);
        avatarWrapper = new AvatarWrapper(this, avatar, contactInitials);
        avatarWrapper.update(contact);
        contactName.setText(contact.name);
        contactPhone.setText(Helper.formatPhoneInternational(contact.phone));
        updateStatusLabel();
        updateBlockedLabel();

        //TODO: use later when start implementing the rest of screen functionality
        phoneNumberType.setText("mobile");
        shradeMediaCount.setText("1");
        updateMuteSwitch();
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, new IntentFilter(NEW_STATUS_UPDATE_EVENT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusUpdateReceiver);
        hideProgressDialog();
    }

    private void updateStatusLabel() {
        contactStatus.setText(contact.getLastSeenLabel(this));
        contactStatus.setTextColor(ContextCompat.getColor(this, contact.isOnline ? R.color.green : R.color.manatee));
    }

    private void updateBlockedLabel() {
        userBlocked.setText(contact.blocked
                ? R.string.unblock_user_label
                : R.string.block_user_label);
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

    @OnClick(R.id.block_user_layout)
    public void onBlockClicked() {
        if (Application.app.contacts.isBlocked(contact.phone)) {
            updateContactStatus(Collections.singletonList(contact.phone), null, null);
        } else {
            updateContactStatus(null, Collections.singletonList(contact.phone), null);
        }
    }

    public void updateContactStatus(final List<String> normal, final List<String> block, final List<String> muted) {
        showProgressDialog();
        Application.app.connectionManager.apiRequests.setContactStatus(normal, block, muted, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                if (block != null) {
                    Application.app.contacts.setContactsBlocked(block, true);
                    Log.d(LOG_TAG, String.format("Contacts  blocked %s", block));
                }

                if (normal != null) {
                    Application.app.contacts.setContactsBlocked(normal, false);
                    Application.app.contacts.setContactsMuted(normal, false);
                    Log.d(LOG_TAG, String.format("Contacts  unblocked %s", normal));
                }

                if (muted != null) {
                    Application.app.contacts.setContactsMuted(muted, true);
                    Log.d(LOG_TAG, String.format("Contacts  muted %s", muted));
                }

                Intent intent = new Intent(NEW_STATUS_UPDATE_EVENT);
                LocalBroadcastManager.getInstance(Application.app).sendBroadcast(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                updateMuteSwitch();
                hideProgressDialog();
                Log.w(LOG_TAG, "Could not change contacts status", error);
            }
        });
    }

    private void updateMuteSwitch() {
        muteSwitch.setEnabled(!contact.blocked);
        muteSwitch.setChecked(!contact.blocked && contact.muted);
    }

    @OnClick(R.id.clear_history_layout)
    public void onClearHistoryClicked() {
        ClearHistoryDialog clearHistoryDialog = ClearHistoryDialog.newInstance(contact.phone);
        clearHistoryDialog.show(getSupportFragmentManager(), "dialog");
    }

    @OnClick(R.id.mute_switch)
    public void onMuteUserClicked() {
        List<String> list = Collections.singletonList(contact.phone);
        if (muteSwitch.isChecked()) {
            updateContactStatus(null, null, list);
        } else {
            updateContactStatus(list, null, null);
        }
    }

    @OnClick(R.id.shared_media_layout)
    public void onSharedMediaClicked() {
        Toast.makeText(this, "Shared media screen in development", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.to_chat_layout)
    public void onChatClicked() {
        //TODO: this button will be probably useful later. Maybe hide it for now?0
        onBackPressed();
    }

    @OnClick(R.id.to_call_layout)
    public void onCallClicked() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
            return;
        }
        call();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                call();
                return;
            }
            Toast.makeText(this, R.string.no_phone_call_permission, Toast.LENGTH_SHORT).show();
        }
    }

    private void call() {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact.phone));
        startActivity(intent);
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog.show(this, "", "In progress ...");
            progressDialog.setCancelable(false);
        }

        if (progressDialog.isShowing()) {
            return;
        }

        progressDialog.show();
    }



}
