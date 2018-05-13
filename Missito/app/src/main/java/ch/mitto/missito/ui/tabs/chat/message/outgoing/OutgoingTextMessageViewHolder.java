package ch.mitto.missito.ui.tabs.chat.message.outgoing;

import android.ch.mitto.missito.R;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.CellActionListener;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;

public class OutgoingTextMessageViewHolder extends OutgoingMessageViewHolder {

    private static final String LOG_TAG = OutgoingTextMessageViewHolder.class.getSimpleName();

    private CellActionListener listener;

    @BindView(R.id.message_txt)
    TextView messageText;

    public OutgoingTextMessageViewHolder(View itemView, CellActionListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
    }

    public void setMessage(final OutgoingChatMessage message) {
        super.setMessage(message);
        messageText.setText(message.text);

        bubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatCellHelper.textMessageAlertDialog(context, message, listener).show();
            }
        });
    }
}
