package ch.mitto.missito.ui.tabs.chat.message.incoming;

import android.ch.mitto.missito.R;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.IncomingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.BaseChatMessageViewHolder;
import ch.mitto.missito.util.AvatarWrapper;

public class IncomingMessageViewHolder extends BaseChatMessageViewHolder {

    private final AvatarWrapper avatar;

    @BindView(R.id.initials_txt)
    TextView interlocutorInitials;
    @BindView(R.id.avatar)
    RoundedImageView messageImg;

    public IncomingMessageViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        avatar = new AvatarWrapper(context, messageImg, interlocutorInitials);
    }

    public void setMessage(final IncomingChatMessage message) {
        super.setMessage(message);
        if (message.inGroupType == ChatMessage.MessageInGroupType.SINGLE || message.inGroupType == ChatMessage.MessageInGroupType.LAST) {
            interlocutorInitials.setVisibility(View.VISIBLE);
            messageImg.setVisibility(View.VISIBLE);
            avatar.update(Application.app.contacts.missitoContactsByPhone.get(message.senderContact.phone));
        } else {
            interlocutorInitials.setVisibility(View.INVISIBLE);
            messageImg.setVisibility(View.INVISIBLE);
        }
        String contactName = Application.app.contacts.getContactName(message.senderContact.phone);
        interlocutorInitials.setText(String.valueOf(contactName.charAt(0)));
    }
}
