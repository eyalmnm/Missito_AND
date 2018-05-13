package ch.mitto.missito.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Request;
import okio.BufferedSink;
import okio.Okio;

import static ch.mitto.missito.util.ImageHelper.okHttpClient;

public class DownloadFileAsyncTask extends AsyncTask<Void, String, Boolean> {

    private static final String LOG_TAG = DownloadFileAsyncTask.class.getSimpleName();

    private String filePath;
    private String url;

    protected DownloadFileAsyncTask(String url, String filePath) {
        this.filePath = filePath;
        this.url = url;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            Request request = new Request.Builder().url(url).method("GET", null).build();
            final okhttp3.Response response = okHttpClient.newCall(request).execute();

            File file = new File(filePath);

            file.getParentFile().mkdirs();
            BufferedSink sink = Okio.buffer(Okio.sink(file));
            sink.writeAll(response.body().source());
            sink.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to download or save file: " + filePath, e);
            return false;
        }
        return true;
    }
}