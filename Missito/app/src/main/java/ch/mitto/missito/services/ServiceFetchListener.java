package ch.mitto.missito.services;

import android.util.Log;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;

import java.io.File;

import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.util.DecryptAttachmentAsyncTask;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.MissitoConfig;
import ch.mitto.missito.util.NotificationHelper;
import ch.mitto.missito.util.RealmDBHelper;

public class ServiceFetchListener extends AbstractFetchListener {

    private final static String LOG_TAG = ServiceFetchListener.class.getSimpleName();

    @Override
    public void onCompleted(Download download) {
        MessageRec messageRec = RealmDBHelper.getMessageByServerId(download.getTag());

        if (messageRec != null && messageRec.attach.hasImages()) {
            imageDownloaded(messageRec);
        }

        super.onCompleted(download);
    }

    @Override
    public void onError(Download download) {
        super.onError(download);
        notifyErrorOrCanceledDownload(download);
        Log.e(LOG_TAG, download.getError().name(), download.getError().getThrowable());
    }

    @Override
    public void onPaused(Download download) {
        super.onPaused(download);
        notifyErrorOrCanceledDownload(download);
    }

    @Override
    public void onCancelled(Download download) {
        super.onCancelled(download);
        notifyErrorOrCanceledDownload(download);
    }

    private void imageDownloaded(final MessageRec messageRec){
        final ImageAttachRec imageAttachRec = messageRec.attach.images.first();
        final String filePath = MissitoConfig.getAttachmentsPath(messageRec.senderUid) + imageAttachRec.fileName;
        new DecryptAttachmentAsyncTask(imageAttachRec.secret, filePath){

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    RealmDBHelper.setImageMsgFileURI(imageAttachRec, Helper.getUriFromFile(new File(filePath)).toString());
                    NotificationHelper.notifyDownloadedAndDecryptedImage(messageRec, true);
                } else {
                    Log.e(LOG_TAG, "Failed to decrypt file");
                }
            }
        }.execute();
    }

    private void notifyErrorOrCanceledDownload(Download download) {
        MessageRec messageRec = RealmDBHelper.getMessageByServerId(download.getTag());
        NotificationHelper.notifyDownloadedAndDecryptedImage(messageRec, false);
    }
}
