package ch.mitto.missito.ui.tabs.chat.message;

import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;

public interface VideoCellListener extends CellActionListener {
    void onVideoDownloadRequested(ChatMessage videoMessage, int position);
}
