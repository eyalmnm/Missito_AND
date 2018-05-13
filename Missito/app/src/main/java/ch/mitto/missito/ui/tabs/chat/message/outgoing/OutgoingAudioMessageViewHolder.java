package ch.mitto.missito.ui.tabs.chat.message.outgoing;


import android.ch.mitto.missito.R;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.AudioPlayerDownloadListener;
import ch.mitto.missito.ui.tabs.chat.view.AudioPlayerView;

public class OutgoingAudioMessageViewHolder extends OutgoingMessageViewHolder {

    @BindView(R.id.bubble)
    AudioPlayerView audioPlayerView;

    private AudioPlayerDownloadListener listener;

    public OutgoingAudioMessageViewHolder(View itemView, AudioPlayerDownloadListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
    }

    public void setMessage(final OutgoingChatMessage message) {
        super.setMessage(message);
        audioPlayerView.seekBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        if (!message.attachmentRec.audio.isEmpty()) {
            audioPlayerView.setMessage(message, listener, getAdapterPosition());
        }
    }

    @Override
    protected float[] getCornerRadiiFor(ChatMessage message) {
        float[] cornerRadii = super.getCornerRadiiFor(message);
        cornerRadii = Arrays.copyOf(cornerRadii, cornerRadii.length);
        for (int i = 0; i < cornerRadii.length; i++) {
            if (cornerRadii[i] == dp22) {
                cornerRadii[i] = dp30;
            }
        }
        return cornerRadii;
    }
}
