package ch.mitto.missito.ui.tabs.chat.message;

import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;

public interface AudioPlayerDownloadListener {
    void onAudioDownloadRequested(ChatMessage audioMessage, int position);
}
