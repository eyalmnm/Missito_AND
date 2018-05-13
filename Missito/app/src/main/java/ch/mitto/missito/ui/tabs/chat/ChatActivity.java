package ch.mitto.missito.ui.tabs.chat;

import android.ch.mitto.missito.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import java.util.Collections;

import ch.mitto.missito.Application;
import ch.mitto.missito.ui.tabs.chats.model.Chat;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.util.Helper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static ch.mitto.missito.net.broker.MQTTConnectionManager.NEW_STATUS_UPDATE_EVENT;

public class ChatActivity extends AppCompatActivity {

    public static final String CHAT_EXTRA_KEY = "chat";
    public static final String TAG_CURRENT_FRAGMENT = "currentFragment";
    private ChatFragment chatFragment;
    private Chat chat;

    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MissitoContact contact = Application.app.contacts.missitoContactsByPhone.get(chat.participants.get(0).phone);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                String subtitle;
                if (contact.isOnline) {
                    subtitle = contact.getLastSeenLabel(ChatActivity.this);
                } else {
                    subtitle = String.format(getString(R.string.last_seen_label), contact.getLastSeenLabel(ChatActivity.this));
                }
                actionBar.setSubtitle(subtitle);
            }
        }
    };

    public static void start(Context context, MissitoContact contact) {
        Chat chat = new Chat(contact.phone, null, 0, Collections.singletonList(contact));
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(ChatActivity.CHAT_EXTRA_KEY, chat);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chat = (Chat) getIntent().getSerializableExtra(CHAT_EXTRA_KEY);
        chatFragment = ChatFragment.newInstance(chat);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, chatFragment, TAG_CURRENT_FRAGMENT).commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(chat.participants.get(0).name);
            String subtitle;
            if (chat.participants.get(0).isOnline) {
                subtitle = chat.participants.get(0).getLastSeenLabel(ChatActivity.this);
            } else {
                subtitle = String.format(getString(R.string.last_seen_label), chat.participants.get(0).getLastSeenLabel(ChatActivity.this));
            }
            actionBar.setSubtitle(subtitle);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver,
                new IntentFilter(NEW_STATUS_UPDATE_EVENT));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusUpdateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        MenuItem item = menu.findItem(R.id.action_profile);
        Helper.setMenuItemColor(this, item, android.R.color.white);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (chatFragment.bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            chatFragment.bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                finish();
                return true;
            case R.id.action_profile:
                Intent intent = new Intent(this, ContactInfoActivity.class);
                intent.putExtra(ContactInfoActivity.CONTACT_PHONE_KEY, chat.participants.get(0).phone);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
