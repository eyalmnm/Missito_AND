package ch.mitto.missito.ui.tabs.chat.message;

import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;

public interface ImageCellListener extends CellActionListener {

    void onImageMessageClick(ChatMessage message, int position);

}