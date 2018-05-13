package ch.mitto.missito.util;

import android.Manifest;
import android.ch.mitto.missito.BuildConfig;
import android.ch.mitto.missito.R;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.File;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.attach.LocationAttachRec;
import ch.mitto.missito.net.broker.model.ContactsStatusModel;
import ch.mitto.missito.net.broker.model.OfflineContact;
import ch.mitto.missito.net.broker.model.ContactEntry;
import ch.mitto.missito.services.model.MissitoContact;

public class Helper {

    private static final String TAG = Helper.class.getSimpleName();

    private Helper() {
    }

    public static Map<String, MissitoContact> fetchContacts(Context context) {

        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");

        Map<String, MissitoContact> contactList = new HashMap<>();

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                String photoId = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));

                if (cursor.getInt(cursor.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor phonesCursor = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);

                    while (phonesCursor.moveToNext()) {
                        String phoneNo = phonesCursor.getString(phonesCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String acceptablePhone = Helper.getAcceptablePhone(phoneNo);
                        if (acceptablePhone != null) {
                            // TODO: optimize this (one request for all?)
                            // See https://stackoverflow.com/questions/22798990/android-contacts-id-data-contact-id
                            String fullname[] = getNameParts(context, id);
                            contactList.put(acceptablePhone, new MissitoContact(name, fullname[0],
                                    fullname[1], acceptablePhone, 0, null,
                                    photoId != null ? getContactPhotoURI(id).toString() : null));
                        }
                    }
                    phonesCursor.close();
                }
            }
        }
        cursor.close();
        return contactList;
    }

    private static String[] getNameParts(Context context, String contactId){
        String firstLastName[] = new String[2];
        String whereName = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.Data.CONTACT_ID + " = ?";
        String[] whereNameParams = new String[] { ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contactId };
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, whereName, whereNameParams, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
        if (cursor != null && cursor.moveToNext()) {
            firstLastName[0] = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            firstLastName[1] = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
        }
        if (cursor != null) {
            cursor.close();
        }
        return firstLastName;
    }

    public static Bitmap downscale(Bitmap bitmap, float maxWidth, float maxHeight) {
        float verticalRatio = bitmap.getHeight() / maxHeight;
        float horizontalRatio = bitmap.getWidth() / maxWidth;

        if (verticalRatio > 1.0 || horizontalRatio > 1.0) {
            if (verticalRatio > horizontalRatio) {
                return ThumbnailUtils.extractThumbnail(bitmap, (int) (bitmap.getWidth() / verticalRatio), (int) (bitmap.getHeight() / verticalRatio));
            } else {
                return ThumbnailUtils.extractThumbnail(bitmap, (int) (bitmap.getWidth() / horizontalRatio), (int) (bitmap.getHeight() / horizontalRatio));
            }
        } else {
            return bitmap;
        }
    }

    public static Bitmap upscale(Bitmap bitmap, float maxWidth, float maxHeight) {
        float verticalRatio = maxHeight / bitmap.getHeight();
        float horizontalRatio = maxWidth / bitmap.getWidth();

        if (verticalRatio > 1.0 || horizontalRatio > 1.0) {
            if (verticalRatio < horizontalRatio) {
                return ThumbnailUtils.extractThumbnail(bitmap, (int) (bitmap.getWidth() * verticalRatio), (int) (bitmap.getHeight() * verticalRatio));
            } else {
                return ThumbnailUtils.extractThumbnail(bitmap, (int) (bitmap.getWidth() * horizontalRatio), (int) (bitmap.getHeight() * horizontalRatio));
            }
        } else {
            return bitmap;
        }
    }

    public static AlertDialog showErrorDialog(Context context, String title, String message) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static Uri getContactPhotoURI(String contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId));
        return Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    public static boolean canReadContacts(Context context) {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canReceiveSms(Context context) {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static String formatWhen(Context context, long when) {
        Calendar whenDateTime = Calendar.getInstance();
        whenDateTime.setTimeInMillis(when);
        if (Calendar.getInstance().get(Calendar.YEAR) == whenDateTime.get(Calendar.YEAR)) {
            long difference = System.currentTimeMillis() - when;
            return (difference <= DateUtils.MINUTE_IN_MILLIS) ?
                    context.getResources().getString(R.string.just_now) :
                    DateUtils.getRelativeTimeSpanString(
                            when,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE)
                            .toString();
        } else {
            return DateUtils.getRelativeTimeSpanString(when).toString();
        }
    }

    public static String getInitials(String name) {
        String[] words = name.split(" ");
        switch (words.length) {
            case 0:
                return "";
            case 1:
                return words[0].substring(0, 1).toUpperCase();
            default:
                return words[words.length - 1].substring(0, 1).toUpperCase();
        }

//        if (name == null) {
//            return "";
//        }
//        name = name.trim();
//        return name.length() == 0 ? "" : name.substring(0, 1).toUpperCase();
    }

    public static ArrayList<ContactEntry> getNewContacts(ContactsStatusModel data) {
        final ArrayList<ContactEntry> contacts = new ArrayList<>();
        if (data.msg.blocked != null) {
            for (ContactEntry contactEntry : data.msg.blocked) {
                contacts.add(contactEntry);
            }
        }

        if (data.msg.muted != null) {
            for (ContactEntry contactEntry : data.msg.muted) {
                contacts.add(contactEntry);
            }
        }

        if (data.msg.online != null) {
            contacts.addAll(data.msg.online);
        }

        if (data.msg.offline != null) {
            for (OfflineContact offlineContact : data.msg.offline) {
                contacts.add(new ContactEntry(offlineContact.userId, 0));
            }
        }
        Set<String> missitoContacts = Application.app.contacts.missitoContactsByPhone.keySet();
        ArrayList<ContactEntry> newContacts = new ArrayList<>();
        for (ContactEntry contact : contacts) {
            if (!missitoContacts.contains(contact.userId)) {
                newContacts.add(contact);
            }
        }
        return newContacts;
    }

    private static String getCountryCode() {
        TelephonyManager tm = (TelephonyManager) Application.app.getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = tm.getSimCountryIso();
        return TextUtils.isEmpty(countryCode)
                ? PrefsHelper.getCountryCode()
                : countryCode.toUpperCase();
    }

    public static String getAcceptablePhone(String phone) {
        try {
            PhoneNumberUtil numberUtil = PhoneNumberUtil.getInstance();
            String cCode = getCountryCode();
            return numberUtil.format(numberUtil.parse(phone, cCode),
                    PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            return null;
        }
    }

    public static String getDeviceId() {
        return Settings.Secure.getString(Application.app.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getTitle(LocationAttachRec location) {
        return !TextUtils.isEmpty(location.label) ? location.label : prettyFormatCoordinates(location.lat, location.lon);
    }

    private static String prettyFormatCoordinates(double latitude, double longitude) {
        StringBuilder builder = new StringBuilder();


        builder.append(formatDegrees(latitude));

        if (latitude < 0) {
            builder.append(" S ");
        } else {
            builder.append(" N ");
        }

        builder.append(formatDegrees(longitude));

        if (longitude < 0) {
            builder.append(" W");
        } else {
            builder.append(" E");
        }

        return builder.toString();
    }

    /**
     * @param degrees latitude on longitude to format
     * @return pretty formatted degrees like 44°51'02.1"
     */
    private static String formatDegrees(double degrees) {
        StringBuilder builder = new StringBuilder();
        String latitudeDegrees = Location.convert(Math.abs(degrees), Location.FORMAT_SECONDS);
        String[] latitudeSplit = latitudeDegrees.split(":");
        builder.append(latitudeSplit[0]);
        builder.append("°");
        builder.append(latitudeSplit[1]);
        builder.append("'");
        try {
            double seconds = NumberFormat.getInstance().parse(latitudeSplit[2]).doubleValue();
            DecimalFormat form = new DecimalFormat("00.0");
            builder.append(form.format(seconds));
            builder.append("\"");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static void setMenuItemColor(Context context, MenuItem menuItem, int color) {
        Drawable drawable = menuItem.getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, color));
        menuItem.setIcon(drawable);
    }

    public static String formatPhoneInternational(String phoneNumber) {
        try {
            PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
            String phone = Helper.addPlus(phoneNumber);
            Phonenumber.PhoneNumber pn = pnu.parse(phone, "");
            return pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        } catch (Exception e) {
            Log.w(TAG, "Could not format phone number", e);
            return phoneNumber;
        }
    }

    public static void startVideoPlayer(String uriString, Context context, boolean isOutgoing) {
        Uri videoUri = Uri.parse(uriString);
        Intent intent = new Intent(Intent.ACTION_VIEW, videoUri);
        if (Build.VERSION.SDK_INT > 19 && isOutgoing) { //crash on kitkat
            intent.setDataAndType(FileProvider.getUriForFile(context, context.getPackageName(), new File(URI.create(uriString))), "video/*");
        } else {
            intent.setDataAndType(videoUri, "video/*");
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public static Uri getUriFromFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(Application.app,
                    BuildConfig.APPLICATION_ID,

                    file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public static String removePlus(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.trim().startsWith("+") ? phone.substring(1) : phone;
    }

    public static List<String> removePlus(List<String> phones) {
        if (phones == null || phones.isEmpty()) {
            return phones;
        }

        ArrayList<String> result = new ArrayList<>();
        for (String phone : phones) {
            result.add(Helper.removePlus(phone));
        }
        return result;
    }

    public static String addPlus(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.trim().startsWith("+") ? phone : "+" + phone;
    }

//    public static ArrayList<String> addPlus(ArrayList<String> phones) {
//        ArrayList<String> fixedPhones = new ArrayList<>();
//        if (phones == null || phones.isEmpty()) {
//            return fixedPhones;
//        }
//
//        for (String phone : phones) {
//            fixedPhones.add(phone.trim().startsWith("+") ? phone : "+" + phone);
//        }
//
//        return fixedPhones;
//    }

    public static ArrayList<ContactEntry> addPlus(ArrayList<ContactEntry> contactEntries) {
        ArrayList<ContactEntry> result = new ArrayList<>();
        if (contactEntries == null || contactEntries.isEmpty()) {
            return result;
        }

        for (ContactEntry contactEntry : contactEntries) {
            result.add(new ContactEntry(contactEntry.userId.trim().startsWith("+") ?
                    contactEntry.userId : "+" + contactEntry.userId,
                    contactEntry.deviceId));
        }

        return result;
    }

    // https://stackoverflow.com/a/36714242
    public static String getFilePathFromUri(Uri uri) {
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(Application.app, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = Application.app.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean fileExists(String dirPath, String fileName) {
        return new File(dirPath + fileName).exists();
    }
}
