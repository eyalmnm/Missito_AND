package ch.mitto.missito.ui.tabs.chat.message.outgoing;

import android.ch.mitto.missito.R;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.EnumMap;

import butterknife.BindView;
import ch.mitto.missito.net.model.OutgoingMessageStatus;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.BaseChatMessageViewHolder;

/**
 * Created by jenea on 9/4/17.
 */

public class OutgoingMessageViewHolder extends BaseChatMessageViewHolder {

    private static EnumMap<OutgoingMessageStatus, Integer> statusImageResource = new EnumMap<>(OutgoingMessageStatus.class);

    static {
        statusImageResource.put(OutgoingMessageStatus.RECEIVED, R.drawable.ic_chat_sent);
        statusImageResource.put(OutgoingMessageStatus.SEEN, R.drawable.ic_seen);
    }

    @BindView(R.id.img_seen)
    ImageView statusImg;

    @BindView(R.id.status_space)
    View statusSpaceView;

    OutgoingMessageViewHolder(View itemView) {
        super(itemView);
    }

    public void setMessage(OutgoingChatMessage message) {
        super.setMessage(message);

        if (statusImageResource.containsKey(message.status)) {
            statusImg.setVisibility(View.VISIBLE);
            statusImg.setImageResource(statusImageResource.get(message.status));
            statusSpaceView.setVisibility(View.GONE);
        } else {
            statusImg.setVisibility(View.GONE);
            statusSpaceView.setVisibility(View.VISIBLE);
        }
    }
}

