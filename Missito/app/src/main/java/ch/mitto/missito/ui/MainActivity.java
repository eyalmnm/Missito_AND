package ch.mitto.missito.ui;

import android.Manifest;
import android.app.Dialog;
import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.ChatRec;
import ch.mitto.missito.net.ConnectionManager;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.ui.common.ContactPickerListener;
import ch.mitto.missito.ui.signin.SigninActivity;
import ch.mitto.missito.ui.tabs.MainTabsFragment;
import ch.mitto.missito.ui.tabs.chat.ChatActivity;
import ch.mitto.missito.ui.tabs.chats.ChatsFragment;
import ch.mitto.missito.ui.tabs.chats.model.Chat;
import ch.mitto.missito.ui.tabs.contacts.InviteFragment;
import ch.mitto.missito.ui.tabs.contacts.InviteResultFragment;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.PrefsHelper;
import ch.mitto.missito.util.RealmDBHelper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static ch.mitto.missito.net.ConnectionManager.AuthState.LOGGED_IN;

public class MainActivity extends AppCompatActivity implements
        ChatsFragment.Listener,
        ContactPickerListener,
        MainActivityAccess,
        InviteResultFragment.Listener,
        ConnectionManager.GetUserNameHandler {


    public static final String TAG_CURRENT_FRAGMENT = "content_fragment";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQ_CODE = 101;
    private static final int PERIOD = 1000 * 20 * 60 * 48;

    public static final String PERMISSIONS_GRANTED_EVT = "permissions_granted";

    private Fragment currentFragment;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;


    private Dialog inviteDialog ;
    private void showInviteDialog() {
        if (inviteDialog != null && inviteDialog.isShowing()) {
            return;
        }
        inviteDialog = new Dialog(this);
        inviteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        inviteDialog.setContentView(R.layout.dialog_invite_dialog);
        TextView noThanksView = (TextView) inviteDialog.findViewById(R.id.no_thanks);
        noThanksView.setPaintFlags(noThanksView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        noThanksView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inviteDialog.dismiss();
            }
        });
        inviteDialog.findViewById(R.id.inviteBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inviteDialog.dismiss();
                replaceFragment(new InviteFragment());
            }
        });
        inviteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        inviteDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        inviteDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                PrefsHelper.setInvitationDate(System.currentTimeMillis() + PERIOD);
            }
        });
        inviteDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeNoTitleBar);
        if (Application.app.connectionManager.authState != LOGGED_IN) {
            SigninActivity.start(this);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        Application.app.connectionManager.resendUndeliveredMessages();
        Application.app.connectionManager.resendIncomingStatuses();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, MainTabsFragment.newInstance())
                .addToBackStack(TAG_CURRENT_FRAGMENT)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkCurrentPermissions()) {
                requestPermissions();
            } else {
                invokeUsername();
            }
        } else {
            invokeUsername();
        }

        if (getIntent().hasExtra("phone")) {
            String contactPhone = Helper.addPlus(getIntent().getStringExtra("phone"));
            MissitoContact contact = Application.app.contacts.missitoContactsByPhone.get(contactPhone);
            if (contact != null) {
                ChatRec chatRec = RealmDBHelper.getChat(contactPhone);
                Chat chat;
                if (chatRec != null) {
                    chat = new Chat(chatRec);
                } else {
                    chat = new Chat(contactPhone, null, 0, Collections.singletonList(contact));
                }

                onChatSelected(chat);
            }
        }
    }

    // Get username as it stored on server
    private void invokeUsername() {
        Application.app.connectionManager.getUserName(this);
    }

    // ConnectionManager.GetUserNameHandler callback method
    @Override
    public void onResult(final String name) {
        if (name != null && false == name.isEmpty()) {
            PrefsHelper.saveUsername(name);
        }
    }


    private boolean shouldShowInvitationHeader() {
        long invite = PrefsHelper.getInvitationDate();
        return ((invite - System.currentTimeMillis()) <= 0);
    }

    private boolean checkCurrentPermissions() {
        return Helper.canReadContacts(this);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                PERMISSIONS_REQ_CODE);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQ_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Application.app.connectionManager.synchronizeContacts();
                    notifyPermissionsGranted();
                    invokeUsername();
                } else {
                    //not granted
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldShowInvitationHeader()) {
            showInviteDialog();
        }
    }

    private void notifyPermissionsGranted() {
        Intent intent = new Intent(PERMISSIONS_GRANTED_EVT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onBackPressed() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else if (backStackEntryCount > 1) {
            super.onBackPressed();
        } else {
            finish();
        }
    }

    @Override
    public void onChatSelected(Chat chat) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.CHAT_EXTRA_KEY, chat);
        startActivity(intent);
    }

    @Override
    public void onContactsSelected(ArrayList<String> phones) {
        String current = getResources().getConfiguration().locale.getLanguage();
        replaceFragment(InviteResultFragment.newInstance(phones, current));
    }


    @Override
    public void onCancel(InviteResultFragment sender) {
        getSupportFragmentManager().popBackStack(MainActivity.TAG_CURRENT_FRAGMENT, 0);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void updateTitle(String title, boolean homeButtonFlag) {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(title);
            supportActionBar.setHomeButtonEnabled(homeButtonFlag);
            supportActionBar.setDisplayHomeAsUpEnabled(homeButtonFlag);
        }
    }

    @Override
    public void onInviteCalled(Fragment sender) {
        replaceFragment(new InviteFragment());
        if (shouldShowInvitationHeader()) {
            PrefsHelper.setInvitationDate(System.currentTimeMillis() + PERIOD);
        }
    }
}
