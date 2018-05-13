package ch.mitto.missito.ui.tabs.chat.message.incoming;

import android.ch.mitto.missito.R;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.ui.tabs.chat.adapter.IncomingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.CellActionListener;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;

/**
 * Created by jenea on 9/4/17.
 */

public class IncomingTextMessageViewHolder extends IncomingMessageViewHolder {

    private CellActionListener listener;

    @BindView(R.id.message_txt)
    TextView messageText;

    public IncomingTextMessageViewHolder(View itemView, CellActionListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
    }

    public void setMessage(final IncomingChatMessage message) {
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

