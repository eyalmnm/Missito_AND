package ch.mitto.missito.util;

import android.os.AsyncTask;


/**
 * Created by usr1 on 12/29/17.
 */

public class DecryptAttachmentAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private String keySecret, filePath;

    protected DecryptAttachmentAsyncTask(String keySecret, String filePath) {
        this.keySecret = keySecret;
        this.filePath = filePath;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        return AESHelper.decrypt(keySecret, filePath);
    }
}
