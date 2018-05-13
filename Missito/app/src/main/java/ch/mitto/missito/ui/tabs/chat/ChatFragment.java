package ch.mitto.missito.ui.tabs.chat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.ch.mitto.missito.BuildConfig;
import android.ch.mitto.missito.R;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.stfalcon.frescoimageviewer.ImageViewer;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Func;
import com.tonyodev.fetch2.Status;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.db.model.attach.AudioAttachRec;
import ch.mitto.missito.db.model.attach.ContactAttachRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.db.model.attach.LocationAttachRec;
import ch.mitto.missito.db.model.attach.VideoAttachRec;
import ch.mitto.missito.db.model.common.RealmString;
import ch.mitto.missito.events.ClearHistoryEvent;
import ch.mitto.missito.net.ConnectionManager;
import ch.mitto.missito.net.model.AttachmentSpec;
import ch.mitto.missito.net.model.IncomingMessageStatus;
import ch.mitto.missito.net.model.OutgoingMessageStatus;
import ch.mitto.missito.services.DownloadHelper;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatSectionedAdapter;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.ui.tabs.chat.view.CameraActivity;
import ch.mitto.missito.ui.tabs.chats.model.Chat;
import ch.mitto.missito.ui.tabs.forward.ForwardActivity;
import ch.mitto.missito.util.AESHelper;
import ch.mitto.missito.util.DecryptAttachmentAsyncTask;
import ch.mitto.missito.util.DownloadFileAsyncTask;
import ch.mitto.missito.util.EncryptAttachmentAsyncTask;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.ImageHelper;
import ch.mitto.missito.util.MediaPlayerSingleton;
import ch.mitto.missito.util.MissitoConfig;
import ch.mitto.missito.util.RealmDBHelper;
import ch.mitto.missito.util.SendMessageHelper;
import ch.mitto.missito.util.SendMessageListener;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static ch.mitto.missito.net.broker.MQTTConnectionManager.MSG_STATUS_UPDATE_EVENT;
import static ch.mitto.missito.net.broker.MQTTConnectionManager.MSG_STATUS_UPDATE_ID;
import static ch.mitto.missito.ui.tabs.chat.ImageOrVideoSelectedActivity.IMAGE_FILE_KEY;
import static ch.mitto.missito.ui.tabs.chat.ImageOrVideoSelectedActivity.IS_FROM_CAMERA_KEY;
import static ch.mitto.missito.ui.tabs.chat.view.CameraActivity.CONTACT_PHONE_KEY;
import static ch.mitto.missito.ui.tabs.chat.view.CameraActivity.FILEPATH_KEY;
import static ch.mitto.missito.ui.tabs.forward.ForwardActivity.NEW_MESSAGES_KEY;
import static ch.mitto.missito.util.NotificationHelper.INCOMING_MESSAGE_SAVED;
import static ch.mitto.missito.util.NotificationHelper.KEY_MESSAGE;
import static ch.mitto.missito.util.NotificationHelper.MESSAGE_REC_KEY;
import static ch.mitto.missito.util.NotificationHelper.NEW_TYPING_NOTIFICATION;
import static ch.mitto.missito.util.NotificationHelper.NEW_TYPING_UID_KEY;
import static ch.mitto.missito.util.NotificationHelper.LOADED_AND_DECRYPTED_IMAGE_EVENT;
import static ch.mitto.missito.util.NotificationHelper.SUCCESFULLY_LOADED_AND_DECRYPTED_KEY;

public class ChatFragment extends Fragment implements ChatSectionedAdapter.Listener, SendMessageListener {

    private static final int PERMISSIONS_REQUEST_CODE = 5;
    private static final int CAMERA_PERM_REQUEST_CODE = 6;
    private static final int WRITE_EXT_STORAGE_REQUEST_CODE = 7;
    private static final int RECORD_AUDIO = 9;
    private static final int CHOOSE_CONTACT = 8501;
    private static final int PLACE_PICKER_REQUEST = 4;

    private static final String LOG_TAG = ChatFragment.class.getSimpleName();
    private static final String ARG_CHAT = "chat";
    private static final int GALLERY_SELECT_REQUEST_CODE = 10;
    private static final int CAMERA_SELECT_REQUEST_CODE = 11;
    private static final int VIDEO_PERM_REQUEST_CODE = 11;
    private static final int IMAGE_OR_VIDEO_CONFIRM_REQUEST_CODE = 12;
    private static final int FORWARD_REQ_CODE = 13;

