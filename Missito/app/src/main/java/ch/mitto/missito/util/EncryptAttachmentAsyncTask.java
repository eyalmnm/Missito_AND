package ch.mitto.missito.util;

import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;

/**
 * Created by usr1 on 12/29/17.
 */

public class EncryptAttachmentAsyncTask extends AsyncTask<Void, Void, Boolean> {

    String secretKey, companion;
    Uri uri;
    File file;


    protected EncryptAttachmentAsyncTask(String secretKey, Uri uri, String companion) {
        this.secretKey = secretKey;
        this.uri = uri;
        this.companion = companion;
    }

    protected EncryptAttachmentAsyncTask(String secretKey, File file, String companion) {
        this.secretKey = secretKey;
        this.file = file;
        this.companion = companion;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        if (uri == null) {
            return AESHelper.encrypt(secretKey, file, companion);
        } else {
            return AESHelper.encrypt(secretKey, uri, companion);
        }
    }
}
