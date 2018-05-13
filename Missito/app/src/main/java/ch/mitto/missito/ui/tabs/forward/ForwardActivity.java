package ch.mitto.missito.ui.tabs.forward;

import android.app.ProgressDialog;
import android.ch.mitto.missito.BuildConfig;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.ch.mitto.missito.R;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.db.model.attach.AudioAttachRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.db.model.attach.VideoAttachRec;
import ch.mitto.missito.net.model.OutgoingMessageStatus;
import ch.mitto.missito.ui.common.ContactPickerListener;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.util.DownloadFileAsyncTask;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.MissitoConfig;
import ch.mitto.missito.util.RealmDBHelper;
import ch.mitto.missito.util.SendMessageHelper;
import io.realm.Realm;


public class ForwardActivity extends AppCompatActivity implements ContactPickerListener {


    public static final String MESSAGE_ID_KEY = "msg_id";
    public static final String CALLING_SCREEN_COMPANION_PHONE_KEY = "phone";
    private static final String LOG_TAG = ForwardActivity.class.getSimpleName();
    public static final String NEW_MESSAGES_KEY = "new_msgs";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    MessageRec message;
    Realm realm;
    String callingScreenCompanionPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeNoTitleBar);
        setContentView(R.layout.activity_forward);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);


        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, new ForwardFagment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
        }

        realm = Realm.getDefaultInstance();
        if (getIntent() != null) {
            String localMsgId = getIntent().getStringExtra(MESSAGE_ID_KEY);
            if (localMsgId != null) {
                message = realm.where(MessageRec.class).equalTo("localMsgId", localMsgId).findFirst();
            }
            callingScreenCompanionPhone = getIntent().getStringExtra(CALLING_SCREEN_COMPANION_PHONE_KEY);
        }


//        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver,
//                new IntentFilter(MSG_STATUS_UPDATE_EVENT));
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

    private ProgressDialog dialog;

    @Override
    public void onContactsSelected(final ArrayList<String> phones) {
        final ArrayList<MessageRec> newMessages = new ArrayList<>(phones.size());
        dialog = new ProgressDialog(this);
        dialog.setMessage(getResources().getString(R.string.downloading_attachments));
        if (message.attach == null) {
            for (String phone : phones) {
                Integer deviceId = Application.app.contacts.deviceIds.get(phone);
                MessageRec msg = MessageRec.getBuilder()
                    .setTimestamp(new Date().getTime())
                    .setSenderUid(Application.app.connectionManager.uid)
                    .setSenderDeviceId(Application.app.connectionManager.deviceId)
                    .setDestUid(phone)
                    .setDestDeviceId(deviceId)
                    .setBody(message.body)
                    .setOutgoingStatus(OutgoingMessageStatus.OUTGOING)
                    .build();
                SendMessageHelper.sendMessage(msg);
                newMessages.add(msg);
            }
            finishWithMessages(newMessages);
        } else {
            dialog.show();
            downloadAllAttachments(new Runnable() {
                @Override
                public void run() {
                    for (String phone : phones) {
                        MessageRec msg = SendMessageHelper.prepareSendWithAttachment(ForwardActivity.this, phone, "", message.attach, null);
                        if (msg != null) {
                            newMessages.add(msg);

                        }
                    }
                    dialog.dismiss();
                    finishWithMessages(newMessages);
                }
            });
        }
    }

    private void downloadAllAttachments(final Runnable completion) {
        AttachmentRec attach = message.attach;
        final AtomicInteger workCount = new AtomicInteger(0);
        List<DownloadFileAsyncTask> tasks = new ArrayList<>();

        // Download audio attachments
        if (attach.hasAudio()) {
            for (final AudioAttachRec audio : attach.audio) {
                if (audio.localFileURI != null) {
                    continue;
                }
                String audioPath = MissitoConfig.getAttachmentsPath(message.senderUid);
                final String filePath = audioPath + audio.fileName;
                tasks.add(new DownloadFileAsyncTask(BuildConfig.API_ENDPOINT + audio.link, filePath) {
                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success) {
                            RealmDBHelper.setAudioMsgFileURI(audio, Helper.getUriFromFile(new File(filePath)).toString());
                            int workLeft = workCount.decrementAndGet();
                            if (workLeft == 0) {
                                completion.run();
                            }
                        } else {
                            Log.e(LOG_TAG, "Failed to download or save file");
                        }
                    }
                });
                workCount.incrementAndGet();
            }
        }

        // Download video attachments
        if (attach.hasVideo()) {
            for (final VideoAttachRec video : attach.video) {
                if (video.localFileURI != null) {
                    continue;
                }
                final String filePath = MissitoConfig.getAttachmentsPath(message.senderUid) + video.fileName;
                tasks.add(new DownloadFileAsyncTask(BuildConfig.API_ENDPOINT + video.link, filePath) {
                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success) {
                            RealmDBHelper.setVideoMsgFileURI(video, Helper.getUriFromFile(new File(filePath)).toString());
                            int workLeft = workCount.decrementAndGet();
                            if (workLeft == 0) {
                                completion.run();
                            }
                        } else {
                            Log.e(LOG_TAG, "Failed to download or save file");
                        }
                    }
                });
                workCount.incrementAndGet();
            }
        }

        // Download images
        if (attach.hasImages()) {
            for (final ImageAttachRec image : attach.images) {
                if (image.localFileURI != null) {
                    continue;
                }
                final String filePath = MissitoConfig.getAttachmentsPath(message.senderUid) + image.fileName;
                tasks.add(new DownloadFileAsyncTask(BuildConfig.API_ENDPOINT + image.link, filePath) {
                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success) {
                            RealmDBHelper.setImageMsgFileURI(image, Helper.getUriFromFile(new File(filePath)).toString());
                            int workLeft = workCount.decrementAndGet();
                            if (workLeft == 0) {
                                completion.run();
                            }
                        } else {
                            Log.e(LOG_TAG, "Failed to download or save file");
                        }
                    }
                });
                workCount.incrementAndGet();
            }
        }

        if (tasks.isEmpty()) {
            completion.run();
        } else {
            for (DownloadFileAsyncTask task : tasks) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }


    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO when message is sent
        }
    };


    @Override
    public void onBackPressed() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else if (backStackEntryCount > 1) {
            super.onBackPressed();
        } else {
            finishWithMessages(new ArrayList<MessageRec>());
        }
    }

    private void finishWithMessages(ArrayList<MessageRec> messages) {
        ArrayList<ChatMessage> result = new ArrayList<>(messages.size());
        for (MessageRec messageRec : messages) {
            if (messageRec.destUid.equals(callingScreenCompanionPhone)) {
                result.add(new OutgoingChatMessage(messageRec));
            }
        }
        Intent intent = new Intent();
        intent.putExtra(NEW_MESSAGES_KEY, result);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusUpdateReceiver);
    }
}