    public BottomSheetBehavior bottomSheetBehavior;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;
    @BindView(R.id.et_message_text)
    EditText messageText;
    @BindView(R.id.typing)
    TextView typingLabel;
    @BindView(R.id.audio_msg)
    ImageButton audioMessage;
    @BindView(R.id.send_message)
    ImageButton sendMessageRightButton;
    @BindView(R.id.gallery_picker)
    ImageButton galleryPickerButton;
    @BindView(R.id.camera_shot)
    ImageButton cameraShotButton;
    @BindView(R.id.add_attachment)
    ImageButton attachmentButton;
    @BindView(R.id.shrink_text_view_button)
    ImageButton shrinkTextViewButton;
    @BindView(R.id.bottom_sheet)
    NestedScrollView bottomSheet;
    private PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
    private ChatSectionedAdapter adapter;
    private ImageViewer imageViewer;
    private Chat chat;
    private MissitoContact contact;
    private boolean typing, fromCamera;
    private File attachmentFile;
    private ArrayList<AsyncTask> asyncTasks = new ArrayList<>();
    private Handler handler = new Handler();
    private boolean messageSend = false;
    private ProgressDialog dialog;
    private ImageAttachRec imageToOpenAfterDownloading;
    
    private Runnable hideTypingLabel = new Runnable() {
        @Override
        public void run() {
            typingLabel.setVisibility(View.GONE);
        }
    };
    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final MessageRec incomingMessage = (MessageRec) intent.getExtras()
                    .getSerializable(KEY_MESSAGE);

            String partnerPhone = contact.phone;
            // TODO: consider deviceId
            if (incomingMessage == null || partnerPhone == null ||
                    !partnerPhone.equals(incomingMessage.senderUid)) {
                return;
            }
            adapter.insert(ChatMessage.make(incomingMessage, contact));
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            typingLabel.setVisibility(View.GONE);

