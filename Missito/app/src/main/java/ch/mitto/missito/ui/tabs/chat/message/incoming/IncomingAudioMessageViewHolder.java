package ch.mitto.missito.ui.tabs.chat.message.incoming;

import android.ch.mitto.missito.R;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.View;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.IncomingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.AudioPlayerDownloadListener;
import ch.mitto.missito.ui.tabs.chat.view.AudioPlayerView;

public class IncomingAudioMessageViewHolder extends IncomingMessageViewHolder {

    @BindView(R.id.bubble)
    AudioPlayerView audioPlayerView;

    private AudioPlayerDownloadListener listener;

    public IncomingAudioMessageViewHolder(View itemView, AudioPlayerDownloadListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
    }

    public void setMessage(final IncomingChatMessage message) {
        super.setMessage(message);
        audioPlayerView.seekBar.getThumb().setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
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
