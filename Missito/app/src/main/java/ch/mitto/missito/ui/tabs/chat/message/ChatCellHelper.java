package ch.mitto.missito.ui.tabs.chat.message;

import android.ch.mitto.missito.R;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import ch.mitto.missito.db.model.attach.ContactAttachRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.db.model.common.RealmString;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;

/**
 * Created by usr1 on 1/16/18.
 */

public class ChatCellHelper {

    private static final String TAG = ChatCellHelper.class.getSimpleName();

    private ChatCellHelper() {
    }

    public static AlertDialog textMessageAlertDialog(final Context context, final ChatMessage message,
                                                     final CellActionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(R.string.message)
                .setItems(R.array.message_dialog_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                copyToClipboard(context, message.text);
                                Toast.makeText(context, R.string.clipboard, Toast.LENGTH_SHORT).show();
                                break;

                            case 1:
                                if (listener != null) {
                                    listener.onForwardSelected(message.id);
                                }
                                break;

                            case 2:
                                Toast.makeText(context, R.string.not_implemented, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                }).create();
    }

    private static void copyToClipboard(Context context, String str) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(null, str);
        clipboard.setPrimaryClip(clip);
    }

    public static AlertDialog imageMessageAlertDialog(final Context context, final ChatMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(R.string.image_message)
                .setItems(R.array.dots_menu_dialog_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                shareImage(context, message);
                                break;

                            case 1:
                                saveImageToGallery(context, message);
                                break;
                        }
                    }
                }).create();
    }

    public static void shareImage(Context context, ChatMessage message) {
        Log.d(TAG, "Image URI = " + "'" + message.attachmentRec.images.get(0).localFileURI + "'");
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/*");
        sharingIntent.putExtra(
                android.content.Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(context, context.getPackageName(), new File(URI.create(message.attachmentRec.images.get(0).localFileURI))));
        context.startActivity(Intent.createChooser(
                sharingIntent,
                context.getResources().getString(R.string.share_via)));
    }

    public static void saveImageToGallery(Context context, ChatMessage message) {
        ImageAttachRec image = message.attachmentRec.images.get(0);
        String imagePath = Uri.parse(image.localFileURI).getPath();

        try {
            MediaStore.Images.Media.insertImage(
                    context.getContentResolver(),
                    imagePath,
                    image.fileName,
                    image.fileName);
        } catch (IOException e) {
            Log.e(TAG, "File : '" + imagePath + "' was not found!");
        }
        Toast.makeText(context, R.string.saved_gallery, Toast.LENGTH_SHORT).show();
    }

    public static AlertDialog videoMessageAlertDialog(final Context context, final ChatMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(R.string.video_message)
                .setItems(R.array.dots_menu_dialog_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                shareVideo(context, message);
                                break;

                            case 1:
                                context.sendBroadcast(new Intent(
                                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                        Uri.parse(message.attachmentRec.video.get(0).localFileURI)));
                                Toast.makeText(context, R.string.saved_gallery, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                }).create();
    }

    private static void shareVideo(Context context, ChatMessage message) {
        Log.d(TAG, "Video URI = " + "'" + message.attachmentRec.video.get(0).localFileURI + "'");
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("video/*");
        sharingIntent.putExtra(
                android.content.Intent.EXTRA_STREAM,
                Uri.parse(message.attachmentRec.video.get(0).localFileURI));
        context.startActivity(Intent.createChooser(sharingIntent, context.getResources().getString(R.string.share_via)));
    }

    public static AlertDialog locationMessageAlertDialog(final Context context, final Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(R.string.location)
                .setItems(R.array.location_message_dialog_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                double latitude = marker.getPosition().latitude;
                                double longitude = marker.getPosition().longitude;
                                shareLocation(context, latitude, longitude);
                                break;
                        }
                    }
                }).create();
    }

    private static void shareLocation(Context context, double latitude, double longitude) {
        String markerLocationUri = "http://maps.google.com/maps?saddr=" + latitude + "," + longitude;
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, markerLocationUri);
        context.startActivity(Intent.createChooser(sharingIntent, context.getResources().getString(R.string.share_via)));
    }

    public static AlertDialog contactMessageAlertDialog(final Context context, final ChatMessage message, final CellActionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(R.string.contact_message)
                .setItems(R.array.contact_message_dialog_array, new DialogInterface.OnClickListener() {
                    Intent intent;

                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                copyToClipboard(context, message.attachmentRec.contacts.get(0).phones.first().string);
                                Toast.makeText(context, R.string.clipboard, Toast.LENGTH_SHORT).show();
                                break;

                            case 1:
                                saveContact(context, message.attachmentRec.contacts.get(0));
                                break;

                            case 2:
                                if (listener != null) {
                                    listener.onForwardSelected(message.id);
                                }
                                break;

                            case 3:
                                intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + message.attachmentRec.contacts.get(0).phones.first().string));
                                context.startActivity(intent);
                                break;
                        }
                    }
                }).create();
    }

    private static void saveContact(Context context, ContactAttachRec contact) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        ArrayList<ContentValues> data = new ArrayList<>();
        for (RealmString phone : contact.phones) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            row.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.string);
            row.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
            data.add(row);
        }

        for (RealmString email : contact.emails) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
            row.put(ContactsContract.CommonDataKinds.Email.ADDRESS, email.string);
            row.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
            data.add(row);
        }

        if (contact.avatar != null) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.CommonDataKinds.Photo.PHOTO, Base64.decode(contact.avatar, Base64.NO_WRAP));
            row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
            data.add(row);
        }

        intent.putExtra(ContactsContract.Intents.Insert.NAME, contact.name + (contact.surname == null ? "" : " " + contact.surname));
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        context.startActivity(intent);
    }
}
