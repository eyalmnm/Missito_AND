package ch.mitto.missito.services;

import android.app.IntentService;
import android.ch.mitto.missito.BuildConfig;
import android.content.Intent;

import com.tonyodev.fetch2.Request;

import ch.mitto.missito.Application;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.util.MissitoConfig;
import io.realm.Realm;


public class DownloadHelper  {


    public static void downloadAttachment(MessageRec messageRec, boolean onlyFromWifi) {
        if (messageRec == null || messageRec.attach == null){
            return;
        }
        if (messageRec.attach.hasImages()) {
            downloadImageAttach(messageRec, onlyFromWifi);
        }
    }

    private static void downloadImageAttach(final MessageRec messageRec, boolean onlyFromWifi){
        ImageAttachRec imageAttachRec = messageRec.attach.images.get(0);
        String filePath = MissitoConfig.getAttachmentsPath(messageRec.senderUid) + imageAttachRec.fileName;
        Request request = new Request(BuildConfig.API_ENDPOINT + imageAttachRec.link, filePath);
        request.addHeader("Authorization", "Bearer " + Application.app.connectionManager.backendToken);
        request.setGroupId(messageRec.senderUid.hashCode());
        request.setTag(messageRec.serverMsgId);
        if (onlyFromWifi) {
            Application.WiFiFetch.enqueue(request, null, null);
        } else {
            Application.allNetworkFetch.enqueue(request, null, null);
        }
    }

}