            RealmDBHelper.setIncomingMsgStatus(incomingMessage.serverMsgId, IncomingMessageStatus.SEEN);
            Application.app.connectionManager.confirmMessageSeen(incomingMessage);
        }
    };
    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String localMsgID = (String) intent.getExtras()
                    .getSerializable(MSG_STATUS_UPDATE_ID);

            MessageRec message = RealmDBHelper.getMessageByLocalId(localMsgID);
            if (fromCurrentChat(message)) {
                adapter.updateMessageStatus(contact.phone);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        }
    };
    private BroadcastReceiver typingNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String from = intent.getExtras().getString(NEW_TYPING_UID_KEY);

            if (from != null && from.equals(contact.phone)) {
                handler.removeCallbacks(hideTypingLabel);
                typingLabel.setText(String.format(getString(R.string.is_typing), contact.name));
                typingLabel.setVisibility(View.VISIBLE);
                handler.postDelayed(hideTypingLabel, 5000);
            }

        }
    };
    private BroadcastReceiver imageLoadedAndDecryptedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean succesfullyLoadedAndDecrypted = intent.getBooleanExtra(SUCCESFULLY_LOADED_AND_DECRYPTED_KEY, false);
            MessageRec messageRec = (MessageRec) intent.getSerializableExtra(MESSAGE_REC_KEY);
            if (!messageRec.senderUid.equals(contact.phone)) {
                return;
            }
            if (succesfullyLoadedAndDecrypted) {
                adapter.notifyDataSetChanged();
                dismissProgressDialog();
                if (mustOpenImageUri(messageRec.attach.images.get(0).localFileURI)) {
                    List<ImageAttachRec> images = Collections.singletonList(imageToOpenAfterDownloading);
                    openImageViewer(images);
                    imageToOpenAfterDownloading = null;
                }
            } else {
                onStopOrCancelLoadingImage(messageRec);
            }
        }
    };

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(Chat chat) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CHAT, chat);
        fragment.setArguments(args);
        return fragment;
    }

    public static String getRealPathFromContentUri(Context context, String companion, Uri contentUri) {
        String fileName = FilenameUtils.getBaseName(contentUri.getPath());
        if (!TextUtils.isEmpty(fileName)) {
            File copyFile = new File(MissitoConfig.getAttachmentsPath(companion), fileName);
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
                OutputStream outputStream = new FileOutputStream(copyFile);
                IOUtils.copy(inputStream, outputStream);
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return copyFile.getAbsolutePath();
        }
        return null;
    }

    @OnClick(R.id.location)
    public void onLocationBtnPressed() {
        try {
            FragmentActivity activity = getActivity();
            activity.setTheme(R.style.AppThemeLight);   //Setting activity theme colors to white otherwise default app colors
            // are used and text in place select picker toolbar is black,
            // that doesn't look well with our app colorPrimary.
            Intent intent = builder.build(activity);
            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Toast.makeText(this.getContext(), R.string.location_select_error, Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "Could not start location select screen", e);
        }
    }

    @OnClick(R.id.audio_msg)
    public void onAudioBtnPressed() {
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        PackageManager manager = getActivity().getPackageManager();
        List<ResolveInfo> list = manager.queryIntentActivities(intent, 0);

        if (list != null && list.size() > 0) {
            startActivityForResult(intent, RECORD_AUDIO);
        } else {
            Toast.makeText(getContext(), R.string.no_audio_record_apps, Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.shrink_text_view_button)
    public void onShrinkClicked() {
        changeToolbarView(false);
    }

    @OnClick(R.id.send_message)
    public void onSendMessage() {
        messageSend = true;
        String msgText = messageText.getText().toString();
        if (!TextUtils.isEmpty(msgText)) {
            appendMessage(SendMessageHelper.prepareSendWithAttachment(getActivity(), contact.phone,
                    msgText, null, this));

            messageText.setText("");
        }
        messageSend = false;
    }

    @OnClick(R.id.gallery_picker)
    public void onGallerySelected() {
        if (!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXT_STORAGE_REQUEST_CODE);
            Log.d(LOG_TAG, "No permissions for images");
            return;
        }
        openGallery();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT < 19) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/* video/*");
            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.image_select_or_video)), GALLERY_SELECT_REQUEST_CODE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            startActivityForResult(intent, GALLERY_SELECT_REQUEST_CODE);
        }
    }

    @OnClick(R.id.camera_shot)
    public void onCameraShotSelected() {
        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            requestPermission(Manifest.permission.CAMERA, VIDEO_PERM_REQUEST_CODE);
            Log.d(LOG_TAG, "No permissions for camera");
            return;
        }
        openCamera();
    }

    private void openCamera() {
        Intent intent = new Intent(new Intent(getActivity(), CameraActivity.class));
        intent.putExtra(CONTACT_PHONE_KEY, contact.phone);
        startActivityForResult(intent, CAMERA_SELECT_REQUEST_CODE);
    }

    @OnClick(R.id.contact_picker)
    public void chooseContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, CHOOSE_CONTACT);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        Fresco.initialize(getActivity(), ImageHelper.getFrescoConfig(getContext()));
        checkForPermissions();
        if (getArguments() != null) {
            chat = (Chat) getArguments().getSerializable(ARG_CHAT);
            contact = chat.participants.get(0);
            Application.app.contacts.resetContactUnreadCount(contact.phone);
            final List<MessageRec> history = getHistory();

            adapter = new ChatSectionedAdapter(history, contact, this);

            ArrayList<MessageRec> deliveredMessages = new ArrayList<>();
            for (MessageRec message : history) {
                // TODO: consider deviceId
                if (!IncomingMessageStatus.SEEN_ACKNOWLEDGED.value.equals(message.incomingStatus)
                        && message.destUid.equals(Application.app.connectionManager.uid)) {
                    deliveredMessages.add(message);
                }
            }

            if (!deliveredMessages.isEmpty()) {
                Application.app.connectionManager.confirmMessagesSeen(deliveredMessages);
            }
        }
    }

    private void requestPermission(String permissionCode, int requestCode) {
        requestPermissions(new String[]{permissionCode}, requestCode);
    }

    private void checkForPermissions() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
    }

    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(getActivity(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isPermissionGranted(int[] grantResults) {
        return grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.bind(this, view);

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0);
                return false;
            }
        });

        messageText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (messageSend) {
                    return;
                }
                Integer deviceId = Application.app.contacts.deviceIds.get(contact.phone);
                if (!typing && deviceId != null) {
                    typing = true;
                    final MessageRec message =
                            MessageRec.getBuilder()
                                    .setTimestamp(new Date().getTime())
                                    .setSenderUid(Application.app.connectionManager.uid)
                                    .setSenderDeviceId(Application.app.connectionManager.deviceId)
                                    .setDestUid(contact.phone)
                                    .setDestDeviceId(deviceId)
                                    .build();

                    Application.app.connectionManager.sendMessage(message, true);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            typing = false;
                        }
                    }, 4000);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        recyclerView.setItemAnimator(null);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);
        messageText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                changeToolbarView(hasFocus);
            }
        });
        messageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (attachmentButton.getVisibility() == View.VISIBLE) {
                    changeToolbarView(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        messageText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

                    String msgText = textView.getText().toString();
                    if (!TextUtils.isEmpty(msgText)) {
                        appendMessage(SendMessageHelper.prepareSendWithAttachment(getActivity(), contact.phone,
                                msgText, null, ChatFragment.this));
                        messageText.setText("");
                    }

                    return true;
                }
                // Return true if you have consumed the action, else false.
                return false;
            }
        });
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(newMessageReceiver,
                new IntentFilter(INCOMING_MESSAGE_SAVED));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(statusUpdateReceiver,
                new IntentFilter(MSG_STATUS_UPDATE_EVENT));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(typingNotificationReceiver,
                new IntentFilter(NEW_TYPING_NOTIFICATION));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(imageLoadedAndDecryptedReceiver,
                new IntentFilter(LOADED_AND_DECRYPTED_IMAGE_EVENT));

        return view;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void clearHistory(ClearHistoryEvent eventContact) {
        if (eventContact.getContactPhone().equals(contact.phone)) {
            List<MessageRec> history = getHistory();
            if (history.size() > 0) {
                adapter = new ChatSectionedAdapter(history, contact, this);
                recyclerView.setAdapter(adapter);
            } else {
                getActivity().finish();
            }
        }
    }

    private void changeToolbarView(boolean hasFocus) {
        boolean isTextViewEmpty = TextUtils.isEmpty(messageText.getText().toString());
        audioMessage.setVisibility(hasFocus || !isTextViewEmpty ? View.GONE : View.VISIBLE);
        attachmentButton.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
        cameraShotButton.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
        galleryPickerButton.setVisibility(hasFocus ? View.GONE : View.VISIBLE);

        sendMessageRightButton.setVisibility(hasFocus || !isTextViewEmpty ? View.VISIBLE : View.GONE);
        shrinkTextViewButton.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
    }

    private void sendVideo(final String videoPath) {
        attachmentFile = new File(videoPath);
        fromCamera = true;
        Log.d(LOG_TAG, String.format("Sending video file with path: %s ", attachmentFile.getAbsolutePath()));
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
        ImageHelper.CreateThumbnailAsyncTask asyncTask =
                new ImageHelper.CreateThumbnailAsyncTask(thumbnail,
                        new ImageHelper.CreateThumbnailAsyncTask.OnFinishedListener() {
                            @Override
                            public void onFinished(ImageHelper.CreateThumbnailAsyncTask task, String base64Thumbnail) {
                                VideoAttachRec videoAttachRec = new VideoAttachRec(attachmentFile.getName(), new File(videoPath).getName(),
                                        null, attachmentFile.length(), "some_secret", base64Thumbnail, "file://" + videoPath);
                                prepareSendMessage(null, videoAttachRec);
                            }
                        });
        asyncTask.execute();
        asyncTasks.add(asyncTask);

    }

    private void sendImage(final String imagePath, final String message) {
        attachmentFile = new File(imagePath);
        ImageHelper.CreateThumbnailAsyncTask task =
                new ImageHelper.CreateThumbnailAsyncTask(attachmentFile,
                        new ImageHelper.CreateThumbnailAsyncTask.OnFinishedListener() {
                            @Override
                            public void onFinished(ImageHelper.CreateThumbnailAsyncTask task, String base64Thumbnail) {
                                asyncTasks.remove(task);
                                ImageAttachRec imageAttachRec = new ImageAttachRec(attachmentFile, base64Thumbnail);
                                prepareSendMessage(message, Collections.singletonList(imageAttachRec));
                            }
                        });
        asyncTasks.add(task);
        task.execute();
    }

    private void prepareSendMessage(String text, List<ImageAttachRec> images) {
        AttachmentRec attachmentRec = null;
        if (images != null && !images.isEmpty()) {
            RealmList<ImageAttachRec> list;
            list = new RealmList<>();
            list.addAll(images);

            attachmentRec = new AttachmentRec(list, null, null, null, null);
        }
        appendMessage(SendMessageHelper.prepareSendWithAttachment(getActivity(), contact.phone,
                text, attachmentRec, this));
    }

    private void prepareSendMessage(String text, VideoAttachRec videoAttachRec) {
        AttachmentRec attachmentRec = null;
        if (videoAttachRec != null) {
            RealmList<VideoAttachRec> list;
            list = new RealmList<>();
            list.add(videoAttachRec);

            attachmentRec = new AttachmentRec(null, null, null, null, list);
        }
        appendMessage(SendMessageHelper.prepareSendWithAttachment(getActivity(), contact.phone,
                text, attachmentRec, this));
    }

    private void appendMessage(MessageRec message) {
        if (message == null) {
            return;
        }
        appendMessage(ChatMessage.make(message, contact));
    }

    private void appendMessage(ChatMessage message) {
        boolean showProgress = message.attachmentRec != null && !message.attachmentRec.isEmpty();
        adapter.append(message);
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        if (showProgress) {
            adapter.updateMessageProgress(message.id, 0.5f);
        }

    }


    @SuppressLint("StaticFieldLeak")
    @Override
    public void attachmentUploaded(final MessageRec message) {
        if (fromCamera && attachmentFile != null) {
            String attachmentsPath = MissitoConfig.getAttachmentsPath(contact.phone);
            final String imagePath = attachmentsPath + attachmentFile.getName();
            new ImageHelper.CopyFileAsyncTask(attachmentFile, new File(imagePath)) {

                File attachmentFile = ChatFragment.this.attachmentFile;

                @Override
                protected void onPostExecute(Boolean success) {
                    super.onPostExecute(success);
                    if (!success) {
                        if (message.attach.hasImages()) {
                            Log.w(LOG_TAG, "Could not save image file");
                        } else {
                            Log.w(LOG_TAG, "Could not save video file");
                        }
                    } else {
                        if (message.attach.images != null && !message.attach.images.isEmpty()) {
                            RealmDBHelper.changeURIForImagesToLocal(message, contact.phone);
                        } else {
                            RealmDBHelper.changeURIForVideoToLocal(message, contact.phone);
                        }
                        attachmentFile.delete();
                        adapter.notifyDataSetChanged();
                    }
                }
            }.execute();
        }
        adapter.updateMessageProgress(message.localMsgId, 1f);
        fromCamera = false;
        attachmentFile = null;
    }

    private boolean fromCurrentChat(MessageRec message) {
        String uid = Application.app.connectionManager.uid;
        return message.senderUid.equals(uid) && message.destUid.equals(contact.phone)
                || message.senderUid.equals(contact.phone) && message.destUid.equals(uid);
    }

    private List<MessageRec> getHistory() {
        List<MessageRec> result = new ArrayList<>();

        RealmResults<MessageRec> all = RealmDBHelper.getChatHistory(contact.phone);
        all.sort("timestamp", Sort.ASCENDING);
        result.addAll(all);
        return result;
    }

    @OnClick(R.id.add_attachment)
    public void addAttachment() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERM_REQUEST_CODE:
                if (isPermissionGranted(grantResults)) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), R.string.no_camera_permission, Toast.LENGTH_SHORT).show();
                }
                break;
            case VIDEO_PERM_REQUEST_CODE:
                if (isPermissionGranted(grantResults)) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), R.string.no_camera_permission, Toast.LENGTH_SHORT).show();
                }
                break;
            case WRITE_EXT_STORAGE_REQUEST_CODE:
                if (isPermissionGranted(grantResults)) {
                    openGallery();
                } else {
                    Toast.makeText(getContext(), R.string.no_permission_to_read_storage, Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSIONS_REQUEST_CODE:
                for (String permission : permissions) {
                    toastIfPermissionNotGranted(permission);
                }
                break;
        }
    }

    private void toastIfPermissionNotGranted(String permission) {
        if (!isPermissionGranted(permission)) {
            if (permission.equals(Manifest.permission.CAMERA)) {
                Toast.makeText(getContext(), R.string.no_camera_permission, Toast.LENGTH_LONG).show();
            } else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(getContext(), R.string.no_permission_to_read_storage, Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final Integer deviceId = Application.app.contacts.deviceIds.get(contact.phone);

        if (deviceId == null) {
            Log.w(LOG_TAG, String.format("Can't send message to %s: no device_id", contact.phone));
            return;
        }

        if (resultCode == RESULT_OK) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            switch (requestCode) {
                case FORWARD_REQ_CODE:
                    List<ChatMessage> newMessages = (List<ChatMessage>) data.getSerializableExtra(NEW_MESSAGES_KEY);
                    for (ChatMessage newMessage : newMessages) {
                        appendMessage(newMessage);
                    }
                    break;
                case CAMERA_SELECT_REQUEST_CODE:
                    String path = data.getStringExtra(FILEPATH_KEY);
                    showResultActivity(path, true);
                    Log.d(LOG_TAG, "File path: " + path);
                    break;
                case IMAGE_OR_VIDEO_CONFIRM_REQUEST_CODE:
                    String filePath = data.getStringExtra(IMAGE_FILE_KEY);
                    String mimeTypeCamera = URLConnection.guessContentTypeFromName(filePath);
                    if (mimeTypeCamera != null && mimeTypeCamera.startsWith("image")) {
                        sendImage(filePath, null);
                    } else {
                        sendVideo(filePath);
                    }
                    break;
                case GALLERY_SELECT_REQUEST_CODE:
                    Uri uriGallery = data.getData();
                    showResultActivity(Helper.getFilePathFromUri(uriGallery), false);
                    break;
                case PLACE_PICKER_REQUEST:

                    Place place = PlacePicker.getPlace(getContext(), data);
                    LocationAttachRec locationAttachRec;
                    if (place != null && place.getViewport() != null) {
                        double radius = SphericalUtil.computeDistanceBetween(place.getViewport().northeast, place.getViewport().southwest);
                        locationAttachRec = new LocationAttachRec(place.getLatLng(), radius, place.getAddress().toString());
                    } else {
                        LatLngBounds latLngBounds = PlacePicker.getLatLngBounds(data);
                        double radius = SphericalUtil.computeDistanceBetween(latLngBounds.northeast, latLngBounds.southwest);
                        locationAttachRec = new LocationAttachRec(latLngBounds.getCenter(), radius, null);
                    }

                    RealmList<LocationAttachRec> locations = new RealmList<>();
                    locations.add(locationAttachRec);

                    MessageRec mssg = MessageRec.getBuilder()
                            .setTimestamp(new Date().getTime())
                            .setSenderUid(Application.app.connectionManager.uid)
                            .setSenderDeviceId(Application.app.connectionManager.deviceId)
                            .setDestUid(contact.phone)
                            .setDestDeviceId(deviceId)
                            .setOutgoingStatus(OutgoingMessageStatus.OUTGOING)
                            .setAttach(new AttachmentRec(null, null, locations, null, null))
                            .build();

                    adapter.append(ChatMessage.make(mssg, contact));
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    SendMessageHelper.sendMessage(mssg);
                    break;
                case CHOOSE_CONTACT:
                    Uri uriContact = data.getData();
                    ContentResolver contactResolver = getActivity().getContentResolver();
                    if (uriContact != null) {
                        Cursor cursor = contactResolver.query(uriContact, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            final String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            final RealmList<RealmString> phones = new RealmList<>();
                            phones.add(new RealmString(Helper.getAcceptablePhone(number)));
                            cursor.close();

                            final RealmList<RealmString> emails = addEmails(contactResolver, contactId);

                            Cursor cr = contactResolver.query(
                                    ContactsContract.Contacts.CONTENT_URI,
                                    null,
                                    ContactsContract.Contacts._ID + "=?", new String[]{contactId},
                                    null);

                            String photoId = null;
                            if (cr != null && cr.moveToFirst()) {
                                photoId = cr.getString(cr.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                                cr.close();
                            }

                            Bitmap bitmap = null;
                            Uri avatarUri = null;
                            try {
                                if (photoId != null) {
                                    avatarUri = Helper.getContactPhotoURI(contactId);
                                    bitmap = MediaStore.Images.Media.getBitmap(contactResolver, avatarUri);
                                }
                            } catch (IOException e) {
                                Log.e(String.format("Could not get contact %s avatar", phones.isEmpty() ? contactName : phones.get(0).string), e.getMessage());
                            }
                            if (avatarUri != null) {
                                final Bitmap finalBitmap = bitmap;
                                AsyncTask<Void, Void, String> task = new ImageHelper.Base64FromBitmapAsyncTask(avatarUri, finalBitmap) {
                                    @Override
                                    protected void onPostExecute(String base64String) {
                                        super.onPostExecute(base64String);
                                        if (TextUtils.isEmpty(base64String) && finalBitmap != null) {
                                            Log.e(LOG_TAG, "Could not convert bitmap to base64");
                                        }
                                        sendContact(contactName, phones, emails, base64String);
                                        asyncTasks.remove(this);
                                    }
                                };
                                asyncTasks.add(task);
                                task.execute();
                            } else {
                                sendContact(contactName, phones, emails, null);
                            }
                        }
                    } else {
                        Log.d(LOG_TAG, "Null contact Uri!");
                    }
                    break;
                case RECORD_AUDIO:

                    final Uri uri = data.getData();
                    final File file = new File(getRealPathFromContentUri(getContext(), contact.phone, uri));

                    final RealmList<AudioAttachRec> audio = new RealmList<>();
                    audio.add(new AudioAttachRec(null, file.getName(), null, file.length(), "secret", Helper.getUriFromFile(file).toString()));

                    final MessageRec msg =
                            MessageRec.getBuilder()
                                    .setTimestamp(new Date().getTime())
                                    .setSenderUid(Application.app.connectionManager.uid)
                                    .setSenderDeviceId(Application.app.connectionManager.deviceId)
                                    .setDestUid(contact.phone)
                                    .setDestDeviceId(deviceId)
                                    .setOutgoingStatus(OutgoingMessageStatus.OUTGOING)
                                    .setAttach(new AttachmentRec(null, null, null, audio, null))
                                    .build();

                    final ChatMessage chatMessage = ChatMessage.make(msg, contact);
                    adapter.append(chatMessage);
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    final String secretKey = AESHelper.generateBase64Key();
                    new EncryptAttachmentAsyncTask(secretKey, file, contact.phone) {

                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (success) {
                                final File encryptedFile = new File(MissitoConfig.getAttachmentsPath(contact.phone), file.getName() + ".enc");
                                Application.app.connectionManager.apiRequests.attach(contact.phone, deviceId, encryptedFile.length(),
                                        new Response.Listener<AttachmentSpec>() {
                                            @Override
                                            public void onResponse(AttachmentSpec response) {
                                                RealmDBHelper.setAudioMsgDownloadLink(msg, response.downloadURL);
                                                audio.get(0).secret = secretKey;
                                                SendMessageHelper.uploadAndRemove(getActivity(), response, encryptedFile, msg, null);
                                            }
                                        }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Log.e(LOG_TAG, "Attach error: ", error);
                                            }
                                        });
                            } else {
                                Toast.makeText(getActivity(), R.string.failed_encrypt_audio, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                    break;
            }
        }
    }

    private RealmList<RealmString> addEmails(ContentResolver contactResolver, String contactId) {
        RealmList<RealmString> emails = new RealmList<>();
        Cursor emailCursor = contactResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?", new String[]{contactId},
                null);
        while (emailCursor != null && emailCursor.moveToNext()) {
            String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            emails.add(new RealmString(email));
        }

        if (emailCursor != null) {
            emailCursor.close();
        }
        return emails;
    }

    @Override
    public void onPause() {
        super.onPause();
        MediaPlayerSingleton.releasePlayer();
        if (Application.app.connectionManager.authState != ConnectionManager.AuthState.LOGGED_OUT) {
            Application.app.contacts.resetContactUnreadCount(contact.phone);
        }
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(newMessageReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(typingNotificationReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(imageLoadedAndDecryptedReceiver);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onImageMessageClick(final ChatMessage message, final int position) {
        if (imageViewer != null) {
            Log.d(ImageHelper.class.getSimpleName(), "DEBUG: ImageViewer is visible. Cannot create new one");
            return;
        }


        final ImageAttachRec imageAttachRec = message.attachmentRec.images.get(0);

        final List<ImageAttachRec> images = new ArrayList<>();
        images.add(imageAttachRec);
        if (!TextUtils.isEmpty(imageAttachRec.localFileURI)) {
            openImageViewer(images);
        } else {
            dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getString(R.string.image_is_loading));
            dialog.show();
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    imageToOpenAfterDownloading = null;
                }
            });
            Application.WiFiFetch.getDownloadsInGroupWithStatus(contact.phone.hashCode(), Status.DOWNLOADING, new Func<List<? extends Download>>() {
                @Override
                public void call(List<? extends Download> downloads) {
                    for (Download download : downloads) {
                        if (download.getTag().equals(message.serverId)) {
                            imageToOpenAfterDownloading = images.get(0);
                            return;
                        }
                    }
                    imageToOpenAfterDownloading = images.get(0);
                    MessageRec messageRec = RealmDBHelper.getMessageByServerId(message.serverId);
                    DownloadHelper.downloadAttachment(messageRec, false);
                }
            });
        }
    }

    private void openImageViewer(List<ImageAttachRec> images) {
        imageViewer = new ImageViewer.Builder<>(getContext(), images)
                .setStartPosition(0)
                .setOnDismissListener(new ImageViewer.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        imageViewer = null;
                    }
                })
                .setFormatter(new ImageViewer.Formatter<ImageAttachRec>() {
                    @Override
                    public String format(ImageAttachRec imageRealm) {
                        if (!TextUtils.isEmpty(imageRealm.localFileURI)) {
                            return imageRealm.localFileURI;
                        }

                        //TODO: check why we need this 'ImageHelper.formLocalURI'
                        String uri = ImageHelper.formLocalURI(getContext(), imageRealm.fileName);
                        if (uri != null && new File(uri).exists()) {
                            return uri;
                        }

                        String attachmentsPath = MissitoConfig.getAttachmentsPath(chat.participants.get(0).phone);
                        String imagePath = attachmentsPath + imageRealm.fileName;
                        if (new File(imagePath).exists()) {
                            return imagePath;
                        }

                        return BuildConfig.API_ENDPOINT + imageRealm.link;
                    }
                })
                .setCustomImageRequestBuilder(ImageViewer.createImageRequestBuilder().setPostprocessor(new BasePostprocessor() {
                    @Override
                    public CloseableReference<Bitmap> process(Bitmap sourceBitmap, PlatformBitmapFactory bitmapFactory) {
                        //TODO: Handle exception (OpenGLRenderer: Bitmap too large to be uploaded into a texture (4608x3456, max=4096x4096))
                        return super.process(sourceBitmap, bitmapFactory);
                    }
                }))
                .build();

        if (isResumed()) {
            imageViewer.show();
        }
    }

    @Override
    public void onAudioDownloadRequested(final ChatMessage audioMessage, final int position) {
        final AudioAttachRec audio = audioMessage.attachmentRec.audio.get(0);
        String audioPath = MissitoConfig.getAttachmentsPath(contact.phone);
        final String filePath = audioPath + audio.fileName;
        new DownloadFileAsyncTask(BuildConfig.API_ENDPOINT + audio.link, filePath) {
            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    new DecryptAttachmentAsyncTask(audio.secret, filePath) {
                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (success) {
                                RealmDBHelper.setAudioMsgFileURI(audio, Helper.getUriFromFile(new File(filePath)).toString());
                                audioMessage.isLoading = false;
                                adapter.notifyItemChanged(position);
                            } else {
                                Toast.makeText(getActivity(), R.string.failed_decrypt_audio, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                } else {
                    Log.e(LOG_TAG, "Failed to download or save file");
                }
            }
        }.execute();
    }

    @Override
    public void onVideoDownloadRequested(final ChatMessage videoMessage, final int position) {
        final VideoAttachRec video = videoMessage.attachmentRec.video.get(0);
        final String filePath = Helper.fileExists(MissitoConfig.getAttachmentsPath(contact.phone), video.fileName)
                ? MissitoConfig.getAttachmentsPath(contact.phone) + String.valueOf(System.currentTimeMillis()) + "_" + video.fileName
                : MissitoConfig.getAttachmentsPath(contact.phone) + video.fileName;
        new DownloadFileAsyncTask(BuildConfig.API_ENDPOINT + video.link, filePath) {
            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    new DecryptAttachmentAsyncTask(video.secret, filePath) {
                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (success) {
                                RealmDBHelper.setVideoMsgFileURI(video, Helper.getUriFromFile(new File(filePath)).toString());
                                videoMessage.isLoading = false;
                                adapter.notifyItemChanged(position);
                            } else {
                                Toast.makeText(getActivity(), R.string.failed_decrypt_video, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                } else {
                    Log.e(LOG_TAG, "Failed to download or save file");
                }
            }
        }.execute();
    }

    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(statusUpdateReceiver);
        for (AsyncTask x : asyncTasks) {
            x.cancel(true);
        }
        asyncTasks = null;
    }

    private void sendContact(String name,
                             RealmList<RealmString> phones,
                             RealmList<RealmString> emails,
                             String avatar) {
        Integer deviceId = Application.app.contacts.deviceIds.get(contact.phone);

        if (deviceId == null) {
            Log.w(LOG_TAG, String.format("Can't send message to %s: no device_id", contact.phone));
            return;
        }

        RealmList<ContactAttachRec> contacts = new RealmList<>();
        ContactAttachRec contact = new ContactAttachRec(name, phones, emails, avatar);
        contacts.add(contact);
        MissitoContact destContact = chat.participants.get(0);
        MessageRec msgContact = MessageRec.getBuilder()
                .setTimestamp(new Date().getTime())
                .setSenderUid(Application.app.connectionManager.uid)
                .setSenderDeviceId(Application.app.connectionManager.deviceId)
                .setDestUid(destContact.phone)
                .setDestDeviceId(deviceId)
                .setOutgoingStatus(OutgoingMessageStatus.OUTGOING)
                .setAttach(new AttachmentRec(null, contacts, null, null, null))
                .build();

        adapter.append(OutgoingChatMessage.make(msgContact, this.contact));
        recyclerView.scrollToPosition(adapter.getItemCount(adapter.getSectionCount() - 1) - 1);
        SendMessageHelper.sendMessage(msgContact);
    }

    private void showResultActivity(final String cameraFilePath, boolean isFromCamera) {
        Intent intent = new Intent(getContext(), ImageOrVideoSelectedActivity.class);
        intent.putExtra(IMAGE_FILE_KEY, cameraFilePath);
        intent.putExtra(IS_FROM_CAMERA_KEY, isFromCamera);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, IMAGE_OR_VIDEO_CONFIRM_REQUEST_CODE);
    }

    @Override
    public void onForwardSelected(String messageId) {
        Intent intent = new Intent(getContext(), ForwardActivity.class);
        intent.putExtra(ForwardActivity.MESSAGE_ID_KEY, messageId);
        startActivityForResult(intent, FORWARD_REQ_CODE);
    }

    private void dismissProgressDialog(){
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private boolean mustOpenImageUri(String localFileUri) {
        return imageToOpenAfterDownloading != null && imageToOpenAfterDownloading.localFileURI.equals(localFileUri);
    }

    private void onStopOrCancelLoadingImage(MessageRec messageRec){
        if (mustOpenImageUri(messageRec.attach.images.get(0).localFileURI)){
            dismissProgressDialog();
            imageToOpenAfterDownloading = null;
        }
    }


}
