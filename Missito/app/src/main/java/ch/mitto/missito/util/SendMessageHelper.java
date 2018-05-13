package ch.mitto.missito.util;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.attach.AttachmentRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.VideoAttachRec;
import ch.mitto.missito.net.model.AttachmentSpec;
import ch.mitto.missito.net.model.OutgoingMessageStatus;
import cz.msebera.android.httpclient.Header;

/**
 * Created by usr1 on 10/24/17.
 */

public class SendMessageHelper {

    public static final String LOG_TAG = SendMessageHelper.class.getSimpleName();


    public static MessageRec prepareSendWithAttachment(final Context context, final String phone, String text, AttachmentRec attachmentRec, final SendMessageListener listener) {

        final Integer deviceId = Application.app.contacts.deviceIds.get(phone);

        if (deviceId == null) {
            Log.w(LOG_TAG, String.format("Can't send message to %s: no device_id", phone));
            return null;
        }

        final MessageRec message =
                MessageRec.getBuilder()
                        .setTimestamp(new Date().getTime())
                        .setSenderUid(Application.app.connectionManager.uid)
                        .setSenderDeviceId(Application.app.connectionManager.deviceId)
                        .setDestUid(phone)
                        .setDestDeviceId(deviceId)
                        .setBody(text)
                        .setOutgoingStatus(OutgoingMessageStatus.OUTGOING)
                        .setAttach(attachmentRec)
                        .build();

//        if (listener != null) {
//            listener.appendMessage(message, attachmentRec != null && !attachmentRec.isEmpty());
//        }

        if (attachmentRec != null) {
            if (attachmentRec.locations != null){
                sendMessage(message);
            }
            if (attachmentRec.images != null) {
                for (final ImageAttachRec realmImage : attachmentRec.images) {
                    Uri uri = Uri.parse(realmImage.localFileURI);
                    final String secretKey = AESHelper.generateBase64Key();
                    new EncryptAttachmentAsyncTask(secretKey, uri, phone) {
                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (success) {
                                final File encryptedFile = new File(MissitoConfig.getAttachmentsPath(companion), uri.getLastPathSegment() + ".enc");
                                Application.app.connectionManager.apiRequests.attach(phone, deviceId, encryptedFile.length(),
                                        new Response.Listener<AttachmentSpec>() {
                                            @Override
                                            public void onResponse(AttachmentSpec response) {
                                                RealmDBHelper.updateLinkAndSecretForImage(realmImage, response.downloadURL, secretKey);
                                                uploadAndRemove(context, response, encryptedFile, message, listener);
                                            }
                                        }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Log.e(LOG_TAG, "Attach error: ", error);
                                            }
                                        });
                            } else {
                                Toast.makeText(context, R.string.failed_encrypt_image, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                }
            }
            if (attachmentRec.video != null) {
                for (final VideoAttachRec realmVideo : attachmentRec.video) {
                    Uri uri = Uri.parse(realmVideo.localFileURI);
                    final String secretKey = AESHelper.generateBase64Key();
                    new EncryptAttachmentAsyncTask(secretKey, uri, phone) {
                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (success) {
                                final File encryptedFile = new File(MissitoConfig.getAttachmentsPath(companion), uri.getLastPathSegment() + ".enc");
                                Application.app.connectionManager.apiRequests.attach(phone, deviceId, encryptedFile.length(),
                                        new Response.Listener<AttachmentSpec>() {
                                            @Override
                                            public void onResponse(AttachmentSpec response) {
                                                RealmDBHelper.updateLinkAndSecretForVideo(realmVideo, response.downloadURL, secretKey);
                                                uploadAndRemove(context, response, encryptedFile, message, listener);
                                            }
                                        }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Log.e(LOG_TAG, "Attach error: ", error);
                                            }
                                        });
                            } else {
                                Toast.makeText(context, R.string.failed_encrypt_video, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                }
            }


        } else {
            sendMessage(message);
        }
        return message;
    }


    public static void uploadAndRemove(final Context context, AttachmentSpec attachmentSpec, final File file, final MessageRec message, final SendMessageListener listener) {

        Application.app.connectionManager.apiRequests.uploadAttachment(
                attachmentSpec,
                file,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        SendMessageHelper.sendMessage(message);
                        if (listener != null)
                            listener.attachmentUploaded(message);
                        file.delete();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Toast.makeText(context, String.format(context.getString(R.string.upload_failed), file.getAbsolutePath()), Toast.LENGTH_LONG).show();
                        // TODO: why it was removed?
                        // adapter.removeItem(message);
                        file.delete();
                        Log.e(LOG_TAG, "Failed to upload", error);
                    }
                });
    }


    public static void sendMessage(MessageRec message) {
        message.uniqueId = UUID.randomUUID().toString();
        RealmDBHelper.saveOutgoingMessage(message);
        Application.app.connectionManager.sendMessage(message, false);
    }


}
