package ch.mitto.missito.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;

import ch.mitto.missito.db.model.attach.AudioAttachRec;

public class MediaPlayerSingleton extends MediaPlayer {
    private static MediaPlayerSingleton mediaPlayerSingleton;
    private Listener listener;

    private MediaPlayerSingleton() {
    }

    public static MediaPlayerSingleton getInstance() {
        if (mediaPlayerSingleton == null) {
            mediaPlayerSingleton = new MediaPlayerSingleton();
        }
        return mediaPlayerSingleton;
    }

    public void setDataSource(Context context, AudioAttachRec audio) throws IOException {
        mediaPlayerSingleton.stop();
        mediaPlayerSingleton.reset();
        if (listener != null) {
            listener.onDataSourceChange();
        }

        setDataSource(context, Uri.parse(audio.localFileURI));
    }

    public void setOnDataSourceChangeListener(Listener listener) {
        this.listener = listener;
    }

    public static void releasePlayer() {
        if (mediaPlayerSingleton != null) {
            mediaPlayerSingleton.stop();
            mediaPlayerSingleton.reset();
        }
    }

    public interface Listener {
        void onDataSourceChange();
    }
}